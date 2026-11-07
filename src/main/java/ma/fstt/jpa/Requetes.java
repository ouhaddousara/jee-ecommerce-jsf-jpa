package ma.fstt.jpa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

/** Small JPQL helpers shared by the service implementations. */
final class Requetes {

    private Requetes() {
    }

    /** First result or null (never throws NoResultException). */
    static <T> T premier(TypedQuery<T> requete) {
        List<T> resultats = requete.setMaxResults(1).getResultList();
        return resultats.isEmpty() ? null : resultats.get(0);
    }

    static long compte(TypedQuery<Long> requete) {
        return requete.getSingleResult();
    }

    /** Number of products per category id, optionally counting only the active ones. */
    static Map<Long, Long> produitsParCategorie(EntityManager em, boolean seulementActifs) {
        String jpql = "SELECT p.categorie.id, COUNT(p) FROM Produit p"
                + (seulementActifs ? " WHERE p.actif = true" : "")
                + " GROUP BY p.categorie.id";
        Map<Long, Long> comptes = new HashMap<>();
        for (Object[] ligne : em.createQuery(jpql, Object[].class).getResultList()) {
            comptes.put(((Number) ligne[0]).longValue(), ((Number) ligne[1]).longValue());
        }
        return comptes;
    }
}
