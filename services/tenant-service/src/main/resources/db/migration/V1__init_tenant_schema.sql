-- Migration Flyway V1 : Création du schéma tenant-service

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Plans tarifaires
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    price_monthly DECIMAL(12,2) NOT NULL,
    max_properties INTEGER NOT NULL DEFAULT 10,
    max_agents INTEGER NOT NULL DEFAULT 2,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Agences
CREATE TABLE agencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    logo_url TEXT,
    address TEXT,
    city VARCHAR(100),
    country VARCHAR(5) NOT NULL DEFAULT 'BF',
    phone VARCHAR(20),
    email VARCHAR(150) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'TRIAL'
        CHECK (status IN ('TRIAL', 'ACTIVE', 'SUSPENDED', 'CANCELLED')),
    keycloak_group_id VARCHAR(100),
    subscription_plan_id UUID REFERENCES subscription_plans(id),
    trial_ends_at TIMESTAMP,
    suspended_at TIMESTAMP,
    suspension_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Abonnements
CREATE TABLE agency_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED')),
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index
CREATE INDEX idx_agencies_status ON agencies(status);
CREATE INDEX idx_agencies_country ON agencies(country);
CREATE INDEX idx_agencies_email ON agencies(email);
CREATE INDEX idx_subscriptions_agency ON agency_subscriptions(agency_id);
CREATE INDEX idx_subscriptions_status ON agency_subscriptions(status);

-- Données initiales : plans tarifaires
INSERT INTO subscription_plans (name, display_name, price_monthly, max_properties, max_agents, description) VALUES
('STARTER',    'Starter',    15000,  20,  3,  'Idéal pour les petites agences. 20 biens, 3 agents.'),
('PRO',        'Pro',        35000,  100, 10, 'Pour les agences en croissance. 100 biens, 10 agents.'),
('ENTERPRISE', 'Enterprise', 75000, -1,  -1, 'Illimité. Pour les grandes agences. Biens et agents illimités.');
-- Note: -1 = illimité

COMMENT ON TABLE agencies IS 'Agences immobilières (tenants de la plateforme SaaS Easy-Immo)';
COMMENT ON TABLE subscription_plans IS 'Plans tarifaires SaaS';
COMMENT ON COLUMN agencies.country IS 'Code pays ISO 3166-1 alpha-2 (zone UEMOA: BJ,BF,CI,GW,ML,NE,SN,TG)';
