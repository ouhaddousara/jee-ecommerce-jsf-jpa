package ma.fstt.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Builds the EntityManagerFactory.
 *
 * persistence.xml holds what never changes (provider, entity list, cache
 * policy). Everything that depends on the machine (JDBC URL, credentials,
 * DDL mode, log level) is read from db.properties, which is git-ignored, and
 * passed as overrides. Credentials therefore never reach the repository.
 */
public final class PersistenceConfig {

    public static final String PERSISTENCE_UNIT = "ecommercePU";

    private static final String RESOURCE = "db.properties";
    private static final Set<String> DDL_MODES =
            Set.of("none", "create-tables", "create-or-extend-tables", "drop-and-create-tables");

    private PersistenceConfig() {
    }

    /** Uses the DDL mode of db.properties (default: create-or-extend-tables). */
    public static EntityManagerFactory createEntityManagerFactory() {
        return createEntityManagerFactory(null);
    }

    /** @param ddlModeOverride forces a DDL mode whatever db.properties says; null keeps the file value. */
    public static EntityManagerFactory createEntityManagerFactory(String ddlModeOverride) {
        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, buildProperties(ddlModeOverride));
    }

    static Map<String, Object> buildProperties(String ddlModeOverride) {
        Properties file = load();
        String ddl = ddlModeOverride != null ? ddlModeOverride
                : file.getProperty("jpa.ddl", "create-or-extend-tables").trim();
        if (!DDL_MODES.contains(ddl)) {
            throw new IllegalStateException("Invalid jpa.ddl value '" + ddl + "'. Allowed: " + DDL_MODES);
        }

        Map<String, Object> props = new HashMap<>();
        props.put("javax.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver");
        props.put("javax.persistence.jdbc.url", required(file, "db.url"));
        props.put("javax.persistence.jdbc.user", required(file, "db.user"));
        props.put("javax.persistence.jdbc.password", file.getProperty("db.password", ""));
        props.put("eclipselink.ddl-generation", ddl);
        props.put("eclipselink.ddl-generation.output-mode", "database");
        props.put("eclipselink.logging.level", file.getProperty("jpa.log", "INFO").trim());
        // Fail fast: connect and generate the schema when the factory is created,
        // not on the first request.
        props.put("eclipselink.deploy-on-startup", "true");
        return props;
    }

    private static Properties load() {
        try (InputStream in = PersistenceConfig.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("'" + RESOURCE + "' not found on the classpath. "
                        + "Copy db.properties.example to db.properties in src/main/resources "
                        + "and fill in your MySQL credentials.");
            }
            Properties file = new Properties();
            file.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
    }

    private static String required(Properties file, String key) {
        String value = file.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property '" + key + "' in " + RESOURCE);
        }
        return value.trim();
    }
}
