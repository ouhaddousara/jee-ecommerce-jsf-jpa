package ma.fstt.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** Small, pure helpers for paths and URLs (no JSF or servlet type: easy to test). */
public final class Chemins {

    private Chemins() {
    }

    /**
     * Only a path inside this application is accepted as a redirect target
     * (starts with a single "/", no scheme, no backslash, no line break):
     * this closes the open-redirect hole of a remembered "retour" path.
     */
    public static String cheminSur(String chemin, String parDefaut) {
        if (chemin == null || !chemin.startsWith("/") || chemin.startsWith("//")
                || chemin.indexOf('\\') >= 0 || chemin.indexOf('\r') >= 0 || chemin.indexOf('\n') >= 0) {
            return parDefaut;
        }
        return chemin;
    }

    /** Appends a query parameter to a path that may already have a query string. */
    public static String avecParametre(String chemin, String nom, String valeur) {
        return chemin + (chemin.indexOf('?') >= 0 ? "&" : "?") + nom + "=" + encoder(valeur);
    }

    /** URL-encodes a value as UTF-8. */
    public static String encoder(String valeur) {
        try {
            return URLEncoder.encode(valeur == null ? "" : valeur, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e); // UTF-8 is always available
        }
    }
}
