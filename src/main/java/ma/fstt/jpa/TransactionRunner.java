package ma.fstt.jpa;

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

/**
 * The single place where transactions begin, commit and roll back (same role
 * as Jdbc.write/read in workshop 1). Services only describe WHAT to do with
 * an EntityManager; they never manage a transaction themselves, so none can
 * forget a rollback or leak an EntityManager.
 *
 * One EntityManager per call (never shared between threads), closed in all cases.
 */
@ApplicationScoped
public class TransactionRunner {

    @Inject
    private EntityManagerFactory emf;

    /** Read-only work: no transaction. Entities are detached when the call returns. */
    public <T> T read(Function<EntityManager, T> work) {
        EntityManager em = emf.createEntityManager();
        try {
            return work.apply(em);
        } finally {
            em.close();
        }
    }

    /** Read-write work: commit when it returns, rollback when it throws. */
    public <T> T write(Function<EntityManager, T> work) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw SqlErrors.traduire(e);
        } finally {
            em.close();
        }
    }
}
