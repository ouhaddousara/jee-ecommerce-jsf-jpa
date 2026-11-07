package ma.fstt.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import ma.fstt.entities.Produit;
import ma.fstt.exception.MetierException;
import ma.fstt.exception.ValidationException;
import ma.fstt.service.CategorieVue;
import ma.fstt.service.GestionCatalogueService;
import ma.fstt.service.ProduitForm;

/**
 * Product management for administrators (admin/produits.xhtml and admin/produit-form.xhtml).
 * Access is enforced by AuthFilter on /admin/*. The same bean serves both pages: the list
 * uses {@link #getProduits()} and the two row actions, the form uses the rest.
 */
@Named
@ViewScoped
public class ProduitAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String LISTE = "/admin/produits.xhtml?faces-redirect=true";

    @Inject
    private GestionCatalogueService gestion;

    /** View parameter of the form page (?id=); absent for a new product. */
    private Long id;

    private ProduitForm form = new ProduitForm();
    private String titre = "Nouveau produit";
    private LinkedHashMap<String, String> erreurs = new LinkedHashMap<>();

    private transient List<Produit> produits;
    private transient List<CategorieVue> categories;

    // ---- list page ----

    public List<Produit> getProduits() {
        if (produits == null) {
            produits = gestion.tousLesProduits();
        }
        return produits;
    }

    public String basculer(long produitId) {
        try {
            boolean actif = gestion.basculerProduit(produitId);
            Messages.succes(actif ? "Produit remis en vente." : "Produit retiré de la vente.");
        } catch (MetierException e) {
            Messages.erreur(e.getMessage());
        }
        Messages.apresRedirection();
        return LISTE;
    }

    public String supprimer(long produitId) {
        try {
            gestion.supprimerProduit(produitId);
            Messages.succes("Produit supprimé.");
        } catch (MetierException e) {
            Messages.erreur(e.getMessage());
        }
        Messages.apresRedirection();
        return LISTE;
    }

    // ---- form page ----

    /** View action: loads the product to edit, or prepares an empty form. */
    public String charger() throws IOException {
        if (id == null) {
            form = new ProduitForm();
            form.setActif(true);
            form.setStock("0");
            titre = "Nouveau produit";
            return null;
        }
        Produit produit = gestion.produit(id);
        if (produit == null) {
            FacesContext contexte = FacesContext.getCurrentInstance();
            contexte.getExternalContext().responseSendError(HttpServletResponse.SC_NOT_FOUND, "Produit introuvable");
            contexte.responseComplete();
            return null;
        }
        // The form keeps the version read here: the service refuses a save based on an outdated copy.
        form = ProduitForm.depuis(produit);
        titre = "Modifier le produit";
        return null;
    }

    public String enregistrer() {
        try {
            Produit produit = gestion.enregistrerProduit(form);
            Messages.succes("Produit « " + produit.getNom() + " » enregistré.");
            Messages.apresRedirection();
            return LISTE;
        } catch (ValidationException e) {
            erreurs = new LinkedHashMap<>(e.getErreurs());
            return null; // stay on the form, field errors are shown next to the fields
        } catch (MetierException e) {
            // e.g. someone else edited the product meanwhile: nothing to fix in the form.
            Messages.erreur(e.getMessage());
            Messages.apresRedirection();
            return LISTE;
        }
    }

    public List<CategorieVue> getCategories() {
        if (categories == null) {
            categories = gestion.categories();
        }
        return categories;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProduitForm getForm() {
        return form;
    }

    public String getTitre() {
        return titre;
    }

    public Map<String, String> getErreurs() {
        return erreurs;
    }

    public boolean isAvecErreurs() {
        return !erreurs.isEmpty();
    }
}
