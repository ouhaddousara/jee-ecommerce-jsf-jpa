package ma.fstt.entities;

import static ma.fstt.testing.Checks.check;
import static ma.fstt.testing.Checks.section;
import static ma.fstt.testing.Checks.thrown;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import ma.fstt.exception.MetierException;
import ma.fstt.exception.StockInsuffisantException;

/**
 * Business rules of the domain model, checked WITHOUT a database: the
 * entities are plain objects until a persistence provider gets hold of them.
 */
public final class PanierCheck {

    private PanierCheck() {
    }

    public static void executer() throws Exception {
        section("Panier : ajout, quantités, stock");

        Client c = new Client("Benani", "Yasmine", "  Yasmine@Example.COM ", "h", "adr", "tel");
        Panier p = c.getPanier();
        check("un client reçoit un panier vide à la création", p != null && p.isVide());
        check("e-mail nettoyé (espaces, majuscules)", "yasmine@example.com".equals(c.getEmail()));
        check("rôle CLIENT", "CLIENT".equals(c.getRole()));

        Produit clavier = produit(1, "INF-001", "549.00", 25, true);
        Produit souris = produit(2, "INF-002", "229.00", 40, true);
        Produit ssd = produit(3, "INF-003", "899.00", 3, true);
        Produit epuise = produit(4, "MAI-002", "199.00", 0, true);
        Produit retire = produit(5, "MOD-003", "159.00", 7, false);

        p.ajouterProduit(clavier, 1);
        p.ajouterProduit(souris, 2);
        check("total 549 + 2 x 229 = 1007.00", new BigDecimal("1007.00").equals(p.getTotal()));
        check("2 lignes, 3 articles", p.getLignes().size() == 2 && p.getNombreArticles() == 3);

        p.ajouterProduit(produit(2, "INF-002", "229.00", 40, true), 1);
        check("ré-ajouter fusionne dans la ligne existante (égalité par id)",
                p.getLignes().size() == 2 && p.getNombreArticles() == 4);

        clavier.setPrix(new BigDecimal("999.00"));
        p.ajouterProduit(clavier, 1);
        check("le prix reste figé à celui du premier ajout",
                new BigDecimal("549.00").equals(p.getLignes().get(0).getPrixUnitaire()));

        check("quantité 0 refusée", MetierException.class == thrown(() -> p.ajouterProduit(souris, 0)));
        check("quantité négative refusée", MetierException.class == thrown(() -> p.ajouterProduit(souris, -3)));
        check("quantité 100 refusée", MetierException.class == thrown(() -> p.ajouterProduit(souris, 100)));
        check("Integer.MAX_VALUE refusé sans débordement",
                MetierException.class == thrown(() -> p.ajouterProduit(souris, Integer.MAX_VALUE)));
        check("produit retiré de la vente refusé", MetierException.class == thrown(() -> p.ajouterProduit(retire, 1)));
        check("produit null refusé", MetierException.class == thrown(() -> p.ajouterProduit(null, 1)));
        check("produit épuisé refusé", StockInsuffisantException.class == thrown(() -> p.ajouterProduit(epuise, 1)));

        p.ajouterProduit(ssd, 3);
        check("exactement le stock est accepté", p.getLignes().get(2).getQuantite() == 3);
        check("stock + 1 refusé", StockInsuffisantException.class == thrown(() -> p.ajouterProduit(ssd, 1)));
        try {
            p.ajouterProduit(ssd, 1);
        } catch (StockInsuffisantException e) {
            check("l'exception porte le stock disponible", e.getStockDisponible() == 3);
        }

        p.modifierQuantite(souris, 10);
        check("modifierQuantite fixe la quantité exacte", p.getLignes().get(1).getQuantite() == 10);
        check("modifierQuantite au-dessus du stock refusé",
                StockInsuffisantException.class == thrown(() -> p.modifierQuantite(souris, 41)));
        check("modifierQuantite à 0 refusé", MetierException.class == thrown(() -> p.modifierQuantite(souris, 0)));
        check("modifierQuantite d'un produit absent refusé",
                MetierException.class == thrown(() -> p.modifierQuantite(epuise, 1)));
        check("une mise à jour refusée laisse la ligne intacte", p.getLignes().get(1).getQuantite() == 10);

        p.retirerProduit(souris);
        check("retirerProduit supprime la ligne", p.getLignes().size() == 2);
        check("retirer un produit absent refusé", MetierException.class == thrown(() -> p.retirerProduit(souris)));
        p.vider();
        check("vider remet le panier à zéro",
                p.isVide() && new BigDecimal("0.00").equals(p.getTotal()) && p.getNombreArticles() == 0);
        check("la liste des lignes est en lecture seule",
                UnsupportedOperationException.class == thrown(() -> p.getLignes().clear()));

        section("Produit, Internaute, relations");
        check("prix négatif refusé",
                MetierException.class == thrown(() -> new Produit("X", "n", "d", new BigDecimal("-1"), 1, true)));
        check("stock négatif refusé",
                MetierException.class == thrown(() -> new Produit("X", "n", "d", BigDecimal.ONE, -1, true)));
        check("prix arrondi à 2 décimales",
                new BigDecimal("10.13").equals(new Produit("X", "n", "d", new BigDecimal("10.125"), 1, true).getPrix()));
        check("e-mail vide refusé", MetierException.class == thrown(() -> c.setEmail("  ")));

        Vitrine v = new Vitrine("T", "d", true);
        Categorie cat = v.ajouterCategorie(new Categorie("C", "d"));
        Produit pr = cat.ajouterProduit(new Produit("R", "n", "d", BigDecimal.TEN, 1, true));
        check("les deux côtés de vitrine / catégorie / produit sont renseignés",
                cat.getVitrine() == v && pr.getCategorie() == cat
                        && v.getCategories().contains(cat) && cat.getProduits().contains(pr));
    }

    private static Produit produit(long id, String ref, String prix, int stock, boolean actif) throws Exception {
        Produit p = new Produit(ref, "Produit " + ref, "d", new BigDecimal(prix), stock, actif);
        Field f = Produit.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(p, id);
        return p;
    }
}
