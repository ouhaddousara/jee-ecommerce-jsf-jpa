package ma.fstt.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Access control, applied to /panier.xhtml (clients only) and /admin/* (administrators only).
 * It sits in front of the JSF servlet, so a new admin page is protected as soon as it
 * is placed under /admin, without anyone having to remember a check.
 *
 * The filter cannot add a FacesMessage (there is no FacesContext yet): it redirects to
 * the login page with ?requis=1 and the login bean shows the message.
 */
public class AuthFilter implements Filter {

    /** Session attribute remembering where to come back after signing in. */
    static final String RETOUR = "retour";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute(SessionUser.CLE);
        boolean zoneAdmin = req.getServletPath().startsWith("/admin");

        if (user == null) {
            // Remember where to come back after signing in (GET only, path built here, not user-supplied).
            if ("GET".equals(req.getMethod())) {
                String retour = req.getServletPath()
                        + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
                req.getSession(true).setAttribute(RETOUR, retour);
            }
            resp.sendRedirect(req.getContextPath() + "/connexion.xhtml?requis=1");
            return;
        }

        if (zoneAdmin ? !user.isAdmin() : !user.isClient()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Cette page n'est pas accessible avec ton compte.");
            return;
        }
        chain.doFilter(request, response);
    }
}
