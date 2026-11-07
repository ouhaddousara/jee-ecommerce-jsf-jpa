package ma.fstt.web;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * Small display helpers used by the Facelets pages (#{affichage.prix(...)}):
 * formatting and icon paths live in Java, where they are testable, instead of
 * long conditional EL expressions in the markup.
 */
@Named("affichage")
@ApplicationScoped
public class Affichage implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Map<String, String> ICONES = new HashMap<>();

    static {
        ICONES.put("vitrine", "M3 9l1.5-5h15L21 9M3 9v11h18V9M3 9h18M9 20v-6h6v6");
        ICONES.put("panier", "M6 7h12l1 13H5L6 7zM9 7a3 3 0 016 0");
        ICONES.put("utilisateur", "M12 12a4 4 0 100-8 4 4 0 000 8zM4 21a8 8 0 0116 0");
        ICONES.put("produits", "M3 7l9-4 9 4v10l-9 4-9-4V7zM3 7l9 4 9-4M12 11v10");
        ICONES.put("categories", "M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z");
        ICONES.put("recherche", "M11 19a8 8 0 100-16 8 8 0 000 16zM21 21l-4.3-4.3");
        ICONES.put("fermer", "M6 6l12 12M18 6L6 18");
        ICONES.put("corbeille", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3");
        ICONES.put("plus", "M12 5v14M5 12h14");
        ICONES.put("menu", "M4 6h16M4 12h16M4 18h16");
        ICONES.put("retour", "M15 6l-6 6 6 6");
    }

    /** SVG path of an icon; a check mark for an unknown name. */
    public String icone(String nom) {
        String chemin = ICONES.get(nom);
        return chemin == null ? "M5 12l5 5L20 7" : chemin;
    }

    /** "1 007,00 MAD": two decimals, French grouping, non-breaking space before the currency. */
    public String prix(BigDecimal montant) {
        if (montant == null) {
            return "";
        }
        NumberFormat format = NumberFormat.getNumberInstance(Locale.FRANCE);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(montant) + " MAD";
    }

    /** First two letters of a product name, in capitals (picture placeholder). */
    public String initiales(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            return "";
        }
        String propre = nom.trim();
        return propre.substring(0, Math.min(2, propre.length())).toUpperCase(Locale.ROOT);
    }

    /** CSS class of the stock badge: stock-out, stock-low or stock-ok. */
    public String stockClasse(int stock) {
        return stock <= 0 ? "stock-out" : (stock <= 5 ? "stock-low" : "stock-ok");
    }

    public String stockLibelle(int stock) {
        if (stock <= 0) {
            return "Épuisé";
        }
        return stock <= 5 ? "Dernières unités · " + stock : "En stock";
    }

    /** Label of the "add to cart" button of a product card. */
    public String libelleAjout(int stock) {
        return stock <= 0 ? "Indisponible" : "Ajouter au panier";
    }

    /** True when the product cannot be added to a cart. */
    public boolean indisponible(int stock) {
        return stock <= 0;
    }

    // ---- error page (erreur.xhtml): the status code comes from the servlet container ----

    /** The HTTP status code as an int; 500 when absent (uncaught exception) or not a number. */
    public int codeErreur(Object code) {
        return code instanceof Number ? ((Number) code).intValue() : 500;
    }

    public String titreErreur(Object code) {
        switch (codeErreur(code)) {
            case 404:
                return "Page introuvable";
            case 403:
                return "Accès refusé";
            case 405:
                return "Action non autorisée";
            default:
                return "Une erreur est survenue";
        }
    }

    /** @param detail the container's error message (used for 403, where it explains why) */
    public String texteErreur(Object code, Object detail) {
        switch (codeErreur(code)) {
            case 404:
                return "Cette page n\u2019existe pas ou n\u2019existe plus.";
            case 403:
                return detail == null || detail.toString().isEmpty()
                        ? "Tu n\u2019as pas le droit d\u2019ouvrir cette page."
                        : detail.toString();
            case 405:
                return "Cette adresse n\u2019accepte pas ce type de requ\u00eate.";
            default:
                return "Le probl\u00e8me a \u00e9t\u00e9 enregistr\u00e9. R\u00e9essaie dans un instant.";
        }
    }
}
