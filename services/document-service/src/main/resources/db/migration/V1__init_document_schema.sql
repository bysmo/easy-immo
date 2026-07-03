-- Migration Flyway V1 : Création du schéma pour document-service

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Table de suivi des documents PDF générés
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('LEASE_CONTRACT', 'RENT_RECEIPT', 'DEPOSIT_RECEIPT')),
    target_id UUID NOT NULL, -- ID logique de l'entité liée
    minio_object_name TEXT NOT NULL,
    filename VARCHAR(150) NOT NULL,
    generated_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexation
CREATE INDEX idx_docs_agency ON documents(agency_id);
CREATE INDEX idx_docs_target ON documents(target_id);
CREATE INDEX idx_docs_type ON documents(type);

COMMENT ON TABLE documents IS 'Métadonnées des fichiers PDF de baux et reçus générés et stockés dans MinIO';
