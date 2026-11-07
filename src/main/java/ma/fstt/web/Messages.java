package ma.fstt.web;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 * Global messages shown at the top of the page by the layout. They replace the
 * hand-made "Flash" of workshop 2: JSF keeps them across a redirect when
 * {@link #apresRedirection()} is called just before returning a redirect outcome.
 *
 * Severity mapping used by the layout: INFO = green "success", WARN = neutral
 * "info", ERROR = red "error".
 */
public final class Messages {

    private Messages() {
    }

    public static void succes(String texte) {
        ajouter(FacesMessage.SEVERITY_INFO, texte);
    }

    public static void info(String texte) {
        ajouter(FacesMessage.SEVERITY_WARN, texte);
    }

    public static void erreur(String texte) {
        ajouter(FacesMessage.SEVERITY_ERROR, texte);
    }

    /** Call right before returning an outcome with faces-redirect=true. */
    public static void apresRedirection() {
        FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
    }

    private static void ajouter(FacesMessage.Severity severite, String texte) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severite, texte, null));
    }
}
