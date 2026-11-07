# Diagrammes de classes : atelier 3 (JSF et JPA)

## 1. Modèle métier (inchangé)

```mermaid
classDiagram
    direction LR
    class Internaute {
        <<abstract>>
        -Long id
        -String nom
        -String prenom
        -String email
        -String motDePasseHash
        -LocalDateTime dateInscription
    }
    class Client {
        -String adresse
        -String telephone
    }
    class Administrateur {
        -String matricule
    }
    class Vitrine {
        -Long id
        -String titre
        -String description
        -boolean active
    }
    class Categorie {
        -Long id
        -String nom
        -String description
    }
    class Produit {
        -Long id
        -String reference
        -String nom
        -String description
        -BigDecimal prix
        -int stock
        -String imageUrl
        -boolean actif
        -long version
    }
    class Panier {
        -Long id
        -LocalDateTime dateCreation
        -LocalDateTime dateModification
        +ajouterProduit(Produit, int)
        +modifierQuantite(Produit, int)
        +retirerProduit(Produit)
        +vider()
        +getTotal() BigDecimal
    }
    class LignePanier {
        -Long id
        -int quantite
        -BigDecimal prixUnitaire
        +getSousTotal() BigDecimal
    }
    Internaute <|-- Client
    Internaute <|-- Administrateur
    Client "1" -- "1" Panier : possède
    Panier "1" *-- "0..*" LignePanier : contient
    LignePanier "0..*" --> "1" Produit : référence
    Vitrine "1" o-- "0..*" Categorie : présente
    Categorie "1" o-- "0..*" Produit : regroupe
    Administrateur ..> Produit : gère
```

## 2. Couche JSF (nouvelle) : un bean par gestion (étape 4)

```mermaid
classDiagram
    direction LR
    class UserSession {
        <<RequestScoped>>
        +isConnecte() boolean
        +isAdmin() boolean
        +ouvrir(Internaute) String
        +deconnecter() String
    }
    class ConnexionBean {
        <<ViewScoped>>
        -String email
        -String motDePasse
        +connecter() String
    }
    class InscriptionBean {
        <<ViewScoped>>
        +inscrire() String
    }
    class VitrineBean {
        <<ViewScoped>>
        -Long categorie
        -String q
        -int page
        +init() String
    }
    class PanierBean {
        <<ViewScoped>>
        +ajouter(long, String, String) String
        +modifier(long) String
        +retirer(long) String
        +vider() String
    }
    class ProduitAdminBean {
        <<ViewScoped>>
        +enregistrer() String
        +basculer(long) String
        +supprimer(long) String
    }
    class CategorieAdminBean {
        <<ViewScoped>>
        +enregistrer() String
        +supprimer(long) String
    }
    class InternauteService {
        <<interface>>
    }
    class CatalogueService {
        <<interface>>
    }
    class PanierService {
        <<interface>>
    }
    class GestionCatalogueService {
        <<interface>>
    }
    ConnexionBean ..> InternauteService
    InscriptionBean ..> InternauteService
    VitrineBean ..> CatalogueService
    PanierBean ..> PanierService
    ProduitAdminBean ..> GestionCatalogueService
    CategorieAdminBean ..> GestionCatalogueService
    ConnexionBean ..> UserSession
    PanierBean ..> UserSession
```

Correspondance avec l'atelier 2 :

| Atelier 2 (servlet) | Atelier 3 (bean) | Page Facelets |
|---|---|---|
| `ClientServlet` | `ConnexionBean`, `InscriptionBean` | `connexion.xhtml`, `inscription.xhtml` |
| `VitrineServlet` | `VitrineBean` | `vitrine.xhtml`, `detail.xhtml` |
| `PanierServlet` | `PanierBean` | `panier.xhtml` |
| `ProduitServlet` | `ProduitAdminBean` | `admin/produits.xhtml`, `admin/produit-form.xhtml` |
| `CategorieServlet` | `CategorieAdminBean` | `admin/categories.xhtml`, `admin/categorie-form.xhtml` |
