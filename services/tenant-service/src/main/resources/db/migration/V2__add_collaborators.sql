-- Migration Flyway V2 : Ajout de la table collaborators pour la gestion des agents d'une agence

CREATE TABLE collaborators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_user_id VARCHAR(100) UNIQUE,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20),
    role VARCHAR(50) NOT NULL, -- AGENCY_ADMIN, AGENCY_AGENT
    status VARCHAR(20) NOT NULL DEFAULT 'INVITED', -- ACTIVE, INVITED, INACTIVE
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_collaborators_agency ON collaborators(agency_id);

COMMENT ON TABLE collaborators IS 'Agents et collaborateurs associés aux agences immobilières';
