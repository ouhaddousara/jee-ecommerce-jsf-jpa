package ma.fstt.entities;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * One product in a cart. Created and modified only by {@link Panier}, which
 * enforces the business rules; hence the package-private constructor and
 * mutator. The (panier, produit) pair is UNIQUE in the database as well.
 */
@Entity
@Table(name = "ligne_panier",
       uniqueConstraints = @UniqueConstraint(name = "uk_ligne_panier_produit",
                                             columnNames = { "panier_id", "produit_id" }))
public class LignePanier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "panier_id", nullable = false)
    private Panier panier;

    /** No cascade: deleting a line must never delete a catalogue product. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "quantite", nullable = false)
    private int quantite;

    /** Price frozen when the product was first added to the cart. */
    @Column(name = "prix_unitaire", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    /** Required by JPA. */
    protected LignePanier() {
    }

    LignePanier(Panier panier, Produit produit, int quantite, BigDecimal prixUnitaire) {
        this.panier = panier;
        this.produit = produit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal getSousTotal() {
        return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }

    public Long getId() {
        return id;
    }

    public Panier getPanier() {
        return panier;
    }

    public Produit getProduit() {
        return produit;
    }

    public int getQuantite() {
        return quantite;
    }

    void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    @Override
    public String toString() {
        return "LignePanier[id=" + id + ", quantite=" + quantite + "]";
    }
}
