package ma.fstt.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import ma.fstt.entities.Administrateur;
import ma.fstt.entities.Categorie;
import ma.fstt.entities.Client;
import ma.fstt.entities.Produit;
import ma.fstt.entities.Vitrine;
import ma.fstt.util.PasswordHasher;

/**
 * Development tool: drops and recreates every table, adds the CHECK
 * constraints that JPA cannot express, and loads demo data.
 *
 * Run it from Eclipse (Run As > Java Application). It is a deliberate,
 * confirmed action and never a side effect of starting Tomcat, because it
 * erases all data.
 *
 * Options: --yes (skip the confirmation), --no-seed (schema only).
 */
public final class SchemaTool {

    static final String DEMO_ADMIN_PASSWORD = "Admin#2026";
    static final String DEMO_CLIENT_PASSWORD = "Client#2026";

    private static final List<String> ENTITIES = List.of(
            "Internaute", "Client", "Administrateur", "Vitrine",
            "Categorie", "Produit", "Panier", "LignePanier");

    private SchemaTool() {
    }

    public static void main(String[] args) throws IOException {
        List<String> options = Arrays.asList(args);
        if (!options.contains("--yes") && !confirmer()) {
            System.out.println("Opération annulée, rien n'a été modifié.");
            return;
        }

        // drop-and-create-tables runs when the factory is created (deploy-on-startup).
        EntityManagerFactory emf = PersistenceConfig.createEntityManagerFactory("drop-and-create-tables");
        try {
            EntityManager em = emf.createEntityManager();
            try {
                appliquerContraintes(em);
                if (!options.contains("--no-seed")) {
                    chargerDonneesDeTest(em);
                }
                afficherBilan(em);
            } finally {
                em.close();
            }
        } finally {
            emf.close();
        }
    }

    /** Not closed on purpose: closing a Scanner on System.in would close System.in itself. */
    @SuppressWarnings("resource")
    private static boolean confirmer() {
        System.out.println("ATTENTION : toutes les tables de la base seront supprimées puis recréées.");
        System.out.print("Tape RESET puis Entrée pour continuer : ");
        Scanner scanner = new Scanner(System.in);
        return scanner.hasNextLine() && "RESET".equals(scanner.nextLine().trim());
    }

    /** Executes db/constraints.sql (CHECK constraints) statement by statement. */
    private static void appliquerContraintes(EntityManager em) throws IOException {
        List<String> statements = lireScript("db/constraints.sql");
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            for (String sql : statements) {
                em.createNativeQuery(sql).executeUpdate();
            }
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        System.out.println(statements.size() + " contrainte(s) CHECK appliquée(s).");
    }

    /** Reads a classpath SQL script: '--' comment lines are dropped, statements split on ';'. */
    static List<String> lireScript(String resource) throws IOException {
        try (InputStream in = SchemaTool.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Script SQL introuvable sur le classpath : " + resource);
            }
            StringBuilder sql = new StringBuilder();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (!line.trim().startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
            List<String> statements = new ArrayList<>();
            for (String statement : sql.toString().split(";")) {
                if (!statement.isBlank()) {
                    statements.add(statement.trim());
                }
            }
            return statements;
        }
    }

    private static void chargerDonneesDeTest(EntityManager em) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            Vitrine vitrine = new Vitrine("Atlas Boutique",
                    "La sélection du moment, livrée partout au Maroc.", true);

            Categorie informatique = vitrine.ajouterCategorie(new Categorie("Informatique",
                    "Claviers, souris et stockage pour travailler confortablement"));
            Produit clavier = informatique.ajouterProduit(new Produit("INF-001",
                    "Clavier mécanique 75 %",
                    "Switchs linéaires, rétroéclairage blanc, câble USB-C détachable.",
                    prix("549.00"), 25, true));
            Produit souris = informatique.ajouterProduit(new Produit("INF-002",
                    "Souris sans fil ergonomique",
                    "Forme verticale, 6 boutons, autonomie de 12 mois.",
                    prix("229.00"), 40, true));
            // Low stock on purpose: exercises the "last units" badge and the stock rule.
            informatique.ajouterProduit(new Produit("INF-003", "SSD externe 1 To",
                    "Boîtier aluminium, USB 3.2, jusqu'à 1 000 Mo/s.",
                    prix("899.00"), 3, true));

