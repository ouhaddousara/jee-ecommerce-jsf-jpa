package ma.fstt.web;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ma.fstt.entities.Internaute;
import ma.fstt.exception.MetierException;
import ma.fstt.exception.ValidationException;
import ma.fstt.service.InscriptionForm;
import ma.fstt.service.InternauteService;

/** Registration page (inscription.xhtml). The service validates; the bean shows its field errors. */
@Named
@ViewScoped
public class InscriptionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private InternauteService internautes;

    @Inject
    private UserSession userSession;

    private InscriptionForm form = new InscriptionForm();

    /** Field name to message, filled when the service refuses the form. */
    private LinkedHashMap<String, String> erreurs = new LinkedHashMap<>();

    /** View action: a signed-in user has nothing to do here. */
    public String init() {
        return userSession.isConnecte() ? "/vitrine.xhtml?faces-redirect=true" : null;
    }

    public String inscrire() {
        try {
            Internaute client = internautes.inscrire(form);
            String cible = userSession.ouvrir(client);
            Messages.succes("Compte créé : bienvenue " + client.getPrenom() + " !");
            Messages.apresRedirection();
            return cible;
        } catch (ValidationException e) {
            erreurs = new LinkedHashMap<>(e.getErreurs());
            effacerMotsDePasse();
            return null;
        } catch (MetierException e) {
            Messages.erreur(e.getMessage());
            effacerMotsDePasse();
            return null;
        }
    }

    /** The passwords are deliberately NOT sent back to the page. */
    private void effacerMotsDePasse() {
        form.setMotDePasse(null);
        form.setConfirmation(null);
    }

    public InscriptionForm getForm() {
        return form;
    }

    public Map<String, String> getErreurs() {
        return erreurs;
    }

    public boolean isAvecErreurs() {
        return !erreurs.isEmpty();
    }
}
