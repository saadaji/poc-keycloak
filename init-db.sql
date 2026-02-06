-- Create openfga database
CREATE DATABASE openfga;

-- Create openfga schema in keycloak database for isolation
\c keycloak
CREATE SCHEMA IF NOT EXISTS openfga;
GRANT ALL ON SCHEMA openfga TO admin;
