package ma.fstt.entities;

import java.time.LocalDateTime;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import ma.fstt.exception.MetierException;

/**
 * Root of the user hierarchy (Client, Administrateur).
 *
 * SINGLE_TABLE: one "internaute" table and one discriminator column, so
 * authentication is a single lookup by e-mail whatever the role is. The
 * columns of the subclasses are therefore nullable at database level; the
 * services enforce them.
 */
@Entity
@Table(name = "internaute",
       uniqueConstraints = @UniqueConstraint(name = "uk_internaute_email", columnNames = "email"))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type_internaute", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Internaute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nom", nullable = false, length = 80)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 80)
    private String prenom;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /** Output of PasswordHasher.hash(...): never the clear-text password. */
    @Column(name = "mot_de_passe_hash", nullable = false, length = 255)
    private String motDePasseHash;

    @Column(name = "date_inscription", nullable = false, updatable = false)
    private LocalDateTime dateInscription;

    /** Required by JPA. */
    protected Internaute() {
    }

    protected Internaute(String nom, String prenom, String email, String motDePasseHash) {
        this.nom = nom;
        this.prenom = prenom;
        setEmail(email);
        this.motDePasseHash = motDePasseHash;
    }

    @PrePersist
    void avantInsertion() {
        if (dateInscription == null) {
            dateInscription = LocalDateTime.now();
        }
    }

    /** Role name used by the web layer ("CLIENT" or "ADMIN"). */
    public abstract String getRole();

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    public String getEmail() {
        return email;
    }

    /** E-mails are stored trimmed and lower-cased so the unique key is case-insensitive. */
    public void setEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new MetierException("L'adresse e-mail est obligatoire.");
        }
        this.email = email.trim().toLowerCase(Locale.ROOT);
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id + ", email=" + email + "]";
    }
}
