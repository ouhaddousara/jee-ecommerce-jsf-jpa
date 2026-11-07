package ma.fstt.jpa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.TypedQuery;

import ma.fstt.entities.Categorie;
import ma.fstt.entities.Produit;
import ma.fstt.entities.Vitrine;
import ma.fstt.exception.MetierException;
import ma.fstt.exception.ValidationException;
import ma.fstt.service.CategorieForm;
import ma.fstt.service.CategorieVue;
import ma.fstt.service.GestionCatalogueService;
import ma.fstt.service.ProduitForm;
import ma.fstt.service.Validation;

@ApplicationScoped
public class JpaGestionCatalogueService implements GestionCatalogueService {

    private static final String CONFLIT = "Cet élément a été modifié par quelqu'un d'autre "
            + "pendant que tu le modifiais. Recharge la page puis recommence.";

    @Inject
    private TransactionRunner tx;

    // ------------------------------------------------------------------
    // Products
    // ------------------------------------------------------------------

    @Override
    public List<Produit> tousLesProduits() {
        return tx.read(em -> {
            List<Produit> produits = em.createQuery(
                    "SELECT p FROM Produit p ORDER BY p.reference", Produit.class).getResultList();
            for (Produit p : produits) {
                p.getCategorie().getNom();
            }
            return produits;
        });
    }

    @Override
    public Produit produit(long id) {
        return tx.read(em -> {
            Produit produit = em.find(Produit.class, id);
            if (produit != null) {
                produit.getCategorie().getNom();
            }
            return produit;
        });
    }

    @Override
    public Produit enregistrerProduit(ProduitForm form) {
        Validation v = new Validation();
        String reference = v.reference("reference", form.getReference());
        String nom = v.texte("nom", form.getNom(), "Le nom", 2, 150, true);
        String description = v.texte("description", form.getDescription(), "La description", 0, 2000, false);
        BigDecimal prix = v.decimal("prix", form.getPrix(), "Le prix",
                BigDecimal.ZERO, new BigDecimal("9999999.99"));
        Integer stock = v.entier("stock", form.getStock(), "Le stock", 0, 1_000_000);
        String imageUrl = v.urlHttps("imageUrl", form.getImageUrl());
        Long categorieId = v.identifiant("categorieId", form.getCategorieId(), "La catégorie");
        v.verifier();

        return tx.write(em -> {
            Categorie categorie = em.find(Categorie.class, categorieId);
            if (categorie == null) {
                throw erreur("categorieId", "Cette catégorie n'existe pas.");
            }

            // Friendly message for the common case; the UNIQUE constraint still guards the race.
            // The id condition is added only when editing: a null parameter in "? IS NULL" is not portable.
            TypedQuery<Long> doublonsQuery = em.createQuery(
                    "SELECT COUNT(p) FROM Produit p WHERE p.reference = :reference"
                            + (form.getId() != null ? " AND p.id <> :id" : ""), Long.class)
                    .setParameter("reference", reference);
            if (form.getId() != null) {
                doublonsQuery.setParameter("id", form.getId());
            }
            Long doublons = doublonsQuery.getSingleResult();
            if (doublons > 0) {
                throw erreur("reference", "Cette référence est déjà utilisée par un autre produit.");
            }

            Produit produit;
            if (form.getId() == null) {
                produit = new Produit(reference, nom, description, prix, stock, form.isActif());
                categorie.ajouterProduit(produit);
                em.persist(produit);
            } else {
                produit = em.find(Produit.class, form.getId());
                if (produit == null) {
                    throw new MetierException("Produit introuvable.");
                }
                if (produit.getVersion() != form.getVersion()) {
                    throw new MetierException(CONFLIT);
                }
                produit.setReference(reference);
                produit.setNom(nom);
                produit.setDescription(description);
                produit.setPrix(prix);
                produit.setStock(stock);
                produit.setActif(form.isActif());
                if (!produit.getCategorie().getId().equals(categorie.getId())) {
                    categorie.ajouterProduit(produit);
                }
            }
            produit.setImageUrl(imageUrl);
            produit.getCategorie().getNom();
            return produit;
        });
    }

