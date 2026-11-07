-- Creates the (empty) database. EclipseLink creates the tables themselves.
-- utf8mb4 so that accents and emojis survive from the form to the table.
CREATE DATABASE IF NOT EXISTS ecommerce_jsf
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
