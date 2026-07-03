-- Migration Flyway V1 : Création du schéma pour billing-service

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Table des factures SaaS pour les agences abonnées
CREATE TABLE saas_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL, -- ID logique vers TENANT-SERVICE
    plan_name VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'XOF',
    period_month INTEGER NOT NULL CHECK (period_month >= 1 AND period_month <= 12),
    period_year INTEGER NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID', 'OVERDUE')),
    paid_at TIMESTAMP,
    payment_method VARCHAR(50),
    invoice_pdf_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (agency_id, period_month, period_year)
);

-- Indexation
CREATE INDEX idx_saas_inv_agency ON saas_invoices(agency_id);
CREATE INDEX idx_saas_inv_status ON saas_invoices(status);

COMMENT ON TABLE saas_invoices IS 'Factures récurrentes SaaS générées pour les agences immobilières';
