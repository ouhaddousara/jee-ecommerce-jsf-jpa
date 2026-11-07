package ma.fstt.web;

import java.io.Serializable;

import ma.fstt.entities.Internaute;

/**
 * What the HTTP session remembers about the signed-in user: three plain
 * values, never a JPA entity (an entity kept in a session goes stale and
 * drags its whole object graph along).
 */
public final class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Session attribute name. */
    public static final String CLE = "utilisateur";

    private final long id;
    private final String nomComplet;
    private final String role;

    public SessionUser(long id, String nomComplet, String role) {
        this.id = id;
        this.nomComplet = nomComplet;
        this.role = role;
    }

    public static SessionUser de(Internaute internaute) {
        return new SessionUser(internaute.getId(), internaute.getNomComplet(), internaute.getRole());
    }

    public long getId() {
        return id;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isClient() {
        return "CLIENT".equals(role);
    }
}
