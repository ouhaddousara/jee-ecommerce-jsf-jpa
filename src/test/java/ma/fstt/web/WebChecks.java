package ma.fstt.web;

import static ma.fstt.testing.Checks.check;
import static ma.fstt.testing.Checks.section;

import java.math.BigDecimal;

/**
 * Checks of the pure parts of the web layer (no server, no JSF runtime needed):
 * redirect safety, CSRF comparison, storefront addresses and display helpers.
 */
public final class WebChecks {

    private WebChecks() {
    }

    public static void executer() throws Exception {
        section("Redirection sûre (anti open-redirect)");
        check("chemin interne accepté", "/vitrine.xhtml?categorie=2".equals(Chemins.cheminSur("/vitrine.xhtml?categorie=2", "/x")));
        check("//site-externe refusé", "/x".equals(Chemins.cheminSur("//evil.example", "/x")));
        check("URL absolue refusée", "/x".equals(Chemins.cheminSur("https://evil.example", "/x")));
        check("chemin sans / initial refusé", "/x".equals(Chemins.cheminSur("vitrine", "/x")));
        check("antislash refusé", "/x".equals(Chemins.cheminSur("/\\evil.example", "/x")));
        check("saut de ligne refusé (injection d'en-tête)",
                "/x".equals(Chemins.cheminSur("/a\r\nSet-Cookie: x=1", "/x")));
        check("null refusé", "/x".equals(Chemins.cheminSur(null, "/x")));

        section("Paramètre ajouté à un chemin");
        check("sans requête : ?", "/a.xhtml?faces-redirect=true".equals(Chemins.avecParametre("/a.xhtml", "faces-redirect", "true")));
        check("avec requête : &", "/a.xhtml?x=1&faces-redirect=true".equals(Chemins.avecParametre("/a.xhtml?x=1", "faces-redirect", "true")));

        section("Jeton CSRF");
        check("jetons identiques", SecurityFilter.egal("abc123", "abc123"));
        check("jetons différents", !SecurityFilter.egal("abc123", "abc124"));
        check("jeton absent refusé", !SecurityFilter.egal("abc123", null) && !SecurityFilter.egal(null, "abc123"));
        check("longueurs différentes refusées", !SecurityFilter.egal("abc123", "abc"));

        section("Politique de sécurité du contenu (CSP)");
        check("scripts du serveur seulement", SecurityFilter.CSP.contains("script-src 'self';"));
        check("aucun script en ligne autorisé", !SecurityFilter.CSP.contains("script-src 'self' 'unsafe-inline'"));
        check("pas d'encadrement par un autre site", SecurityFilter.CSP.contains("frame-ancestors 'none'"));

        section("Adresses de la vitrine (filtres conservés)");
        check("sans filtre", "/app/vitrine.xhtml".equals(VitrineBean.construireUrl("/app", null, "", null)));
        check("catégorie seule", "/app/vitrine.xhtml?categorie=2".equals(VitrineBean.construireUrl("/app", 2L, "", null)));
        check("recherche encodée en UTF-8", "/app/vitrine.xhtml?q=caf%C3%A9+noir".equals(VitrineBean.construireUrl("/app", null, "café noir", null)));
        check("tous les filtres, dans l'ordre", "/app/vitrine.xhtml?categorie=2&q=cle&page=3".equals(VitrineBean.construireUrl("/app", 2L, "cle", 3)));
        check("la page 1 est omise", "/app/vitrine.xhtml?categorie=2".equals(VitrineBean.construireUrl("/app", 2L, "", 1)));
        check("& dans la recherche est encodé (pas d'injection de paramètre)",
                "/app/vitrine.xhtml?q=a%26page%3D9".equals(VitrineBean.construireUrl("/app", null, "a&page=9", null)));

        section("Aides d'affichage");
        Affichage affichage = new Affichage();
        String prix = affichage.prix(new BigDecimal("1007"));
        check("prix : deux décimales, virgule, MAD", prix.contains("007,00") && prix.endsWith("MAD"));
        check("prix : espace insécable avant la devise", prix.contains(" MAD"));
        check("prix null : chaîne vide", affichage.prix(null).isEmpty());
        check("stock épuisé", "stock-out".equals(affichage.stockClasse(0)) && "Épuisé".equals(affichage.stockLibelle(0)));
        check("dernières unités (3)", "stock-low".equals(affichage.stockClasse(3)) && affichage.stockLibelle(3).endsWith("3"));
        check("en stock", "stock-ok".equals(affichage.stockClasse(6)) && "En stock".equals(affichage.stockLibelle(6)));
        check("initiales en majuscules", "CL".equals(affichage.initiales("clavier")));
        check("initiales d'un nom d'une lettre", "A".equals(affichage.initiales("a")));
        check("icône inconnue : coche par défaut", "M5 12l5 5L20 7".equals(affichage.icone("n'existe-pas")));
        check("icône connue", affichage.icone("panier").startsWith("M6 7"));
        check("bouton d'ajout : libellé et état selon le stock",
                "Indisponible".equals(affichage.libelleAjout(0)) && affichage.indisponible(0)
                        && "Ajouter au panier".equals(affichage.libelleAjout(1)) && !affichage.indisponible(1));

        section("Page d'erreur");
        check("404 : page introuvable", "Page introuvable".equals(affichage.titreErreur(404)));
        check("403 : accès refusé, avec le détail du conteneur",
                "Accès refusé".equals(affichage.titreErreur(403))
                        && "Détail".equals(affichage.texteErreur(403, "Détail")));
        check("403 sans détail : texte par défaut", affichage.texteErreur(403, null).startsWith("Tu n"));
        check("405 : action non autorisée", "Action non autorisée".equals(affichage.titreErreur(405)));
        check("code absent : 500", affichage.codeErreur(null) == 500 && "Une erreur est survenue".equals(affichage.titreErreur(null)));
        check("code numérique lu tel quel", affichage.codeErreur(Integer.valueOf(404)) == 404);
    }
}
