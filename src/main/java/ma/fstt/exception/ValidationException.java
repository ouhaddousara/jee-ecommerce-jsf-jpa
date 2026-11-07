package ma.fstt.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Form validation failure: one message per field, so the view can display
 * each message next to the field it belongs to.
 */
public class ValidationException extends MetierException {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> erreurs;

    public ValidationException(Map<String, String> erreurs) {
        super("Le formulaire contient des erreurs.");
        this.erreurs = Collections.unmodifiableMap(new LinkedHashMap<>(erreurs));
    }

    /** Field name to message, in the order the errors were detected. */
    public Map<String, String> getErreurs() {
        return erreurs;
    }
}
