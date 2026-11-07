package ma.fstt.testing;

import ma.fstt.entities.PanierCheck;
import ma.fstt.jpa.JpaChecks;
import ma.fstt.service.ServiceChecks;
import ma.fstt.util.UtilChecks;
import ma.fstt.web.VuesChecks;
import ma.fstt.web.WebChecks;

/**
 * Runs every check that needs no database and no server.
 * In Eclipse: right-click this class, Run As, Java Application.
 * Exit code 0 when everything passes, 1 otherwise.
 */
public final class Verifications {

    private Verifications() {
    }

    public static void main(String[] args) throws Exception {
        PanierCheck.executer();
        ServiceChecks.executer();
        UtilChecks.executer();
        JpaChecks.executer();
        WebChecks.executer();
        VuesChecks.executer();

        System.out.println();
        System.out.println("Bilan : " + Checks.reussis() + " réussis, " + Checks.echoues() + " échoués.");
        System.out.println(Checks.echoues() == 0 ? "TOUT EST CONFORME" : "DES VERIFICATIONS ONT ECHOUE");
        System.exit(Checks.echoues() == 0 ? 0 : 1);
    }
}
