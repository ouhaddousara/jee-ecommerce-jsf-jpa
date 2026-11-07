-- Constraints that JPA annotations cannot generate.
-- Applied by SchemaTool after the tables are created (MySQL 8.0.16 or later
-- enforces CHECK). One statement per line, terminated by ';'.

-- EclipseLink creates the discriminator column as nullable: an internaute
-- without a role would be invisible to the application, so forbid it.
ALTER TABLE internaute MODIFY type_internaute VARCHAR(20) NOT NULL;
ALTER TABLE internaute ADD CONSTRAINT chk_internaute_type CHECK (type_internaute IN ('CLIENT', 'ADMIN'));

ALTER TABLE produit ADD CONSTRAINT chk_produit_prix CHECK (prix >= 0);
ALTER TABLE produit ADD CONSTRAINT chk_produit_stock CHECK (stock >= 0);
ALTER TABLE ligne_panier ADD CONSTRAINT chk_ligne_quantite CHECK (quantite >= 1);
ALTER TABLE ligne_panier ADD CONSTRAINT chk_ligne_prix CHECK (prix_unitaire >= 0);
