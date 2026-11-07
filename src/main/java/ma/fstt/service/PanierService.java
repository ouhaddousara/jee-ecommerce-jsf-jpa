package ma.fstt.service;

import ma.fstt.entities.Panier;

/**
 * Cart operations. Every method takes the client id from the SESSION, never
 * from the request, so a client can only ever touch their own cart. The
 * returned cart is fully loaded (lines and products) and safe to display.
 */
public interface PanierService {

    Panier consulter(long clientId);

    /** @throws ma.fstt.exception.MetierException invalid quantity, inactive product, stock exceeded */
    Panier ajouter(long clientId, long produitId, int quantite);

    /** Sets the exact quantity of a line. */
    Panier modifierQuantite(long clientId, long produitId, int quantite);

    Panier retirer(long clientId, long produitId);

    Panier vider(long clientId);
}
