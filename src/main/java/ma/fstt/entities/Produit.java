package ma.fstt.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import ma.fstt.exception.MetierException;

/** A sellable item of the catalogue. */
@Entity
@Table(name = "produit",
       uniqueConstraints = @UniqueConstraint(name = "uk_produit_reference", columnNames = "reference"))
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "reference", nullable = false, length = 40)
    private String reference;

    @Column(name = "nom", nullable = false, length = 150)
    private String nom;

    @Column(name = "description", length = 2000)
    private String description;

    /** Money is always BigDecimal, scale 2 (never double). */
    @Column(name = "prix", nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    /** false = withdrawn from sale (kept in the database because carts may reference it). */
    @Column(name = "actif", nullable = false)
    private boolean actif;

    /** Optimistic lock: two concurrent edits of the same product cannot overwrite each other silently. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    /** Required by JPA. */
    protected Produit() {
    }

    public Produit(String reference, String nom, String description,
                   BigDecimal prix, int stock, boolean actif) {
        this.reference = reference;
        this.nom = nom;
        this.description = description;
        setPrix(prix);
        setStock(stock);
        this.actif = actif;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        if (prix == null || prix.signum() < 0) {
            throw new MetierException("Le prix doit être positif ou nul.");
        }
        this.prix = prix.setScale(2, RoundingMode.HALF_UP);
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        if (stock < 0) {
            throw new MetierException("Le stock ne peut pas être négatif.");
        }
        this.stock = stock;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public long getVersion() {
        return version;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    /** Package-private: the relation is managed through Categorie.ajouterProduit. */
    void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    /** Identity is the primary key; two transient products are only equal if they are the same object. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Produit)) {
            return false;
        }
        Produit autre = (Produit) o;
        return id != null && id.equals(autre.id);
    }

    /** Constant on purpose: the id is null until the first flush, and the hash must not change meanwhile. */
    @Override
    public int hashCode() {
        return Produit.class.hashCode();
    }

    @Override
    public String toString() {
        return "Produit[id=" + id + ", reference=" + reference + "]";
    }
}