    @Override
    public boolean basculerProduit(long id) {
        return tx.write(em -> {
            Produit produit = em.find(Produit.class, id);
            if (produit == null) {
                throw new MetierException("Produit introuvable.");
            }
            produit.setActif(!produit.isActif());
            return produit.isActif();
        });
    }

    @Override
    public void supprimerProduit(long id) {
        tx.write(em -> {
            Produit produit = em.find(Produit.class, id);
            if (produit == null) {
                throw new MetierException("Produit introuvable.");
            }
            Long paniers = em.createQuery(
                    "SELECT COUNT(l) FROM LignePanier l WHERE l.produit.id = :id", Long.class)
                    .setParameter("id", id)
                    .getSingleResult();
            if (paniers > 0) {
                throw new MetierException("Ce produit est présent dans " + paniers
                        + " panier(s) : retire-le de la vente plutôt que de le supprimer.");
            }
            em.remove(produit);
            return null;
        });
    }

    // ------------------------------------------------------------------
    // Categories
    // ------------------------------------------------------------------

    @Override
    public List<CategorieVue> categories() {
        return tx.read(em -> {
            List<Categorie> categories = em.createQuery(
                    "SELECT c FROM Categorie c ORDER BY c.nom", Categorie.class).getResultList();
            Map<Long, Long> comptes = Requetes.produitsParCategorie(em, false);
            List<CategorieVue> vues = new ArrayList<>();
            for (Categorie c : categories) {
                vues.add(new CategorieVue(c.getId(), c.getNom(), c.getDescription(),
                        comptes.getOrDefault(c.getId(), 0L)));
            }
            return vues;
        });
    }

    @Override
    public Categorie categorie(long id) {
        return tx.read(em -> em.find(Categorie.class, id));
    }

    @Override
    public Categorie enregistrerCategorie(CategorieForm form) {
        Validation v = new Validation();
        String nom = v.texte("nom", form.getNom(), "Le nom", 2, 100, true);
        String description = v.texte("description", form.getDescription(), "La description", 0, 255, false);
        v.verifier();

        return tx.write(em -> {
            Categorie categorie;
            Vitrine vitrine;
            if (form.getId() == null) {
                vitrine = Requetes.premier(em.createQuery(
                        "SELECT v FROM Vitrine v WHERE v.active = true ORDER BY v.id", Vitrine.class));
                if (vitrine == null) {
                    throw new MetierException("Aucune vitrine active : impossible d'ajouter une catégorie.");
                }
                categorie = new Categorie(nom, description);
            } else {
                categorie = em.find(Categorie.class, form.getId());
                if (categorie == null) {
                    throw new MetierException("Catégorie introuvable.");
                }
                vitrine = categorie.getVitrine();
            }

            TypedQuery<Long> doublonsQuery = em.createQuery(
                    "SELECT COUNT(c) FROM Categorie c WHERE c.nom = :nom AND c.vitrine.id = :vitrine"
                            + (form.getId() != null ? " AND c.id <> :id" : ""), Long.class)
                    .setParameter("nom", nom)
                    .setParameter("vitrine", vitrine.getId());
            if (form.getId() != null) {
                doublonsQuery.setParameter("id", form.getId());
            }
            Long doublons = doublonsQuery.getSingleResult();
            if (doublons > 0) {
                throw erreur("nom", "Une catégorie porte déjà ce nom.");
            }

            categorie.setNom(nom);
            categorie.setDescription(description);
            if (form.getId() == null) {
                vitrine.ajouterCategorie(categorie);
                em.persist(categorie);
            }
            return categorie;
        });
    }

    @Override
    public void supprimerCategorie(long id) {
        tx.write(em -> {
            Categorie categorie = em.find(Categorie.class, id);
            if (categorie == null) {
                throw new MetierException("Catégorie introuvable.");
            }
            Long produits = em.createQuery(
                    "SELECT COUNT(p) FROM Produit p WHERE p.categorie.id = :id", Long.class)
                    .setParameter("id", id)
                    .getSingleResult();
            if (produits > 0) {
                throw new MetierException("Cette catégorie contient encore " + produits
                        + " produit(s) : déplace-les ou supprime-les d'abord.");
            }
            em.remove(categorie);
            return null;
        });
    }

    private static ValidationException erreur(String champ, String message) {
        return new ValidationException(Map.of(champ, message));
    }
}
