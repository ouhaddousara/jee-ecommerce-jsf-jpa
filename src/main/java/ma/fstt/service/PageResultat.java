package ma.fstt.service;

import java.util.Collections;
import java.util.List;

/** One page of results plus what the view needs to draw the pagination. */
public class PageResultat<T> {

    private final List<T> elements;
    private final int page;
    private final int taille;
    private final long total;

    public PageResultat(List<T> elements, int page, int taille, long total) {
        this.elements = Collections.unmodifiableList(elements);
        this.page = page;
        this.taille = taille;
        this.total = total;
    }

    public List<T> getElements() {
        return elements;
    }

    /** 1-based number of this page. */
    public int getPage() {
        return page;
    }

    public int getTaille() {
        return taille;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        return (int) Math.max(1, (total + taille - 1) / taille);
    }

    public boolean isPrecedente() {
        return page > 1;
    }

    public boolean isSuivante() {
        return page < getTotalPages();
    }

    /** Rank of the first element shown (1-based), 0 when the page is empty. */
    public long getDebut() {
        return elements.isEmpty() ? 0 : (long) (page - 1) * taille + 1;
    }

    public long getFin() {
        return elements.isEmpty() ? 0 : getDebut() + elements.size() - 1;
    }
}
