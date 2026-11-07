package ma.fstt.service;

import static ma.fstt.testing.Checks.check;
import static ma.fstt.testing.Checks.section;
import static ma.fstt.testing.Checks.thrown;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import ma.fstt.exception.ValidationException;

/** Form validation rules and pagination arithmetic (no database needed). */
public final class ServiceChecks {

    private ServiceChecks() {
    }

    public static void executer() {
        section("Validation des formulaires");

        Validation v = new Validation();
        check("e-mail valide, normalisé en minuscules",
                "yasmine@example.com".equals(v.email("email", "  Yasmine@Example.COM ")));
        check("aucune erreur pour un e-mail valide", v.valide());

        v = new Validation();
        check("e-mail sans arobase refusé", v.email("email", "yasmine.example.com") == null && !v.valide());
        v = new Validation();
        check("e-mail vide refusé", v.email("email", "  ") == null && !v.valide());

        v = new Validation();
        check("mot de passe correct accepté", "Client#2026".equals(v.motDePasse("mdp", "Client#2026", "Client#2026")));
        v = new Validation();
        check("mot de passe trop court refusé", v.motDePasse("mdp", "ab1", "ab1") == null && !v.valide());
        v = new Validation();
        check("mot de passe sans chiffre refusé", v.motDePasse("mdp", "abcdefghij", "abcdefghij") == null && !v.valide());
        v = new Validation();
        check("mot de passe sans lettre refusé", v.motDePasse("mdp", "1234567890", "1234567890") == null && !v.valide());
        v = new Validation();
        v.motDePasse("mdp", "Client#2026", "Autre#2026");
        check("confirmation différente : erreur sur le champ confirmation", !v.valide() && thrownFields(v).contains("confirmation"));

        v = new Validation();
        check("nom composé accepté", "El Idrissi".equals(v.nomPropre("nom", "El Idrissi", "Le nom")));
        check("apostrophe et tiret acceptés",
                "O'Brien-Smith".equals(v.nomPropre("nom", "O'Brien-Smith", "Le nom")) && v.valide());
        check("accents acceptés", "Zoë".equals(v.nomPropre("nom", "Zoë", "Le nom")) && v.valide());
        v = new Validation();
        check("balise HTML refusée dans un nom", v.nomPropre("nom", "<script>", "Le nom") == null && !v.valide());

        v = new Validation();
        check("prix 129,90 (virgule) accepté", new BigDecimal("129.90").equals(
                v.decimal("prix", "129,90", "Le prix", BigDecimal.ZERO, new BigDecimal("9999999.99"))));
        check("prix 1 299,50 (espace) accepté", new BigDecimal("1299.50").equals(
                v.decimal("prix", "1 299,50", "Le prix", BigDecimal.ZERO, new BigDecimal("9999999.99"))));
        v = new Validation();
        check("prix à 3 décimales refusé", v.decimal("prix", "1,999", "Le prix", BigDecimal.ZERO, BigDecimal.TEN) == null && !v.valide());
        v = new Validation();
        check("prix négatif refusé", v.decimal("prix", "-1", "Le prix", BigDecimal.ZERO, BigDecimal.TEN) == null && !v.valide());
        v = new Validation();
        check("prix non numérique refusé", v.decimal("prix", "abc", "Le prix", BigDecimal.ZERO, BigDecimal.TEN) == null && !v.valide());
        v = new Validation();
        check("prix au-dessus du maximum refusé", v.decimal("prix", "11", "Le prix", BigDecimal.ZERO, BigDecimal.TEN) == null && !v.valide());

        v = new Validation();
        check("stock entier accepté", Integer.valueOf(12).equals(v.entier("stock", "12", "Le stock", 0, 100)));
        v = new Validation();
        check("stock décimal refusé", v.entier("stock", "1.5", "Le stock", 0, 100) == null && !v.valide());
        v = new Validation();
        check("stock négatif refusé", v.entier("stock", "-1", "Le stock", 0, 100) == null && !v.valide());

        v = new Validation();
        check("référence normalisée en majuscules", "INF-001".equals(v.reference("reference", "inf-001")));
        v = new Validation();
        check("référence avec espace refusée", v.reference("reference", "inf 001") == null && !v.valide());

        v = new Validation();
        check("image https acceptée",
                "https://exemple.ma/a.png".equals(v.urlHttps("imageUrl", "https://exemple.ma/a.png")) && v.valide());
        check("image vide acceptée (facultative)", v.urlHttps("imageUrl", "  ") == null && v.valide());
        v = new Validation();
        check("javascript: refusé", v.urlHttps("imageUrl", "javascript:alert(1)") == null && !v.valide());
        v = new Validation();
        check("http (non sécurisé) refusé", v.urlHttps("imageUrl", "http://exemple.ma/a.png") == null && !v.valide());

        v = new Validation();
        check("téléphone marocain accepté", "+212 6 12 34 56 78".equals(v.telephone("tel", "+212 6 12 34 56 78")));
        v = new Validation();
        check("téléphone invalide refusé", v.telephone("tel", "abc") == null && !v.valide());
        check("téléphone vide accepté (facultatif)", new Validation().telephone("tel", "") == null);

        v = new Validation();
        v.nomPropre("nom", "", "Le nom");
        v.nomPropre("prenom", "", "Le prénom");
        v.rejeter("nom", "second message ignoré");
        check("toutes les erreurs sont collectées, la première est gardée",
                thrownFields(v).equals(List.of("nom", "prenom")));
        check("verifier() lève ValidationException", ValidationException.class == thrown(v::verifier));
        check("verifier() ne lève rien sans erreur", thrown(new Validation()::verifier) == null);

        section("Pagination");
        PageResultat<String> page3 = new PageResultat<>(List.of("a", "b"), 3, 9, 20);
        check("20 éléments par 9 : 3 pages", page3.getTotalPages() == 3);
        check("page 3 : éléments 19 à 20", page3.getDebut() == 19 && page3.getFin() == 20);
        check("page 3 : précédente oui, suivante non", page3.isPrecedente() && !page3.isSuivante());
        PageResultat<String> vide = new PageResultat<>(Collections.<String>emptyList(), 1, 9, 0);
        check("résultat vide : 1 page, début et fin à 0",
                vide.getTotalPages() == 1 && vide.getDebut() == 0 && vide.getFin() == 0
                        && !vide.isPrecedente() && !vide.isSuivante());
    }

    private static List<String> thrownFields(Validation v) {
        try {
            v.verifier();
            return Collections.emptyList();
        } catch (ValidationException e) {
            return List.copyOf(e.getErreurs().keySet());
        }
    }
}
