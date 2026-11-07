package ma.fstt.jpa;

import static ma.fstt.testing.Checks.check;
import static ma.fstt.testing.Checks.section;

import java.sql.SQLException;

import ma.fstt.exception.MetierException;

/** Translation of database errors and LIKE escaping (no database needed). */
public final class JpaChecks {

    private JpaChecks() {
    }

    /** Same simple name as the real exception: SqlErrors recognises it by name. */
    static class OptimisticLockException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        OptimisticLockException(Throwable cause) {
            super(cause);
        }
    }

    public static void executer() {
        section("Traduction des erreurs MySQL");

        RuntimeException doublon = new RuntimeException("wrapper",
                new RuntimeException("EclipseLink", new SQLException("Duplicate entry", "23000", 1062)));
        RuntimeException traduit = SqlErrors.traduire(doublon);
        check("1062 (doublon) devient un MetierException lisible",
                traduit instanceof MetierException && traduit.getMessage().contains("existe déjà"));

        check("1451 (clé étrangère) : message de suppression impossible",
                SqlErrors.traduire(new RuntimeException(new SQLException("fk", "23000", 1451)))
                        .getMessage().contains("Suppression impossible"));
        check("1452 : référence invalide",
                SqlErrors.traduire(new RuntimeException(new SQLException("fk", "23000", 1452)))
                        .getMessage().contains("Référence invalide"));
        check("3819 (CHECK) : règle d'intégrité",
                SqlErrors.traduire(new RuntimeException(new SQLException("chk", "HY000", 3819)))
                        .getMessage().contains("intégrité"));
        check("4025 (CHECK) : même message que 3819",
                SqlErrors.message(4025).equals(SqlErrors.message(3819)));

        RuntimeException conflit = SqlErrors.traduire(
                new RuntimeException("commit", new OptimisticLockException(new RuntimeException("version"))));
        check("conflit de version : message « modifié par quelqu'un d'autre »",
                conflit instanceof MetierException && conflit.getMessage().contains("modifiées par quelqu'un d'autre"));

        MetierException deja = new MetierException("déjà lisible");
        check("un MetierException traverse sans être modifié", SqlErrors.traduire(deja) == deja);

        RuntimeException inconnue = new RuntimeException(new SQLException("autre", "42000", 1064));
        check("code inconnu : l'exception d'origine est conservée", SqlErrors.traduire(inconnue) == inconnue);

        section("Échappement des jokers LIKE");
        check("% _ et ! sont neutralisés avec ! comme caractère d'échappement",
                JpaCatalogueService.echapper("50%_off!").equals("50!%!_off!!"));
        check("un texte ordinaire est inchangé", JpaCatalogueService.echapper("clavier").equals("clavier"));
    }
}
