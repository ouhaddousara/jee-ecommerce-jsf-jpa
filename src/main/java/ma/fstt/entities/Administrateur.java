package ma.fstt.entities;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/** Back-office user allowed to manage the catalogue (categories and products). */
@Entity
@DiscriminatorValue("ADMIN")
public class Administrateur extends Internaute {

    /** UNIQUE, but nullable (the column is shared with the other subclasses). */
    @Column(name = "matricule", length = 30, unique = true)
    private String matricule;

    /** Required by JPA. */
    protected Administrateur() {
    }

    public Administrateur(String nom, String prenom, String email, String motDePasseHash,
                          String matricule) {
        super(nom, prenom, email, motDePasseHash);
        this.matricule = matricule;
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }
}
