package ma.fstt.web;

import javax.enterprise.context.ApplicationScoped;

import ma.fstt.util.LoginThrottle;

/**
 * Application-wide holder of the sign-in throttle: 5 failed sign-ins per
 * (address + e-mail) in 15 minutes, then a pause. In workshop 2 this was a
 * field of the servlet; a view-scoped bean cannot hold it (one instance per page).
 */
@ApplicationScoped
public class LoginGuard {

    private final LoginThrottle throttle = new LoginThrottle(5, 15 * 60 * 1000L);

    public long secondesRestantes(String cle) {
        return throttle.secondesRestantes(cle);
    }

    public void echec(String cle) {
        throttle.echec(cle);
    }

    public void reussite(String cle) {
        throttle.reussite(cle);
    }
}
