package ma.fstt.util;

import static ma.fstt.testing.Checks.check;
import static ma.fstt.testing.Checks.section;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/** Password hashing, login throttling and money formatting. */
public final class UtilChecks {

    private UtilChecks() {
    }

    public static void executer() {
        section("Hachage des mots de passe (PBKDF2)");
        String hash = PasswordHasher.hash("Client#2026");
        check("le bon mot de passe est vérifié", PasswordHasher.verify("Client#2026", hash));
        check("un mauvais mot de passe est refusé", !PasswordHasher.verify("client#2026", hash));
        check("deux hachages du même mot de passe diffèrent (sel)", !hash.equals(PasswordHasher.hash("Client#2026")));
        check("le mot de passe n'apparaît pas dans le hachage", !hash.contains("Client"));
        check("une valeur stockée mal formée donne false",
                !PasswordHasher.verify("x", "garbage") && !PasswordHasher.verify("x", "a$b$c$d")
                        && !PasswordHasher.verify(null, hash) && !PasswordHasher.verify("x", null));

        section("Limitation des tentatives de connexion");
        AtomicLong horloge = new AtomicLong(1_000_000L);
        LoginThrottle throttle = new LoginThrottle(5, 15 * 60 * 1000L, horloge::get);
        String cle = "127.0.0.1|yasmine@example.com";
        check("aucun échec : accès libre", throttle.secondesRestantes(cle) == 0);
        for (int i = 0; i < 4; i++) {
            throttle.echec(cle);
        }
        check("4 échecs sur 5 autorisés : encore libre", throttle.secondesRestantes(cle) == 0);
        throttle.echec(cle);
        check("5e échec : bloqué", throttle.secondesRestantes(cle) > 0);
        check("l'attente annoncée est de 15 minutes au plus", throttle.secondesRestantes(cle) <= 15 * 60);
        check("une autre clé n'est pas bloquée", throttle.secondesRestantes("127.0.0.1|autre@example.com") == 0);
        horloge.addAndGet(14 * 60 * 1000L);
        check("14 minutes plus tard : toujours bloqué", throttle.secondesRestantes(cle) > 0);
        horloge.addAndGet(2 * 60 * 1000L);
        check("16 minutes plus tard : débloqué", throttle.secondesRestantes(cle) == 0);
        for (int i = 0; i < 5; i++) {
            throttle.echec(cle);
        }
        throttle.reussite(cle);
        check("une connexion réussie remet le compteur à zéro", throttle.secondesRestantes(cle) == 0);

        section("Format des montants");
        String formate = Money.format(new BigDecimal("1007"));
        check("1007 devient 1 007,00 (séparateur de milliers, virgule)",
                formate.replaceAll("\\D", "").equals("100700") && formate.endsWith(",00"));
        check("549.5 devient 549,50", Money.format(new BigDecimal("549.5")).equals("549,50"));
    }
}
