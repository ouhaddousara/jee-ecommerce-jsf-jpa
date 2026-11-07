package ma.fstt.web;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import ma.fstt.entities.Internaute;
import ma.fstt.exception.MetierException;
import ma.fstt.service.PanierService;

/**
 * Who is signed in, as seen by the pages and the other beans.
 *
 * This bean holds NO state of its own: the single source of truth is the
 * {@link SessionUser} stored in the HTTP session, because {@link AuthFilter}
 * (a servlet filter, outside CDI) must read the very same value. A request-scoped
 * facade avoids keeping a second copy that could drift.
 */
@Named("userSession")
@RequestScoped
public class UserSession {

    /** Session attribute: number of articles in the cart (badge of the layout). */
    static final String COMPTEUR = "panierCount";

    @Inject
    private PanierService paniers;

    private static ExternalContext contexte() {
        return FacesContext.getCurrentInstance().getExternalContext();
    }

    public SessionUser getUtilisateur() {
        return (SessionUser) contexte().getSessionMap().get(SessionUser.CLE);
    }

    public boolean isConnecte() {
        return getUtilisateur() != null;
    }

    public boolean isClient() {
        SessionUser user = getUtilisateur();
        return user != null && user.isClient();
    }

    public boolean isAdmin() {
        SessionUser user = getUtilisateur();
        return user != null && user.isAdmin();
    }

    public String getNomComplet() {
        SessionUser user = getUtilisateur();
        return user == null ? "" : user.getNomComplet();
    }

    public String getRoleLibelle() {
        return isAdmin() ? "Administrateur" : "Client";
    }

    public int getNombreArticles() {
        Object valeur = contexte().getSessionMap().get(COMPTEUR);
        return valeur instanceof Integer ? (Integer) valeur : 0;
    }

    public void memoriserCompteur(int nombre) {
        contexte().getSessionMap().put(COMPTEUR, nombre);
    }

    /** The signed-in client's id: always read from the session, never from a request parameter. */
    public long getClientId() {
        SessionUser user = getUtilisateur();
        if (user == null || !user.isClient()) {
            throw new MetierException("Connecte-toi avec un compte client pour continuer.");
        }
        return user.getId();
    }

    /**
     * Starts an authenticated session and returns the navigation outcome (a redirect)
     * to the page to show next. The session id is changed (session fixation) and the
     * CSRF token rotated.
     */
    public String ouvrir(Internaute internaute) {
        HttpServletRequest req = (HttpServletRequest) contexte().getRequest();
        HttpSession ancienne = req.getSession(true);
        String retour = (String) ancienne.getAttribute(AuthFilter.RETOUR);
        req.changeSessionId();

        HttpSession session = req.getSession();
        session.removeAttribute(AuthFilter.RETOUR);
        SessionUser user = SessionUser.de(internaute);
        session.setAttribute(SessionUser.CLE, user);
        SecurityFilter.nouveauJeton(session);
        session.setAttribute(COMPTEUR,
                user.isClient() ? paniers.consulter(user.getId()).getNombreArticles() : 0);

        String cible;
        if (user.isAdmin()) {
            cible = "/admin/produits.xhtml";
        } else {
            cible = Chemins.cheminSur(retour, "/vitrine.xhtml");
            if (cible.startsWith("/admin")) {
                cible = "/vitrine.xhtml";
            }
        }
        return Chemins.avecParametre(cible, "faces-redirect", "true");
    }

    /**
     * Sign out (a POST button, never a link: a link could be triggered by a third-party
     * page). The message travels as a query parameter because the session, and
     * with it the JSF flash, is destroyed here.
     */
    public String deconnecter() {
        contexte().invalidateSession();
        return "/vitrine.xhtml?faces-redirect=true&deconnecte=1";
    }
}
