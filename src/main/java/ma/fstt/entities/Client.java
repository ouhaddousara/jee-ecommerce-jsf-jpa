package ma.fstt.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * A shopper. Owns exactly one cart, created together with the client: the
 * "one cart per client" rule is guaranteed by construction and by the UNIQUE
 * constraint on panier_id.
 */
@Entity
@DiscriminatorValue("CLIENT")
public class Client extends Internaute {

    @Column(name = "adresse", length = 255)
    private String adresse;

    @Column(name = "telephone", length = 30)
    private String telephone;

    /** Owning side of the 1-1 relation: the foreign key lives in internaute.panier_id. */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "panier_id", unique = true)
    private Panier panier;

    /** Required by JPA. */
    protected Client() {
    }

    public Client(String nom, String prenom, String email, String motDePasseHash,
                  String adresse, String telephone) {
        super(nom, prenom, email, motDePasseHash);
        this.adresse = adresse;
        this.telephone = telephone;
        this.panier = new Panier();
    }

    @Override
    public String getRole() {
        return "CLIENT";
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Panier getPanier() {
        return panier;
    }
}
