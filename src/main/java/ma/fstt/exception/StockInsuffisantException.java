package ma.fstt.exception;

/**
 * Raised when the requested quantity exceeds the available stock.
 * Carries the available stock so the view can suggest a valid quantity.
 */
public class StockInsuffisantException extends MetierException {

    private static final long serialVersionUID = 1L;

    private final int stockDisponible;

    public StockInsuffisantException(String message, int stockDisponible) {
        super(message);
        this.stockDisponible = stockDisponible;
    }

    public int getStockDisponible() {
        return stockDisponible;
    }
}
