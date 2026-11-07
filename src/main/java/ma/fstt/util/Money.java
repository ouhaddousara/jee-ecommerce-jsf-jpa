package ma.fstt.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formats amounts the way the JSP pages do (French locale, 2 decimals). */
public final class Money {

    private Money() {
    }

    public static String format(BigDecimal montant) {
        // NumberFormat is not thread-safe: create one per call (cheap enough here).
        NumberFormat format = NumberFormat.getNumberInstance(Locale.FRANCE);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(montant);
    }
}
