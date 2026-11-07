# jee-ecommerce-jsf-jpa

Atelier JEE 3 (FST Tanger, LSI, Pr. Elaachak) : application web e-commerce basée sur **JSF** et **JPA**.
C'est la version JSF de l'application de l'atelier 2 ([jee-ecommerce-jpa-mvc2](https://github.com/ouhaddousara/jee-ecommerce-jpa-mvc2)) :
la couche métier et la persistance sont reprises telles quelles, seule la couche web change (servlets et JSP remplacées par des beans CDI et des pages Facelets).

## Fonctionnalités

- **Internaute** : inscription, connexion, déconnexion (Client ou Administrateur).
- **Vitrine** : liste des produits avec recherche, filtre par catégorie et pagination, page de détail.
- **Panier** (clients) : ajout, modification de quantité, retrait, vidage ; prix figé à l'ajout ; contrôle du stock.
- **Administration** (administrateurs) : gestion des produits et des catégories.

## Pile technique

| Couche | Technologie |
|---|---|
| Vue | JSF 2.2 (Mojarra 2.2.20), Facelets, composants composites, aucune bibliothèque tierce |
| Contrôleurs | Beans CDI (`@Named`, `@ViewScoped`) : un bean par gestion |
| Métier | Services (interfaces) et règles dans les entités |
| Persistance | JPA 2.2, EclipseLink 2.7.5, MySQL 8 (pilote 8.0.33) |
| Injection | CDI 2.0, Weld Servlet 3.1.9 |
| Serveur | Tomcat 9 (`javax.*`), Java 17 à l'exécution, classes compilées pour Java 11 |

## Installation

Prérequis : JDK 17, Maven 3.9, MySQL 8, Tomcat 9.0.x.

```powershell
git clone https://github.com/ouhaddousara/jee-ecommerce-jsf-jpa.git
cd jee-ecommerce-jsf-jpa

# 1. Identifiants de la base (db.properties n'est jamais versionné)
Copy-Item src\main\resources\db.properties.example src\main\resources\db.properties
notepad src\main\resources\db.properties        # renseigner db.password

# 2. Base vide
mysql -u root -p -e "source db/create-database.sql"

# 3. Tables, contraintes et données de démonstration :
#    dans Eclipse, Run As > Java Application sur ma.fstt.persistence.SchemaTool, puis taper RESET

# 4. Construction
mvn clean package
```

Déployer `target/ecommerce-jsf-jpa.war` dans Tomcat (ou lancer depuis Eclipse), puis ouvrir
`http://localhost:8081/ecommerce-jsf-jpa/` (le port dépend de la configuration de Tomcat).

## Comptes de démonstration (développement uniquement)

| Rôle | Adresse | Mot de passe |
|---|---|---|
| Administrateur | `admin@example.com` | `Admin#2026` |
| Client (panier rempli) | `yasmine@example.com` | `Client#2026` |
| Client (panier vide) | `omar@example.com` | `Client#2026` |

## Architecture

```
src/main/java/ma/fstt/
  entities/      Internaute (abstraite), Client, Administrateur, Vitrine, Categorie, Produit, Panier, LignePanier
  service/       interfaces, formulaires, validation
  jpa/           TransactionRunner, services JPA, requêtes
  persistence/   configuration JPA, SchemaTool
  web/           beans JSF, filtres, aides (Messages, Chemins, Affichage)
src/main/webapp/
  *.xhtml, admin/*.xhtml        pages
  WEB-INF/templates/layout.xhtml gabarit commun
  resources/components/*.xhtml   composants composites (champ, csrf, icone, visuel, vide)
```

| Atelier 2 (servlet) | Atelier 3 (bean) | Pages |
|---|---|---|
| `ClientServlet` | `ConnexionBean`, `InscriptionBean` | `connexion.xhtml`, `inscription.xhtml` |
| `VitrineServlet` | `VitrineBean`, `DetailBean` | `vitrine.xhtml`, `detail.xhtml` |
| `PanierServlet` | `PanierBean` | `panier.xhtml` |
| `ProduitServlet` | `ProduitAdminBean` | `admin/produits.xhtml`, `admin/produit-form.xhtml` |
| `CategorieServlet` | `CategorieAdminBean` | `admin/categories.xhtml`, `admin/categorie-form.xhtml` |

Diagrammes de classes : [`docs/diagramme-classes.md`](docs/diagramme-classes.md).

## Décisions de conception (JSF)

- **Beans CDI** (`@Named`) plutôt que `@ManagedBean` (déprécié en JSF 2.2) : Weld est déjà en place.
- **`@ViewScoped`** pour les écrans (un formulaire garde son état tant qu'on reste sur la page), jamais d'entité JPA conservée dans l'état de la vue (champs `transient`, rechargés à la demande).
- **`UserSession`** n'a pas d'état propre : la source unique est le `SessionUser` de la session HTTP, que `AuthFilter` (hors CDI) lit aussi.
- **Pas de `h:commandLink` ni de `f:ajax`** : ils produisent des `onclick` en ligne, interdits par la CSP (`script-src 'self'`). Les actions sont des `h:commandButton`, la navigation des `h:link` ou des liens simples.
- **CSRF** : le jeton explicite de l'atelier 2 est conservé (`t:csrf` dans chaque `h:form`, vérifié par `SecurityFilter`) ; on ne s'appuie pas sur le `ViewState`.
- **Post/Redirect/Get** : chaque action se termine par une redirection (`faces-redirect=true`) et un message JSF conservé par le *flash*.
- **Validation dans les services**, pas avec les validateurs JSF : une seule source de règles.
- Les filtres de la vitrine sont des **paramètres de vue** (`?categorie=`, `?q=`, `?page=`) : adresses partageables, bouton « retour » fonctionnel.

## Vérifications automatiques

Sans serveur ni framework de test : dans Eclipse, *Run As > Java Application* sur `ma.fstt.testing.Verifications`
(depuis la racine du projet). Elle contrôle les règles métier, la sécurité et les pages Facelets (XML valide,
pas de composant produisant du script en ligne, jeton CSRF dans chaque formulaire, références EL vers des beans et
des membres existants). Résultat attendu : `TOUT EST CONFORME`.

## Rapport

Le rapport de l'atelier (conception, décisions JSF/CDI/JPA, sécurité, tests, incidents rencontrés et captures) est dans le dépôt :

- PDF : [`docs/rapport/rapport_atelier_jee3.pdf`](docs/rapport/rapport_atelier_jee3.pdf)
- Sources LaTeX : [`docs/rapport/`](docs/rapport/) (`main.tex` et un fichier par chapitre dans `chapitres/`)

Pour le recompiler : `latexmk -pdf main.tex` depuis `docs/rapport/` (paquets `texlive-lang-french` et `texlive-latex-extra` nécessaires).

## Limites connues

- Pas de HTTPS, donc pas d'attribut `Secure` sur le cookie de session.
- Limitation des tentatives de connexion conservée en mémoire (perdue au redémarrage).
- Pas de commande (`Commande`) : hors du périmètre de cet atelier.
- Tests sans framework : JUnit et une base de test seraient une suite logique.