            Categorie maison = vitrine.ajouterCategorie(new Categorie("Maison & Cuisine",
                    "Petit électroménager et art de la table"));
            maison.ajouterProduit(new Produit("MAI-001", "Cafetière filtre 12 tasses",
                    "Maintien au chaud 40 minutes, arrêt automatique.",
                    prix("389.00"), 12, true));
            // Out of stock on purpose: exercises the "sold out" state.
            maison.ajouterProduit(new Produit("MAI-002", "Bouilloire en verre 1,7 L",
                    "Base lumineuse, coupure automatique à l'ébullition.",
                    prix("199.00"), 0, true));
            maison.ajouterProduit(new Produit("MAI-003", "Set de 6 verres à thé",
                    "Verre trempé décoré à la main.",
                    prix("119.00"), 60, true));

            Categorie mode = vitrine.ajouterCategorie(new Categorie("Mode",
                    "Accessoires du quotidien"));
            mode.ajouterProduit(new Produit("MOD-001", "Sac à dos urbain 35 L",
                    "Compartiment ordinateur 15 pouces, tissu déperlant.",
                    prix("299.00"), 18, true));
            mode.ajouterProduit(new Produit("MOD-002", "Casquette coton brodée",
                    "Taille réglable, coton épais.",
                    prix("89.00"), 50, true));
            // Inactive on purpose: must never appear in the storefront nor be added to a cart.
            mode.ajouterProduit(new Produit("MOD-003", "Écharpe en laine",
                    "Retirée de la vente (fin de série).",
                    prix("159.00"), 7, false));

            Categorie sport = vitrine.ajouterCategorie(new Categorie("Sport & Loisirs",
                    "De quoi bouger à la maison comme en plein air"));
            sport.ajouterProduit(new Produit("SPO-001", "Tapis de yoga antidérapant",
                    "6 mm d'épaisseur, sangle de transport incluse.",
                    prix("179.00"), 30, true));
            sport.ajouterProduit(new Produit("SPO-002", "Haltères réglables 2 × 10 kg",
                    "Disques et barres en fonte, fermeture sécurisée.",
                    prix("649.00"), 5, true));
            sport.ajouterProduit(new Produit("SPO-003", "Gourde isotherme 750 ml",
                    "Acier inoxydable, froid 24 h, chaud 12 h.",
                    prix("139.00"), 80, true));

            // Cascades PERSIST: categories and products are saved with the storefront.
            em.persist(vitrine);

            em.persist(new Administrateur("Boutique", "Admin", "admin@example.com",
                    PasswordHasher.hash(DEMO_ADMIN_PASSWORD), "ADM-001"));

            // This client gets a non-empty cart: 549 + 2 x 229 = 1007.00
            Client yasmine = new Client("Benani", "Yasmine", "yasmine@example.com",
                    PasswordHasher.hash(DEMO_CLIENT_PASSWORD),
                    "12 rue des Orangers, Tanger", "+212 6 00 00 00 01");
            yasmine.getPanier().ajouterProduit(clavier, 1);
            yasmine.getPanier().ajouterProduit(souris, 2);
            em.persist(yasmine);

            // This one keeps an empty cart (empty-state page).
            em.persist(new Client("El Idrissi", "Omar", "omar@example.com",
                    PasswordHasher.hash(DEMO_CLIENT_PASSWORD),
                    "5 avenue Mohammed V, Tanger", "+212 6 00 00 00 02"));

            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
        System.out.println("Données de test chargées.");
    }

    /** Row count per entity: a smoke test of the mapping and of the JPQL entity names. */
    private static void afficherBilan(EntityManager em) {
        System.out.println();
        System.out.println("=== Bilan ===");
        for (String entity : ENTITIES) {
            Long count = em.createQuery("SELECT COUNT(e) FROM " + entity + " e", Long.class)
                    .getSingleResult();
            System.out.printf("%-15s %d%n", entity, count);
        }
        System.out.println();
        System.out.println("Comptes de démonstration :");
        System.out.println("  admin@example.com    / " + DEMO_ADMIN_PASSWORD + "   (administrateur)");
        System.out.println("  yasmine@example.com  / " + DEMO_CLIENT_PASSWORD + "  (client, panier de 1007.00)");
        System.out.println("  omar@example.com     / " + DEMO_CLIENT_PASSWORD + "  (client, panier vide)");
    }

    private static BigDecimal prix(String value) {
        return new BigDecimal(value);
    }
}
