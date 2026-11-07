package ma.fstt.jpa;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManagerFactory;

import ma.fstt.persistence.PersistenceConfig;

/**
 * Exposes the one EntityManagerFactory of the application to CDI. It is
 * created once, shared by every service (it is thread-safe, unlike an
 * EntityManager) and closed when the application stops.
 */
@ApplicationScoped
public class EntityManagerFactoryProducer {

    @Produces
    @ApplicationScoped
    public EntityManagerFactory creer() {
        return PersistenceConfig.createEntityManagerFactory();
    }

    public void fermer(@Disposes EntityManagerFactory emf) {
        if (emf.isOpen()) {
            emf.close();
        }
    }

    /**
     * Forces the creation at startup: a wrong password or an unreachable MySQL
     * shows up in the Tomcat log when the application starts, not at the
     * first visitor's request.
     */
    void demarrer(@Observes @Initialized(ApplicationScoped.class) Object evenement,
                  EntityManagerFactory emf) {
        emf.isOpen();
    }
}
