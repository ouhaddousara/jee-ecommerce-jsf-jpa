package ma.fstt.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ma.fstt.entities.Vitrine;
import ma.fstt.service.CatalogueService;
import ma.fstt.service.CategorieVue;
import ma.fstt.service.PageResultat;
import ma.fstt.entities.Produit;

/**
 * Public storefront (vitrine.xhtml): category filter, search and pagination.
 * The three filters arrive as view parameters (?categorie=, ?q=, ?page=), so a
 * result page can be bookmarked and the browser back button works.
 */
@Named
@ViewScoped
public class VitrineBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private CatalogueService catalogue;

    private Long categorie;
    private String q = "";
    private Integer page = 1;

    /** Set by the sign-out redirect (?deconnecte=1). */
    private String deconnecte;

    // Entities are not Serializable and go stale: they are reloaded lazily, never kept in the view state.
    private transient Vitrine vitrine;
    private transient List<CategorieVue> categories;
    private transient PageResultat<Produit> resultat;

    /** View action: confirms a sign-out. */
    public String init() {
        if ("1".equals(deconnecte)) {
            Messages.info("Tu es déconnecté(e). À bientôt !");
        }
        return null;
    }

    private void charger() {
        if (resultat == null) {
            vitrine = catalogue.vitrineActive();
            categories = catalogue.categories();
            resultat = catalogue.catalogue(categorie, q, page == null || page < 1 ? 1 : page);
        }
    }

    public Vitrine getVitrine() {
        charger();
        return vitrine;
    }

    public List<CategorieVue> getCategories() {
        charger();
        return categories;
    }

    public PageResultat<Produit> getResultat() {
        charger();
        return resultat;
    }

    /** 1..totalPages, for the pagination loop. */
    public List<Integer> getNumerosPages() {
        List<Integer> numeros = new ArrayList<>();
        for (int n = 1; n <= getResultat().getTotalPages(); n++) {
            numeros.add(n);
        }
        return numeros;
    }

    // ---- CSS classes of the active filter and page (computed here, not with comparisons in the page) ----

    public String getClasseTous() {
        return categorie == null ? " is-active" : "";
    }

    public String classeCategorie(Long id) {
        return id != null && id.equals(categorie) ? " is-active" : "";
    }

    public String classePage(int numero) {
        return numero == getResultat().getPage() ? "is-active" : "";
    }

    // ---- links that keep the current filters ----

    public String getUrlTous() {
        return construireUrl(contexte(), null, q, null);
    }

    public String urlCategorie(Long id) {
        return construireUrl(contexte(), id, q, null);
    }

    public String urlPage(int numero) {
        return construireUrl(contexte(), categorie, q, numero);
    }

    private static String contexte() {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
    }

    /** Builds /vitrine.xhtml?categorie=..&q=..&page=.. with only the parameters that matter. */
    public static String construireUrl(String contexte, Long categorieId, String recherche, Integer numeroPage) {
        StringBuilder url = new StringBuilder(contexte).append("/vitrine.xhtml");
        char separateur = '?';
        if (categorieId != null) {
            url.append(separateur).append("categorie=").append(categorieId);
            separateur = '&';
        }
        if (recherche != null && !recherche.isEmpty()) {
            url.append(separateur).append("q=").append(Chemins.encoder(recherche));
            separateur = '&';
        }
        if (numeroPage != null && numeroPage > 1) {
            url.append(separateur).append("page=").append(numeroPage);
        }
        return url.toString();
    }

    // ---- view parameters ----

    public Long getCategorie() {
        return categorie;
    }

    public void setCategorie(Long categorie) {
        this.categorie = categorie;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q == null ? "" : q.trim();
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page == null ? 1 : page;
    }

    public String getDeconnecte() {
        return deconnecte;
    }

    public void setDeconnecte(String deconnecte) {
        this.deconnecte = deconnecte;
    }
}
