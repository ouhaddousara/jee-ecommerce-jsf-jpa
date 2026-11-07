package ma.fstt.exception;

/**
 * Business-rule violation (invalid quantity, product unavailable, ...).
 * Unchecked on purpose: servlets catch it once and show the message to the
 * user. The message is written in French because it is displayed as-is.
 */
public class MetierException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MetierException(String message) {
        super(message);
    }
}
