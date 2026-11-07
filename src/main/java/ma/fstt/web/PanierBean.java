package ma.fstt.web;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ma.fstt.entities.LignePanier;
import ma.fstt.entities.Panier;
import ma.fstt.exception.MetierException;
import ma.fstt.service.PanierService;

/**
 * Cart (panier.xhtml) and the "add to cart" buttons of the storefront and the product page.
 *
 * The client id always comes from the session ({@link UserSession#getClientId()}),
 * never from the request. Every action ends with a redirect (Post/Redirect/Get) and
 * a message, so refreshing the page never sends the form again.
 */
@Named
@ViewScoped
public class PanierBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private PanierService paniers;

    @Inject
    private UserSession userSession;

    // Reloaded lazily: entities are not Serializable and go stale.
    private transient Panier panier;
    private transient Map<Long, String> quantites;

    public Panier getPanier() {
        if (panier == null) {
            panier = paniers.consulter(userSession.getClientId());
            userSession.memoriserCompteur(panier.getNombreArticles());
        }
        return panier;
    }

    /** Typed quantity of each line, by product id (text, so a bad value reaches the business rules). */
    public Map<Long, String> getQuantites() {
        if (quantites == null) {
            quantites = new LinkedHashMap<>();
            for (LignePanier ligne : getPanier().getLignes()) {
                quantites.put(ligne.getProduit().getId(), String.valueOf(ligne.getQuantite()));
            }
        }
        return quantites;
    }

    /**
     * Adds a product, then goes back to the page the button was on
     * ("vitrine" or "detail", keeping its filters through the view parameters).
     */
    public String ajouter(long produitId, String quantite, String retour) {
        String page = "detail".equals(retour) || "panier".equals(retour) ? retour : "vitrine";
        String cible = "/" + page + ".xhtml?faces-redirect=true&includeViewParams=true";
        return executer(cible, "Produit ajouté au panier.",
                () -> paniers.ajouter(userSession.getClientId(), produitId, entier(quantite, 1)));
    }

    public String modifier(long produitId) {
        String saisie = getQuantites().get(produitId);
        return executer("/panier.xhtml?faces-redirect=true", "Quantité mise à jour.",
                () -> paniers.modifierQuantite(userSession.getClientId(), produitId, entier(saisie, 1)));
    }

    public String retirer(long produitId) {
        return executer("/panier.xhtml?faces-redirect=true", "Produit retiré du panier.",
                () -> paniers.retirer(userSession.getClientId(), produitId));
    }

    public String vider() {
        return executer("/panier.xhtml?faces-redirect=true", "Panier vidé.",
                () -> paniers.vider(userSession.getClientId()));
    }

    /** One operation of the cart service: returns the cart after the change. */
    private interface Operation {
        Panier appliquer();
    }

    private String executer(String cible, String messageSucces, Operation operation) {
        try {
            Panier resultat = operation.appliquer();
            userSession.memoriserCompteur(resultat.getNombreArticles());
            Messages.succes(messageSucces);
        } catch (MetierException e) {
            Messages.erreur(e.getMessage());
        }
        panier = null;
        quantites = null;
        Messages.apresRedirection();
        return cible;
    }

    /** The default when absent; 0 (rejected later by the business rules) when malformed. */
    private static int entier(String brut, int parDefaut) {
        if (brut == null || brut.trim().isEmpty()) {
            return parDefaut;
        }
        try {
            return Integer.parseInt(brut.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
