package ma.fstt.testing;

/**
 * Minimal assertion helper: no test framework, so the checks run with a plain
 * "Run As > Java Application" in Eclipse and never slow down or break the Maven build.
 */
public final class Checks {

    private static int reussis;
    private static int echoues;

    private Checks() {
    }

    /** Code that may throw; used to assert that a rule is enforced. */
    public interface Action {
        void run() throws Exception;
    }

    public static void section(String titre) {
        System.out.println();
        System.out.println("== " + titre);
    }

    public static void check(String nom, boolean condition) {
        if (condition) {
            reussis++;
            System.out.println("  OK    " + nom);
        } else {
            echoues++;
            System.out.println("  ECHEC " + nom);
        }
    }

    /** The class of the exception thrown by the action, or null when nothing was thrown. */
    public static Class<?> thrown(Action action) {
        try {
            action.run();
            return null;
        } catch (Exception e) {
            return e.getClass();
        }
    }

    public static int reussis() {
        return reussis;
    }

    public static int echoues() {
        return echoues;
    }
}
