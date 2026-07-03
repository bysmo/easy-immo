-- Migration Flyway V1 : Création du schéma pour lease-service

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Table des locataires
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_user_id VARCHAR(100) UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150),
    national_id VARCHAR(50),
    profession VARCHAR(100),
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table des contrats de baux (leases)
CREATE TABLE leases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL,
    property_id UUID NOT NULL, -- Référence logique vers PROPERTY-SERVICE
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    start_date DATE NOT NULL,
    end_date DATE,
    monthly_rent DECIMAL(12,2) NOT NULL,
    deposit_amount DECIMAL(12,2) NOT NULL,
    payment_day INTEGER NOT NULL DEFAULT 5 CHECK (payment_day >= 1 AND payment_day <= 28),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'TERMINATED', 'EXPIRED')),
    contract_pdf_url TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table des renouvellements de baux
CREATE TABLE lease_renewals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lease_id UUID NOT NULL REFERENCES leases(id) ON DELETE CASCADE,
    new_end_date DATE NOT NULL,
    new_monthly_rent DECIMAL(12,2) NOT NULL,
    notes TEXT,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table des résiliations de baux
CREATE TABLE lease_terminations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lease_id UUID NOT NULL REFERENCES leases(id) ON DELETE CASCADE,
    termination_date DATE NOT NULL,
    reason TEXT NOT NULL,
    deposit_refunded DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexation
CREATE INDEX idx_tenants_phone ON tenants(phone);
CREATE INDEX idx_leases_agency ON leases(agency_id);
CREATE INDEX idx_leases_property ON leases(property_id);
CREATE INDEX idx_leases_tenant ON leases(tenant_id);
CREATE INDEX idx_leases_status ON leases(status);
CREATE INDEX idx_renewals_lease ON lease_renewals(lease_id);
CREATE INDEX idx_terminations_lease ON lease_terminations(lease_id);

COMMENT ON TABLE tenants IS 'Locataires gérés par les agences ou inscrits via application mobile';
COMMENT ON TABLE leases IS 'Contrats de bail associant un locataire à un bien immobilier logique';
COMMENT ON COLUMN leases.property_id IS 'ID de propriété stocké dans PROPERTY-SERVICE';
