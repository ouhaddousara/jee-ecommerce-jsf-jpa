package ma.fstt.jpa;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import ma.fstt.entities.Client;
import ma.fstt.entities.LignePanier;
import ma.fstt.entities.Panier;
import ma.fstt.entities.Produit;
import ma.fstt.exception.MetierException;
import ma.fstt.service.PanierService;

/**
 * The business rules (quantity, stock, active product, frozen price) live in
 * Panier itself. This class only loads the right cart, applies the operation
 * inside one transaction and returns the cart fully loaded.
 *
 * The product is read with an OPTIMISTIC lock: if an administrator changes
 * its stock between the check and the commit, the commit fails and the user
 * is asked to retry, instead of validating against a stale stock.
 */
@ApplicationScoped
public class JpaPanierService implements PanierService {

    @Inject
    private TransactionRunner tx;

    @Override
    public Panier consulter(long clientId) {
        return tx.read(em -> materialiser(panierDe(em, clientId)));
    }

    @Override
    public Panier ajouter(long clientId, long produitId, int quantite) {
        return tx.write(em -> {
            Panier panier = panierDe(em, clientId);
            panier.ajouterProduit(produit(em, produitId), quantite);
            return materialiser(panier);
        });
    }

    @Override
    public Panier modifierQuantite(long clientId, long produitId, int quantite) {
        return tx.write(em -> {
            Panier panier = panierDe(em, clientId);
            panier.modifierQuantite(produit(em, produitId), quantite);
            return materialiser(panier);
        });
    }

    @Override
    public Panier retirer(long clientId, long produitId) {
        return tx.write(em -> {
            Panier panier = panierDe(em, clientId);
            panier.retirerProduit(produit(em, produitId));
            return materialiser(panier);
        });
    }

    @Override
    public Panier vider(long clientId) {
        return tx.write(em -> {
            Panier panier = panierDe(em, clientId);
            panier.vider();
            return materialiser(panier);
        });
    }

    private static Panier panierDe(EntityManager em, long clientId) {
        Client client = em.find(Client.class, clientId);
        if (client == null) {
            throw new MetierException("Compte client introuvable.");
        }
        return client.getPanier();
    }

    private static Produit produit(EntityManager em, long produitId) {
        Produit produit = em.find(Produit.class, produitId, LockModeType.OPTIMISTIC);
        if (produit == null) {
            throw new MetierException("Produit introuvable.");
        }
        return produit;
    }

    /** Loads everything the views read, while the EntityManager is still open. */
    private static Panier materialiser(Panier panier) {
        for (LignePanier ligne : panier.getLignes()) {
            Produit produit = ligne.getProduit();
            produit.getNom();
            produit.getCategorie().getId();
        }
        return panier;
    }
}
