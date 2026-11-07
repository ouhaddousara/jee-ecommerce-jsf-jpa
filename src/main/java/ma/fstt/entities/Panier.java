package ma.fstt.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.eclipse.persistence.annotations.CascadeOnDelete;

import ma.fstt.exception.MetierException;
import ma.fstt.exception.StockInsuffisantException;

/**
 * Shopping cart. All cart rules live here, not in the servlets, so no code
 * path can bypass them:
 * <ul>
 *   <li>quantity between 1 and {@value #MAX_QUANTITE_PAR_LIGNE} per product;</li>
 *   <li>only active products can be added;</li>
 *   <li>the quantity of a line never exceeds the available stock;</li>
 *   <li>a product appears at most once (adding it again increases the line);</li>
 *   <li>the unit price is frozen in the line when the product is first added.</li>
 * </ul>
 * Stock is checked, not decremented: decrementing belongs to an order, which
 * is outside the scope of this version.
 */
@Entity
@Table(name = "panier")
public class Panier {

    public static final int MAX_QUANTITE_PAR_LIGNE = 99;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;

    /**
     * Composition: lines live and die with their cart. orphanRemoval deletes a
     * line removed from the list; CascadeOnDelete asks the database to do
     * ON DELETE CASCADE when the cart itself is deleted.
     */
    @OneToMany(mappedBy = "panier", cascade = CascadeType.ALL, orphanRemoval = true)
    @CascadeOnDelete
    @OrderBy("id ASC")
    private List<LignePanier> lignes = new ArrayList<>();

    public Panier() {
        LocalDateTime maintenant = LocalDateTime.now();
        this.dateCreation = maintenant;
        this.dateModification = maintenant;
    }

    @PrePersist
    void avantInsertion() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (dateModification == null) {
            dateModification = dateCreation;
        }
    }

    // ------------------------------------------------------------------
    // Business operations
    // ------------------------------------------------------------------

    /** Adds {@code quantite} units of a product; increases the line if it already exists. */
    public LignePanier ajouterProduit(Produit produit, int quantite) {
        exigerProduit(produit);
        exigerQuantiteValide(quantite);
        if (!produit.isActif()) {
            throw new MetierException("Le produit « " + produit.getNom()
                    + " » n'est plus disponible à la vente.");
        }
        LignePanier ligne = trouverLigne(produit);
        int quantiteCible = (ligne == null ? 0 : ligne.getQuantite()) + quantite;
        verifierQuantiteCible(produit, quantiteCible);

        if (ligne == null) {
            // The price is frozen here; later price changes do not alter the cart line.
            ligne = new LignePanier(this, produit, quantite, produit.getPrix());
            lignes.add(ligne);
        } else {
            ligne.setQuantite(quantiteCible);
        }
        toucher();
        return ligne;
    }

    /** Sets the exact quantity of an existing line (use {@link #retirerProduit} to remove it). */
    public LignePanier modifierQuantite(Produit produit, int quantite) {
        exigerProduit(produit);
        exigerQuantiteValide(quantite);
        LignePanier ligne = exigerLigne(produit);
        verifierQuantiteCible(produit, quantite);
        ligne.setQuantite(quantite);
        toucher();
        return ligne;
    }

    public void retirerProduit(Produit produit) {
        exigerProduit(produit);
        lignes.remove(exigerLigne(produit));
        toucher();
    }

    public void vider() {
        lignes.clear();
        toucher();
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public BigDecimal getTotal() {
        BigDecimal total = new BigDecimal("0.00");
        for (LignePanier ligne : lignes) {
            total = total.add(ligne.getSousTotal());
        }
        return total;
    }

    /** Total number of units (sum of the line quantities), shown in the cart badge. */
    public int getNombreArticles() {
        int total = 0;
        for (LignePanier ligne : lignes) {
            total += ligne.getQuantite();
        }
        return total;
    }

    public boolean isVide() {
        return lignes.isEmpty();
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    /** Read-only view: mutate through the business operations above. */
    public List<LignePanier> getLignes() {
        return Collections.unmodifiableList(lignes);
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private LignePanier trouverLigne(Produit produit) {
        for (LignePanier ligne : lignes) {
            if (ligne.getProduit().equals(produit)) {
                return ligne;
            }
        }
        return null;
    }

    private LignePanier exigerLigne(Produit produit) {
        LignePanier ligne = trouverLigne(produit);
        if (ligne == null) {
            throw new MetierException("Ce produit n'est pas dans le panier.");
        }
        return ligne;
    }

    private static void exigerProduit(Produit produit) {
        if (produit == null) {
            throw new MetierException("Produit introuvable.");
        }
    }

    private static void exigerQuantiteValide(int quantite) {
        if (quantite < 1) {
            throw new MetierException("La quantité doit être d'au moins 1.");
        }
        if (quantite > MAX_QUANTITE_PAR_LIGNE) {
            throw new MetierException("La quantité maximale par article est de "
                    + MAX_QUANTITE_PAR_LIGNE + ".");
        }
    }

    /** The target quantity is the final quantity of the line, not the increment. */
    private static void verifierQuantiteCible(Produit produit, int quantiteCible) {
        if (quantiteCible > MAX_QUANTITE_PAR_LIGNE) {
            throw new MetierException("La quantité maximale par article est de "
                    + MAX_QUANTITE_PAR_LIGNE + ".");
        }
        if (quantiteCible > produit.getStock()) {
            throw new StockInsuffisantException("Stock insuffisant pour « " + produit.getNom()
                    + " » : " + produit.getStock() + " disponible(s).", produit.getStock());
        }
    }

    private void toucher() {
        this.dateModification = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Panier[id=" + id + ", lignes=" + lignes.size() + "]";
    }
}
