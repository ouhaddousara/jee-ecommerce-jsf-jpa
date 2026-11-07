package ma.fstt.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.TypedQuery;

import ma.fstt.entities.Categorie;
import ma.fstt.entities.Produit;
import ma.fstt.entities.Vitrine;
import ma.fstt.service.CatalogueService;
import ma.fstt.service.CategorieVue;
import ma.fstt.service.PageResultat;

@ApplicationScoped
public class JpaCatalogueService implements CatalogueService {

    static final int TAILLE_PAGE = 9;
    private static final int LONGUEUR_MAX_RECHERCHE = 60;

    @Inject
    private TransactionRunner tx;

    @Override
    public Vitrine vitrineActive() {
        return tx.read(em -> Requetes.premier(em.createQuery(
                "SELECT v FROM Vitrine v WHERE v.active = true ORDER BY v.id", Vitrine.class)));
    }

    @Override
    public List<CategorieVue> categories() {
        return tx.read(em -> {
            List<Categorie> categories = em.createQuery(
                    "SELECT c FROM Categorie c WHERE c.vitrine.active = true ORDER BY c.nom",
                    Categorie.class).getResultList();
            Map<Long, Long> comptes = Requetes.produitsParCategorie(em, true);
            List<CategorieVue> vues = new ArrayList<>();
            for (Categorie c : categories) {
                vues.add(new CategorieVue(c.getId(), c.getNom(), c.getDescription(),
                        comptes.getOrDefault(c.getId(), 0L)));
            }
            return vues;
        });
    }

    @Override
    public PageResultat<Produit> catalogue(Long categorieId, String recherche, int page) {
        String terme = nettoyer(recherche);
        return tx.read(em -> {
            StringBuilder filtre = new StringBuilder(
                    " WHERE p.actif = true AND p.categorie.vitrine.active = true");
            if (categorieId != null) {
                filtre.append(" AND p.categorie.id = :categorie");
            }
            if (terme != null) {
                // '!' is the escape character: a backslash would break the SQL string literal on MySQL.
                filtre.append(" AND (LOWER(p.nom) LIKE :terme ESCAPE '!'"
                        + " OR LOWER(p.reference) LIKE :terme ESCAPE '!'"
                        + " OR LOWER(p.description) LIKE :terme ESCAPE '!')");
            }

            TypedQuery<Long> comptage = em.createQuery(
                    "SELECT COUNT(p) FROM Produit p" + filtre, Long.class);
            lier(comptage, categorieId, terme);
            long total = Requetes.compte(comptage);

            int pages = (int) Math.max(1, (total + TAILLE_PAGE - 1) / TAILLE_PAGE);
            int courante = Math.max(1, Math.min(page, pages));

            TypedQuery<Produit> requete = em.createQuery(
                    "SELECT p FROM Produit p" + filtre + " ORDER BY p.nom, p.id", Produit.class);
            lier(requete, categorieId, terme);
            requete.setFirstResult((courante - 1) * TAILLE_PAGE).setMaxResults(TAILLE_PAGE);

            List<Produit> produits = requete.getResultList();
            for (Produit p : produits) {
                p.getCategorie().getNom(); // load while the EntityManager is still open
            }
            return new PageResultat<>(produits, courante, TAILLE_PAGE, total);
        });
    }

    @Override
    public Produit produitPublic(long id) {
        return tx.read(em -> {
            Produit produit = em.find(Produit.class, id);
            if (produit == null || !produit.isActif()
                    || !produit.getCategorie().getVitrine().isActive()) {
                return null;
            }
            produit.getCategorie().getNom();
            return produit;
        });
    }

    private static <T> void lier(TypedQuery<T> requete, Long categorieId, String terme) {
        if (categorieId != null) {
            requete.setParameter("categorie", categorieId);
        }
        if (terme != null) {
            requete.setParameter("terme", "%" + echapper(terme.toLowerCase(Locale.ROOT)) + "%");
        }
    }

    /** Neutralises the LIKE wildcards typed by the user. */
    static String echapper(String texte) {
        return texte.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private static String nettoyer(String recherche) {
        if (recherche == null) {
            return null;
        }
        String v = recherche.trim();
        if (v.isEmpty()) {
            return null;
        }
        return v.length() > LONGUEUR_MAX_RECHERCHE ? v.substring(0, LONGUEUR_MAX_RECHERCHE) : v;
    }
}
