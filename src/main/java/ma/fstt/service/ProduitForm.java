package ma.fstt.service;

import ma.fstt.entities.Produit;

/**
 * Raw (string) values of the product form. {@code version} is the optimistic
 * lock value the form was loaded with: if the product changed meanwhile, the
 * save is refused instead of silently overwriting the other edit.
 */
public class ProduitForm implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private long version;
    private String reference;
    private String nom;
    private String description;
    private String prix;
    private String stock;
    private String imageUrl;
    private String categorieId;
    private boolean actif;

    /** Pre-fills the form from an existing product (edit screen). */
    public static ProduitForm depuis(Produit produit) {
        ProduitForm form = new ProduitForm();
        form.id = produit.getId();
        form.version = produit.getVersion();
        form.reference = produit.getReference();
        form.nom = produit.getNom();
        form.description = produit.getDescription();
        form.prix = produit.getPrix().toPlainString();
        form.stock = String.valueOf(produit.getStock());
        form.imageUrl = produit.getImageUrl();
        form.categorieId = String.valueOf(produit.getCategorie().getId());
        form.actif = produit.isActif();
        return form;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
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

    public String getPrix() {
        return prix;
    }

    public void setPrix(String prix) {
        this.prix = prix;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(String categorieId) {
        this.categorieId = categorieId;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
