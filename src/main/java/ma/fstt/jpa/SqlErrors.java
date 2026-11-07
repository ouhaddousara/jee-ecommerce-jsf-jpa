package ma.fstt.jpa;

import java.sql.SQLException;

import ma.fstt.exception.MetierException;

/**
 * Turns low-level persistence failures into messages a user can understand.
 * The database is the last line of defence (UNIQUE, FOREIGN KEY, CHECK):
 * when it refuses something the services did not anticipate, the user still
 * gets a readable message instead of a stack trace.
 */
public final class SqlErrors {

    private static final int MAX_DEPTH = 12;

    private SqlErrors() {
    }

    /** Returns the exception to throw: a MetierException when the cause is recognised, else the original. */
    public static RuntimeException traduire(RuntimeException e) {
        Throwable t = e;
        for (int depth = 0; t != null && depth < MAX_DEPTH; depth++) {
            if (t instanceof MetierException) {
                return (MetierException) t;
            }
            // Matched by name: both javax.persistence's and EclipseLink's exception are in the chain.
            if ("OptimisticLockException".equals(t.getClass().getSimpleName())) {
                return new MetierException("Ces données viennent d'être modifiées par quelqu'un d'autre. "
                        + "Recharge la page puis recommence.");
            }
            if (t instanceof SQLException) {
                String message = message(((SQLException) t).getErrorCode());
                if (message != null) {
                    return new MetierException(message);
                }
            }
            t = t.getCause() == t ? null : t.getCause();
        }
        return e;
    }

    /** MySQL error codes handled: 1062, 1451, 1452, 1048, 1406, 3819, 4025. */
    static String message(int codeMysql) {
        switch (codeMysql) {
            case 1062:
                return "Cette valeur existe déjà : elle doit être unique.";
            case 1451:
                return "Suppression impossible : cet élément est encore utilisé ailleurs.";
            case 1452:
                return "Référence invalide : l'élément lié n'existe pas.";
            case 1048:
                return "Un champ obligatoire est vide.";
            case 1406:
                return "Une des valeurs saisies est trop longue.";
            case 3819:
            case 4025:
                return "Valeur refusée par une règle d'intégrité de la base (par exemple un stock ou une quantité négatifs).";
            default:
                return null;
        }
    }
}
