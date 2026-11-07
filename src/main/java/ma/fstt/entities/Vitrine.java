package ma.fstt.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

/**
 * The storefront: the page that presents the categories (and, through them,
 * the products) to visitors.
 */
@Entity
@Table(name = "vitrine")
public class Vitrine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "titre", nullable = false, length = 120)
    private String titre;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    /**
     * Inverse side. PERSIST/MERGE only: removing a storefront must never
     * silently delete its categories.
     */
    @OneToMany(mappedBy = "vitrine", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @OrderBy("nom ASC")
    private List<Categorie> categories = new ArrayList<>();

    /** Required by JPA. */
    protected Vitrine() {
    }

    public Vitrine(String titre, String description, boolean active) {
        this.titre = titre;
        this.description = description;
        this.active = active;
    }

    /** Keeps both sides of the relation consistent; returns the category for chaining. */
    public Categorie ajouterCategorie(Categorie categorie) {
        categorie.setVitrine(this);
        categories.add(categorie);
        return categorie;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Read-only view: mutate through {@link #ajouterCategorie(Categorie)}. */
    public List<Categorie> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    @Override
    public String toString() {
        return "Vitrine[id=" + id + ", titre=" + titre + "]";
    }
}
