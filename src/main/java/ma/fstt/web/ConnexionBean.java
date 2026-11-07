package ma.fstt.web;

import java.io.Serializable;
import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import ma.fstt.entities.Internaute;
import ma.fstt.exception.MetierException;
import ma.fstt.service.InternauteService;

/** Sign-in page (connexion.xhtml). */
@Named
@ViewScoped
public class ConnexionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private InternauteService internautes;

    @Inject
    private UserSession userSession;

    @Inject
    private LoginGuard guard;

    private String email;
    private String motDePasse;

    /** Set by AuthFilter (?requis=1) when an anonymous visitor hit a protected page. */
    private String requis;

    /** View action: already signed in, go to the storefront; redirected from a protected page, say why. */
    public String init() {
        if (userSession.isConnecte()) {
            return "/vitrine.xhtml?faces-redirect=true";
        }
        if ("1".equals(requis)) {
            Messages.info("Connecte-toi pour continuer.");
        }
        return null;
    }

    public String connecter() {
        String adresseEmail = email == null ? "" : email.trim();
        HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance()
                .getExternalContext().getRequest();
        String cle = req.getRemoteAddr() + "|" + adresseEmail.toLowerCase(Locale.ROOT);

        long attente = guard.secondesRestantes(cle);
        if (attente > 0) {
            Messages.erreur("Trop de tentatives. Réessaie dans " + ((attente + 59) / 60) + " minute(s).");
            motDePasse = null;
            return null;
        }
        try {
            Internaute internaute = internautes.authentifier(adresseEmail, motDePasse == null ? "" : motDePasse);
            guard.reussite(cle);
            String cible = userSession.ouvrir(internaute);
            Messages.succes("Bienvenue " + internaute.getPrenom() + " !");
            Messages.apresRedirection();
            return cible;
        } catch (MetierException e) {
            guard.echec(cle);
            Messages.erreur(e.getMessage());
            motDePasse = null; // the password is never sent back to the page
            return null;
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getRequis() {
        return requis;
    }

    public void setRequis(String requis) {
        this.requis = requis;
    }
}
