package ma.fstt.web;

import static ma.fstt.testing.Checks.check;
import static ma.fstt.testing.Checks.section;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Static checks of the Facelets pages: nothing here needs Tomcat or a JSF runtime,
 * so a typo is found before the first deployment. Run from the project root
 * (the working directory of "Run As > Java Application" in Eclipse).
 *
 * <ul>
 *   <li>every page is well-formed XML;</li>
 *   <li>no component that renders an inline script (h:commandLink, f:ajax, onclick...):
 *       the CSP forbids inline scripts;</li>
 *   <li>every h:form contains the CSRF token component;</li>
 *   <li>every #{bean.member} points to an existing @Named bean and a member that exists.</li>
 * </ul>
 */
public final class VuesChecks {

    private static final String JSF_HTML = "http://xmlns.jcp.org/jsf/html";
    private static final String JSF_CORE = "http://xmlns.jcp.org/jsf/core";
    private static final String COMPOSANTS = "http://xmlns.jcp.org/jsf/composite/components";

    /** Names that are not beans: JSF implicit objects and the variables of the pages. */
    private static final Set<String> NON_BEANS = new HashSet<>(Arrays.asList(
            "request", "facesContext", "sessionScope", "requestScope", "cc", "param", "view", "flash",
            // ui:param and loop variables
            "titre", "nav", "p", "cat", "l", "n", "c", "message", "code"));

    private static final Pattern EXPRESSION = Pattern.compile("#\\{([^}]*)\\}");
    private static final Pattern MEMBRE = Pattern.compile("(?<![\\w.'\\]])([A-Za-z_]\\w*)\\.([A-Za-z_]\\w*)");
    private static final Pattern NAMED = Pattern.compile("@Named(?:\\(\\s*\"(\\w+)\"\\s*\\))?");

    private VuesChecks() {
    }

    public static void executer() throws Exception {
        Path webapp = Paths.get("src", "main", "webapp");
        Path sources = Paths.get("src", "main", "java");
        if (!Files.isDirectory(webapp)) {
            section("Vues Facelets");
            check("exécuter depuis la racine du projet (src/main/webapp introuvable)", false);
            return;
        }

        List<Path> pages = new ArrayList<>();
        try (Stream<Path> tous = Files.walk(webapp)) {
            pages.addAll(tous.filter(p -> p.toString().endsWith(".xhtml")).collect(Collectors.toList()));
        }
        Map<String, Class<?>> beans = beansNommes(sources);

        section("Pages Facelets : " + pages.size() + " fichiers");
        check("au moins une page trouvée", !pages.isEmpty());
        check("beans @Named trouvés : " + new java.util.TreeSet<>(beans.keySet()), beans.size() >= 8);

        for (Path page : pages) {
            String nom = webapp.relativize(page).toString().replace(File.separatorChar, '/');
            String texte = new String(Files.readAllBytes(page), StandardCharsets.UTF_8);

            Document document = null;
            try {
                DocumentBuilderFactory usine = DocumentBuilderFactory.newInstance();
                usine.setNamespaceAware(true);
                document = usine.newDocumentBuilder().parse(page.toFile());
            } catch (Exception e) {
                check(nom + " : XML bien formé (" + e.getMessage() + ")", false);
                continue;
            }
            check(nom + " : XML bien formé", true);

            check(nom + " : pas d'h:commandLink ni de f:ajax",
                    count(document, JSF_HTML, "commandLink") == 0 && count(document, JSF_CORE, "ajax") == 0);
            check(nom + " : aucun gestionnaire en ligne (onclick, onchange...)", !aUnGestionnaireEnLigne(document));
            check(nom + " : chaque h:form contient t:csrf", chaqueFormulaireALeJeton(document));

            List<String> inconnus = referencesInvalides(texte, beans);
            check(nom + " : références EL valides" + (inconnus.isEmpty() ? "" : " " + inconnus), inconnus.isEmpty());
        }
    }

    private static int count(Document document, String namespace, String nomLocal) {
        return document.getElementsByTagNameNS(namespace, nomLocal).getLength();
    }

    private static boolean aUnGestionnaireEnLigne(Document document) {
        NodeList elements = document.getElementsByTagName("*");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            for (int a = 0; a < element.getAttributes().getLength(); a++) {
                String nom = element.getAttributes().item(a).getNodeName().toLowerCase(Locale.ROOT);
                if (nom.startsWith("on") && !nom.startsWith("xmlns")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean chaqueFormulaireALeJeton(Document document) {
        NodeList formulaires = document.getElementsByTagNameNS(JSF_HTML, "form");
        for (int i = 0; i < formulaires.getLength(); i++) {
            if (!contient((Element) formulaires.item(i), COMPOSANTS, "csrf")) {
                return false;
            }
        }
        return true;
    }

    private static boolean contient(Element racine, String namespace, String nomLocal) {
        for (Node enfant = racine.getFirstChild(); enfant != null; enfant = enfant.getNextSibling()) {
            if (enfant instanceof Element) {
                Element element = (Element) enfant;
                if (namespace.equals(element.getNamespaceURI()) && nomLocal.equals(element.getLocalName())) {
                    return true;
                }
                if (contient(element, namespace, nomLocal)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** First-level #{bean.member} references that do not match a bean or one of its members. */
    private static List<String> referencesInvalides(String texte, Map<String, Class<?>> beans) {
        List<String> inconnus = new ArrayList<>();
        Matcher expressions = EXPRESSION.matcher(texte);
        while (expressions.find()) {
            // Text between quotes ('...') is data, not a reference.
            String expression = expressions.group(1).replaceAll("'[^']*'", "''");
            Matcher membres = MEMBRE.matcher(expression);
            while (membres.find()) {
                String racine = membres.group(1);
                String membre = membres.group(2);
                if (NON_BEANS.contains(racine)) {
                    continue;
                }
                Class<?> classe = beans.get(racine);
                if (classe == null) {
                    inconnus.add(racine + " (bean inconnu)");
                } else if (!aMembre(classe, membre)) {
                    inconnus.add(racine + "." + membre);
                }
            }
        }
        return inconnus;
    }

    private static boolean aMembre(Class<?> classe, String membre) {
        String suffixe = Character.toUpperCase(membre.charAt(0)) + membre.substring(1);
        for (Method methode : classe.getMethods()) {
            String nom = methode.getName();
            if (nom.equals(membre) || nom.equals("get" + suffixe) || nom.equals("is" + suffixe)) {
                return true;
            }
        }
        return false;
    }

    /** Name used in EL (explicit @Named("x") or the decapitalized class name) to class. */
    private static Map<String, Class<?>> beansNommes(Path sources) throws Exception {
        Map<String, Class<?>> beans = new HashMap<>();
        try (Stream<Path> tous = Files.walk(sources)) {
            for (Path fichier : tous.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                String code = new String(Files.readAllBytes(fichier), StandardCharsets.UTF_8);
                Matcher named = NAMED.matcher(code);
                if (!named.find()) {
                    continue;
                }
                String classe = fichier.getFileName().toString().replace(".java", "");
                String paquet = sources.relativize(fichier.getParent()).toString().replace(File.separatorChar, '.');
                String nom = named.group(1) != null
                        ? named.group(1)
                        : Character.toLowerCase(classe.charAt(0)) + classe.substring(1);
                beans.put(nom, Class.forName(paquet + "." + classe));
            }
        }
        return beans;
    }
}
