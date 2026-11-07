package ma.fstt.service;

/** Read-only summary of a category with its product count (sidebar and admin list). */
public class CategorieVue {

    private final Long id;
    private final String nom;
    private final String description;
    private final long nombreProduits;

    public CategorieVue(Long id, String nom, String description, long nombreProduits) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.nombreProduits = nombreProduits;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getDescription() {
        return description;
    }

    public long getNombreProduits() {
        return nombreProduits;
    }
}
