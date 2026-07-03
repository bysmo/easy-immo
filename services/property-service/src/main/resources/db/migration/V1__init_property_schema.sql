-- Migration Flyway V1 : Création du schéma pour property-service

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Table des Propriétaires
CREATE TABLE owners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(150),
    national_id VARCHAR(50),
    address TEXT,
    bank_name VARCHAR(100),
    bank_account VARCHAR(100),
    mobile_money_phone VARCHAR(20),
    mobile_money_provider VARCHAR(20),
    share_percentage DECIMAL(5,2) NOT NULL DEFAULT 80.00,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table des Biens Immobiliers
CREATE TABLE properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL,
    owner_id UUID REFERENCES owners(id) ON DELETE SET NULL,
    reference VARCHAR(50),
    type VARCHAR(30) NOT NULL CHECK (type IN ('APPARTEMENT', 'VILLA', 'STUDIO', 'BOUTIQUE', 'BUREAU', 'ENTREPOT', 'CHAMBRE', 'DUPLEX')),
    address TEXT NOT NULL,
    city VARCHAR(100),
    country CHAR(2) NOT NULL DEFAULT 'BJ',
    rooms INTEGER,
    bathrooms INTEGER,
    area_sqm DECIMAL(8,2),
    floor INTEGER,
    current_rent DECIMAL(12,2) NOT NULL,
    deposit_months INTEGER NOT NULL DEFAULT 2,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'RESERVED')),
    description TEXT,
    amenities TEXT, -- Stockage sous forme JSON ou texte simple séparé par virgule
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table des Photos des Biens
CREATE TABLE property_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    filename TEXT NOT NULL,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    content_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table de l'Historique des Loyers
CREATE TABLE property_rent_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    old_rent DECIMAL(12,2),
    new_rent DECIMAL(12,2) NOT NULL,
    effective_date DATE NOT NULL DEFAULT CURRENT_DATE,
    reason TEXT,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table des Réparations et Maintenance
CREATE TABLE property_repairs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    agency_id UUID NOT NULL,
    description TEXT NOT NULL,
    estimated_cost DECIMAL(12,2),
    actual_cost DECIMAL(12,2),
    repair_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    payer VARCHAR(20) CHECK (payer IN ('AGENCY', 'OWNER', 'TENANT')),
    notes TEXT,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexation pour accélérer les filtres et recherches multi-tenancy
CREATE INDEX idx_owners_agency ON owners(agency_id);
CREATE INDEX idx_properties_agency ON properties(agency_id);
CREATE INDEX idx_properties_status ON properties(status);
CREATE INDEX idx_properties_city ON properties(city);
CREATE INDEX idx_photos_property ON property_photos(property_id);
CREATE INDEX idx_repairs_property ON property_repairs(property_id);
CREATE INDEX idx_repairs_agency ON property_repairs(agency_id);

COMMENT ON TABLE owners IS 'Propriétaires des biens gérés par les agences';
COMMENT ON TABLE properties IS 'Biens immobiliers gérés (appartements, villas, etc.)';
COMMENT ON TABLE property_repairs IS 'Historique et suivi de la maintenance et des réparations';
