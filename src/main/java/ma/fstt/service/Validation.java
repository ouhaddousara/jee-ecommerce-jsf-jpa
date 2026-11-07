package ma.fstt.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import ma.fstt.exception.ValidationException;

/**
 * Collects every error of a form instead of stopping at the first one, so the
 * user can fix the whole form in one pass. Each method returns the cleaned
 * value (trimmed, parsed) or null when the field is empty or invalid.
 */
public final class Validation {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern NOM_PROPRE = Pattern.compile("^\\p{L}[\\p{L} '.-]*$");
    private static final Pattern TELEPHONE = Pattern.compile("^[+0-9][0-9 ().-]{5,29}$");
    private static final Pattern REFERENCE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{1,39}$");

    private final Map<String, String> erreurs = new LinkedHashMap<>();

    /** Free text: trimmed; null when empty (and an error if the field is mandatory). */
    public String texte(String champ, String valeur, String libelle, int min, int max, boolean obligatoire) {
        String v = nettoyer(valeur);
        if (v.isEmpty()) {
            if (obligatoire) {
                rejeter(champ, libelle + " est obligatoire.");
            }
            return null;
        }
        if (v.length() < min) {
            rejeter(champ, libelle + " doit contenir au moins " + min + " caractères.");
            return null;
        }
        if (v.length() > max) {
            rejeter(champ, libelle + " doit contenir au plus " + max + " caractères.");
            return null;
        }
        return v;
    }

    /** First name / last name: letters, spaces, apostrophes, dots and hyphens. */
    public String nomPropre(String champ, String valeur, String libelle) {
        String v = texte(champ, valeur, libelle, 2, 80, true);
        if (v != null && !NOM_PROPRE.matcher(v).matches()) {
            rejeter(champ, libelle + " ne peut contenir que des lettres, espaces, apostrophes et tirets.");
            return null;
        }
        return v;
    }

    /** E-mail is returned trimmed and lower-cased, like Internaute.setEmail stores it. */
    public String email(String champ, String valeur) {
        String v = texte(champ, valeur, "L'adresse e-mail", 5, 150, true);
        if (v == null) {
            return null;
        }
        v = v.toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(v).matches()) {
            rejeter(champ, "L'adresse e-mail n'est pas valide.");
            return null;
        }
        return v;
    }

    /** At least 8 characters with a letter and a digit; must match its confirmation. */
    public String motDePasse(String champ, String motDePasse, String confirmation) {
        if (motDePasse == null || motDePasse.isEmpty()) {
            rejeter(champ, "Le mot de passe est obligatoire.");
            return null;
        }
        if (motDePasse.length() < 8 || motDePasse.length() > 100) {
            rejeter(champ, "Le mot de passe doit contenir entre 8 et 100 caractères.");
            return null;
        }
        boolean lettre = motDePasse.chars().anyMatch(Character::isLetter);
        boolean chiffre = motDePasse.chars().anyMatch(Character::isDigit);
        if (!lettre || !chiffre) {
            rejeter(champ, "Le mot de passe doit contenir au moins une lettre et un chiffre.");
            return null;
        }
        if (!motDePasse.equals(confirmation)) {
            rejeter("confirmation", "La confirmation ne correspond pas au mot de passe.");
            return null;
        }
        return motDePasse;
    }

    public String telephone(String champ, String valeur) {
        String v = nettoyer(valeur);
        if (v.isEmpty()) {
            return null;
        }
        if (!TELEPHONE.matcher(v).matches()) {
            rejeter(champ, "Le numéro de téléphone n'est pas valide (ex. +212 6 12 34 56 78).");
            return null;
        }
        return v;
    }

    public String reference(String champ, String valeur) {
        String v = nettoyer(valeur);
        if (v.isEmpty()) {
            rejeter(champ, "La référence est obligatoire.");
            return null;
        }
        if (!REFERENCE.matcher(v).matches()) {
            rejeter(champ, "La référence doit contenir 2 à 40 caractères : lettres, chiffres, point, tiret.");
            return null;
        }
        return v.toUpperCase(Locale.ROOT);
    }

    /** Optional image address; only https is accepted (no javascript: or data: URLs in an img tag). */
    public String urlHttps(String champ, String valeur) {
        String v = texte(champ, valeur, "L'adresse de l'image", 1, 255, false);
        if (v != null && (!v.startsWith("https://") || v.matches(".*\\s.*"))) {
            rejeter(champ, "L'adresse de l'image doit commencer par https:// et ne pas contenir d'espace.");
            return null;
        }
        return v;
    }

    /** Amount with at most 2 decimals; accepts "129,90" as well as "129.90". */
    public BigDecimal decimal(String champ, String valeur, String libelle, BigDecimal min, BigDecimal max) {
        String v = valeur == null ? "" : valeur.replaceAll("[\\s\\u00A0\\u202F]", "").replace(',', '.');
        if (v.isEmpty()) {
            rejeter(champ, libelle + " est obligatoire.");
            return null;
        }
        BigDecimal nombre;
        try {
            nombre = new BigDecimal(v);
        } catch (NumberFormatException e) {
            rejeter(champ, libelle + " doit être un nombre (ex. 129,90).");
            return null;
        }
        if (nombre.scale() > 2) {
            rejeter(champ, libelle + " ne peut avoir que 2 décimales.");
            return null;
        }
        if (nombre.compareTo(min) < 0 || nombre.compareTo(max) > 0) {
            rejeter(champ, libelle + " doit être compris entre " + min.toPlainString()
                    + " et " + max.toPlainString() + ".");
            return null;
        }
        return nombre;
    }

    public Integer entier(String champ, String valeur, String libelle, int min, int max) {
        String v = nettoyer(valeur);
        if (v.isEmpty()) {
            rejeter(champ, libelle + " est obligatoire.");
            return null;
        }
        int nombre;
        try {
            nombre = Integer.parseInt(v);
        } catch (NumberFormatException e) {
            rejeter(champ, libelle + " doit être un nombre entier.");
            return null;
        }
        if (nombre < min || nombre > max) {
            rejeter(champ, libelle + " doit être compris entre " + min + " et " + max + ".");
            return null;
        }
        return nombre;
    }

    public Long identifiant(String champ, String valeur, String libelle) {
        String v = nettoyer(valeur);
        try {
            long id = Long.parseLong(v);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException e) {
            // handled below, same message
        }
        rejeter(champ, libelle + " est obligatoire.");
        return null;
    }

    /** Keeps the first error of a field: it is the most relevant one. */
    public void rejeter(String champ, String message) {
        erreurs.putIfAbsent(champ, message);
    }

    public boolean valide() {
        return erreurs.isEmpty();
    }

    /** Throws if any error was collected. */
    public void verifier() {
        if (!erreurs.isEmpty()) {
            throw new ValidationException(erreurs);
        }
    }

    private static String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }
}
