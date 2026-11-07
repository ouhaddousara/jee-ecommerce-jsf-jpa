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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/** A product category, displayed by exactly one storefront. */
@Entity
@Table(name = "categorie",
       uniqueConstraints = @UniqueConstraint(name = "uk_categorie_nom_vitrine",
                                             columnNames = { "nom", "vitrine_id" }))
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "description", length = 255)
    private String description;

    /** Owning side: foreign key categorie.vitrine_id. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "vitrine_id", nullable = false)
    private Vitrine vitrine;

    @OneToMany(mappedBy = "categorie", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @OrderBy("nom ASC")
    private List<Produit> produits = new ArrayList<>();

    /** Required by JPA. */
    protected Categorie() {
    }

    public Categorie(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }

    /** Keeps both sides of the relation consistent; returns the product for chaining. */
    public Produit ajouterProduit(Produit produit) {
        produit.setCategorie(this);
        produits.add(produit);
        return produit;
    }

    public Long getId() {
        return id;
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

    public Vitrine getVitrine() {
        return vitrine;
    }

    /** Package-private: the relation is managed through Vitrine.ajouterCategorie. */
    void setVitrine(Vitrine vitrine) {
        this.vitrine = vitrine;
    }

    /** Read-only view: mutate through {@link #ajouterProduit(Produit)}. */
    public List<Produit> getProduits() {
        return Collections.unmodifiableList(produits);
    }

    @Override
    public String toString() {
        return "Categorie[id=" + id + ", nom=" + nom + "]";
    }
}
