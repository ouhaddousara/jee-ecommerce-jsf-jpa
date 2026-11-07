package ma.fstt.service;

import java.util.List;

import ma.fstt.entities.Produit;
import ma.fstt.entities.Vitrine;

/** Public read-only view of the storefront: what any visitor may see. */
public interface CatalogueService {

    /** @return the active storefront, or null when none is active */
    Vitrine vitrineActive();

    /** Categories of the active storefront, with their number of ACTIVE products. */
    List<CategorieVue> categories();

    /**
     * Active products of the active storefront, 9 per page.
     *
     * @param categorieId optional category filter
     * @param recherche   optional text searched in name, reference and description
     * @param page        1-based; clamped to the valid range
     */
    PageResultat<Produit> catalogue(Long categorieId, String recherche, int page);

    /** @return the product if it exists AND is active, otherwise null */
    Produit produitPublic(long id);
}
