package ma.fstt.web;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import ma.fstt.entities.Panier;
import ma.fstt.entities.Produit;
import ma.fstt.service.CatalogueService;

/** One product (detail.xhtml?id=...). Read-only: the "add to cart" button calls PanierBean. */
@Named
@ViewScoped
public class DetailBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private CatalogueService catalogue;

    private Long id;

    /** Typed quantity, kept as text: a malformed value is refused later by the business rules. */
    private String quantite = "1";

    private transient Produit produit;

    /** View action: an unknown or inactive product is a 404, as in workshop 2. */
    public String init() throws IOException {
        if (getProduit() == null) {
            FacesContext contexte = FacesContext.getCurrentInstance();
            contexte.getExternalContext().responseSendError(HttpServletResponse.SC_NOT_FOUND, "Produit introuvable");
            contexte.responseComplete();
        }
        return null;
    }

    public Produit getProduit() {
        if (produit == null && id != null) {
            produit = catalogue.produitPublic(id);
        }
        return produit;
    }

    public int getQuantiteMax() {
        Produit p = getProduit();
        return p == null ? 1 : Math.min(p.getStock(), Panier.MAX_QUANTITE_PAR_LIGNE);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuantite() {
        return quantite;
    }

    public void setQuantite(String quantite) {
        this.quantite = quantite;
    }
}
