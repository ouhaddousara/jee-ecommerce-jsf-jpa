package ma.fstt.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * First filter of every request:
 * <ul>
 *   <li>UTF-8 for the request, so accents survive the form to the database;</li>
 *   <li>defensive response headers (no framing, no MIME sniffing, strict CSP);</li>
 *   <li>CSRF: every POST must carry the session token as field "_csrf"
 *       (each JSF form includes it through the {@code t:csrf} component).</li>
 * </ul>
 * Static files under /assets/ only get the headers.
 *
 * The JSF ViewState is NOT relied upon as an anti-CSRF token: its entropy depends
 * on the JSF implementation and its settings, so the explicit token from
 * workshop 2 is kept (defense in depth, same tested code).
 */
public class SecurityFilter implements Filter {

    /** Session attribute holding the CSRF token (also read by the t:csrf component). */
    public static final String JETON = "csrf";

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * No inline script anywhere: scripts come from this server only. This is why the
     * views use h:commandButton and h:link (plain markup) and never h:commandLink or
     * f:ajax, which render inline onclick handlers. Styles allow small style="" values.
     */
    static final String CSP = "default-src 'self'; img-src 'self' data: https:; "
            + "style-src 'self' 'unsafe-inline'; script-src 'self'; "
            + "frame-ancestors 'none'; form-action 'self'; base-uri 'self'";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Must happen before the first getParameter() of the request.
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());

        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Referrer-Policy", "same-origin");
        resp.setHeader("Content-Security-Policy", CSP);

        if (req.getServletPath().startsWith("/assets/")) {
            chain.doFilter(request, response);
            return;
        }

        // Pages depend on who is signed in: never cache them.
        resp.setHeader("Cache-Control", "no-store");

        HttpSession session = req.getSession(true);
        String jeton = (String) session.getAttribute(JETON);
        if (jeton == null) {
            jeton = nouveauJeton(session);
        }

        if (estModification(req.getMethod())) {
            String fourni = req.getParameter("_csrf");
            if (!egal(jeton, fourni)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Requête refusée : jeton de sécurité invalide ou session expirée. "
                                + "Recharge la page puis recommence.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /** Creates a fresh token and stores it in the session (also called at login, to rotate it). */
    static String nouveauJeton(HttpSession session) {
        byte[] octets = new byte[32];
        RANDOM.nextBytes(octets);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        session.setAttribute(JETON, jeton);
        return jeton;
    }

    private static boolean estModification(String methode) {
        return "POST".equals(methode) || "PUT".equals(methode)
                || "DELETE".equals(methode) || "PATCH".equals(methode);
    }

    /** Constant-time comparison. */
    static boolean egal(String attendu, String fourni) {
        if (attendu == null || fourni == null) {
            return false;
        }
        return MessageDigest.isEqual(attendu.getBytes(StandardCharsets.UTF_8),
                fourni.getBytes(StandardCharsets.UTF_8));
    }
}
