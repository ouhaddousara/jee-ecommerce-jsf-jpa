package ma.fstt.jpa;

import java.util.Locale;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import ma.fstt.entities.Client;
import ma.fstt.entities.Internaute;
import ma.fstt.exception.MetierException;
import ma.fstt.exception.ValidationException;
import ma.fstt.service.InscriptionForm;
import ma.fstt.service.InternauteService;
import ma.fstt.service.Validation;
import ma.fstt.util.PasswordHasher;

@ApplicationScoped
public class JpaInternauteService implements InternauteService {

    private static final String IDENTIFIANTS_INCORRECTS = "E-mail ou mot de passe incorrect.";

    /**
     * Verified when the e-mail is unknown, so a wrong e-mail costs as much
     * time as a wrong password: response time does not reveal which accounts exist.
     */
    private static final String HASH_FACTICE = PasswordHasher.hash("mot-de-passe-factice-1");

    @Inject
    private TransactionRunner tx;

    @Override
    public Client inscrire(InscriptionForm form) {
        Validation v = new Validation();
        String nom = v.nomPropre("nom", form.getNom(), "Le nom");
        String prenom = v.nomPropre("prenom", form.getPrenom(), "Le prénom");
        String email = v.email("email", form.getEmail());
        String motDePasse = v.motDePasse("motDePasse", form.getMotDePasse(), form.getConfirmation());
        String adresse = v.texte("adresse", form.getAdresse(), "L'adresse", 5, 255, false);
        String telephone = v.telephone("telephone", form.getTelephone());
        v.verifier();

        // Hashing is slow on purpose: do it before opening the transaction.
        String hash = PasswordHasher.hash(motDePasse);

        return tx.write(em -> {
            Long existants = em.createQuery(
                    "SELECT COUNT(i) FROM Internaute i WHERE i.email = :email", Long.class)
                    .setParameter("email", email)
                    .getSingleResult();
            if (existants > 0) {
                throw new ValidationException(Map.of("email",
                        "Un compte existe déjà avec cette adresse e-mail."));
            }
            // The cart is created by the Client constructor and saved by cascade.
            Client client = new Client(nom, prenom, email, hash, adresse, telephone);
            em.persist(client);
            return client;
        });
    }

    @Override
    public Internaute authentifier(String email, String motDePasse) {
        if (email == null || email.isBlank() || motDePasse == null || motDePasse.isEmpty()) {
            throw new MetierException(IDENTIFIANTS_INCORRECTS);
        }
        String cle = email.trim().toLowerCase(Locale.ROOT);
        Internaute trouve = tx.read(em -> Requetes.premier(em.createQuery(
                "SELECT i FROM Internaute i WHERE i.email = :email", Internaute.class)
                .setParameter("email", cle)));

        String hash = trouve != null ? trouve.getMotDePasseHash() : HASH_FACTICE;
        boolean correct = PasswordHasher.verify(motDePasse, hash);
        if (trouve == null || !correct) {
            throw new MetierException(IDENTIFIANTS_INCORRECTS);
        }
        return trouve;
    }

    @Override
    public Internaute trouver(long id) {
        return tx.read(em -> em.find(Internaute.class, id));
    }
}
