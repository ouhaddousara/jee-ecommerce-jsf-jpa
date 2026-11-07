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

import ma.fstt.entities.Categorie;
import ma.fstt.exception.MetierException;
import ma.fstt.exception.ValidationException;
import ma.fstt.service.CategorieForm;
import ma.fstt.service.CategorieVue;
import ma.fstt.service.GestionCatalogueService;

/**
 * Category management for administrators (admin/categories.xhtml and
 * admin/categorie-form.xhtml). Access is enforced by AuthFilter on /admin/*.
 */
@Named
@ViewScoped
public class CategorieAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String LISTE = "/admin/categories.xhtml?faces-redirect=true";

    @Inject
    private GestionCatalogueService gestion;

    /** View parameter of the form page (?id=); absent for a new category. */
    private Long id;

    private CategorieForm form = new CategorieForm();
    private String titre = "Nouvelle catégorie";
    private LinkedHashMap<String, String> erreurs = new LinkedHashMap<>();

    private transient List<CategorieVue> categories;

    // ---- list page ----

    public List<CategorieVue> getCategories() {
        if (categories == null) {
            categories = gestion.categories();
        }
        return categories;
    }

    public String supprimer(long categorieId) {
        try {
            gestion.supprimerCategorie(categorieId);
            Messages.succes("Catégorie supprimée.");
        } catch (MetierException e) {
            Messages.erreur(e.getMessage());
        }
        Messages.apresRedirection();
        return LISTE;
    }

    // ---- form page ----

    /** View action: loads the category to edit, or prepares an empty form. */
    public String charger() throws IOException {
        if (id == null) {
            form = new CategorieForm();
            titre = "Nouvelle catégorie";
            return null;
        }
        Categorie categorie = gestion.categorie(id);
        if (categorie == null) {
            FacesContext contexte = FacesContext.getCurrentInstance();
            contexte.getExternalContext().responseSendError(HttpServletResponse.SC_NOT_FOUND, "Catégorie introuvable");
            contexte.responseComplete();
            return null;
        }
        form = CategorieForm.depuis(categorie);
        titre = "Modifier la catégorie";
        return null;
    }

    public String enregistrer() {
        try {
            Categorie categorie = gestion.enregistrerCategorie(form);
            Messages.succes("Catégorie « " + categorie.getNom() + " » enregistrée.");
            Messages.apresRedirection();
            return LISTE;
        } catch (ValidationException e) {
            erreurs = new LinkedHashMap<>(e.getErreurs());
            return null;
        } catch (MetierException e) {
            Messages.erreur(e.getMessage());
            Messages.apresRedirection();
            return LISTE;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CategorieForm getForm() {
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
