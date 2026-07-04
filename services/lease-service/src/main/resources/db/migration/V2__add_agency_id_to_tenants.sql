-- Migration V2 : Ajout de la colonne agency_id à la table tenants
-- Permet l'isolation multi-tenant des locataires sans dépendre d'une JOIN sur les baux

ALTER TABLE tenants ADD COLUMN IF NOT EXISTS agency_id UUID;

CREATE INDEX IF NOT EXISTS idx_tenants_agency_id ON tenants(agency_id);
