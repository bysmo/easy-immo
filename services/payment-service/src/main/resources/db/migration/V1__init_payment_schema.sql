-- Migration Flyway V1 : Création du schéma pour payment-service

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Table des cautions attendues
CREATE TABLE deposit_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL,
    lease_id UUID NOT NULL, -- Référence logique vers LEASE-SERVICE
    total_amount DECIMAL(12,2) NOT NULL,
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PARTIAL', 'PAID', 'LATE', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table des règlements de cautions
CREATE TABLE deposit_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    deposit_payment_id UUID NOT NULL REFERENCES deposit_payments(id) ON DELETE CASCADE,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CASH', 'MTN_MOMO', 'ORANGE_MONEY', 'BANK_TRANSFER', 'WAVE')),
    transaction_ref VARCHAR(100),
    payment_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    receipt_url TEXT,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table des loyers attendus (mensuels)
CREATE TABLE rent_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL,
    lease_id UUID NOT NULL, -- Référence logique vers LEASE-SERVICE
    period_month INTEGER NOT NULL CHECK (period_month >= 1 AND period_month <= 12),
    period_year INTEGER NOT NULL,
    expected_amount DECIMAL(12,2) NOT NULL,
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    late_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PARTIAL', 'PAID', 'LATE', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (lease_id, period_month, period_year)
);

-- Table des transactions de loyer
CREATE TABLE rent_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rent_payment_id UUID NOT NULL REFERENCES rent_payments(id) ON DELETE CASCADE,
    amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CASH', 'MTN_MOMO', 'ORANGE_MONEY', 'BANK_TRANSFER', 'WAVE')),
    transaction_ref VARCHAR(100),
    payment_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    receipt_url TEXT,
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Table des transactions Mobile Money (suivi technique MTN/Orange)
CREATE TABLE mobile_money_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('CASH', 'MTN_MOMO', 'ORANGE_MONEY', 'BANK_TRANSFER', 'WAVE')),
    country_code VARCHAR(5) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    amount VARCHAR(50) NOT NULL,
    currency VARCHAR(5) NOT NULL DEFAULT 'XOF',
    external_ref VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    callback_payload TEXT,
    payment_type VARCHAR(20) NOT NULL CHECK (payment_type IN ('RENT', 'DEPOSIT')),
    payment_target_id UUID NOT NULL,
    initiated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

-- Table des reversements propriétaires
CREATE TABLE owner_disbursements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id UUID NOT NULL,
    property_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    period_month INTEGER NOT NULL CHECK (period_month >= 1 AND period_month <= 12),
    period_year INTEGER NOT NULL,
    total_rent_collected DECIMAL(12,2) NOT NULL,
    owner_share_pct DECIMAL(5,2) NOT NULL DEFAULT 80.00,
    owner_amount DECIMAL(12,2) NOT NULL,
    agency_commission DECIMAL(12,2) NOT NULL,
    repairs_deducted DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    net_owner_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID')),
    payment_method VARCHAR(20) CHECK (payment_method IN ('CASH', 'MTN_MOMO', 'ORANGE_MONEY', 'BANK_TRANSFER', 'WAVE')),
    paid_at TIMESTAMP,
    processed_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexation
CREATE INDEX idx_deposits_agency ON deposit_payments(agency_id);
CREATE INDEX idx_deposits_lease ON deposit_payments(lease_id);
CREATE INDEX idx_deposits_status ON deposit_payments(status);
CREATE INDEX idx_rents_agency ON rent_payments(agency_id);
CREATE INDEX idx_rents_lease ON rent_payments(lease_id);
CREATE INDEX idx_rents_period ON rent_payments(period_year, period_month);
CREATE INDEX idx_rents_status ON rent_payments(status);
CREATE INDEX idx_momo_req_external ON mobile_money_requests(external_ref);
CREATE INDEX idx_disb_agency ON owner_disbursements(agency_id);
CREATE INDEX idx_disb_owner ON owner_disbursements(owner_id);
CREATE INDEX idx_disb_status ON owner_disbursements(status);

COMMENT ON TABLE deposit_payments IS 'Suivi du recouvrement de la caution de bail';
COMMENT ON TABLE rent_payments IS 'Mensualités de loyers à recouvrer';
COMMENT ON TABLE mobile_money_requests IS 'Suivi technique des transactions MTN MoMo et Orange Money';
COMMENT ON TABLE owner_disbursements IS 'États de reversement mensuels dus aux propriétaires';
