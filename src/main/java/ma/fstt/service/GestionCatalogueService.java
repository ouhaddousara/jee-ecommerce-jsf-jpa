package ma.fstt.service;

import java.util.List;

import ma.fstt.entities.Categorie;
import ma.fstt.entities.Produit;

/** Back-office: catalogue management, reserved to administrators (checked by AuthFilter). */
public interface GestionCatalogueService {

    /** All products, active or not, ordered by reference. */
    List<Produit> tousLesProduits();

    /** @return the product whatever its state, or null */
    Produit produit(long id);

    /**
     * Creates the product (form id null) or updates it.
     *
     * @throws ma.fstt.exception.ValidationException invalid fields, duplicate reference
     * @throws ma.fstt.exception.MetierException     the product was modified by someone else meanwhile
     */
    Produit enregistrerProduit(ProduitForm form);

    /** Withdraws the product from sale, or puts it back. @return true when it is now active */
    boolean basculerProduit(long id);

    /** @throws ma.fstt.exception.MetierException if a cart still contains the product */
    void supprimerProduit(long id);

    /** All categories with their total product count (active or not). */
    List<CategorieVue> categories();

    /** @return the category, or null */
    Categorie categorie(long id);

    /** Creates (form id null) or renames a category of the active storefront. */
    Categorie enregistrerCategorie(CategorieForm form);

    /** @throws ma.fstt.exception.MetierException if the category still contains products */
    void supprimerCategorie(long id);
}
