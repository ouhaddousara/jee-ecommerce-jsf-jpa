package ma.fstt.service;

import ma.fstt.entities.Client;
import ma.fstt.entities.Internaute;

/** Accounts: registration and authentication. */
public interface InternauteService {

    /**
     * Creates a client account (and its empty cart).
     *
     * @throws ma.fstt.exception.ValidationException invalid fields or e-mail already used
     */
    Client inscrire(InscriptionForm form);

    /**
     * @return the account when the credentials are correct
     * @throws ma.fstt.exception.MetierException with one generic message whether the
     *         e-mail is unknown or the password wrong (no account enumeration)
     */
    Internaute authentifier(String email, String motDePasse);

    /** @return the account, or null when it does not exist */
    Internaute trouver(long id);
}
