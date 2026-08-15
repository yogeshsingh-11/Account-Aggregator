-- ============================================================================
-- Account Aggregator service — initial schema
-- Design notes:
--  * Raw FIP payloads are NOT stored — only normalized canonical fields plus
--    a minimal reference. This limits PII exposure at rest per requirements.
--  * account_number / masked identifiers are stored masked; full values are
--    never persisted if avoidable (Digio/AA payloads already return masked
--    account numbers such as "XXXXXXXX1234").
--  * Every consent + fetch action is written to audit_log for compliance.
-- ============================================================================

CREATE TABLE consent_record (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_ref            VARCHAR(128) NOT NULL,           -- internal customer identifier (not raw PII)
    consent_handle          VARCHAR(128) UNIQUE,              -- returned by Digio/AA after consent creation
    consent_id              VARCHAR(128) UNIQUE,              -- final consent artifact id once APPROVED
    template_id             VARCHAR(128) NOT NULL,
    purpose_code            VARCHAR(64)  NOT NULL,
    fi_types                VARCHAR(256) NOT NULL,            -- comma-separated: DEPOSIT,TERM_DEPOSIT,RECURRING_DEPOSIT...
    consent_start           TIMESTAMPTZ,
    consent_expiry          TIMESTAMPTZ,
    data_range_from         TIMESTAMPTZ,
    data_range_to           TIMESTAMPTZ,
    status                  VARCHAR(32) NOT NULL DEFAULT 'PENDING',  -- PENDING, ACTIVE, REJECTED, EXPIRED, REVOKED, FAILED
    idempotency_key         VARCHAR(128) UNIQUE,
    requested_by            VARCHAR(128),                     -- FIU service/user that requested it
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_consent_customer_ref ON consent_record (customer_ref);
CREATE INDEX idx_consent_status ON consent_record (status);
CREATE INDEX idx_consent_handle ON consent_record (consent_handle);

-- ----------------------------------------------------------------------------

CREATE TABLE fi_data_request (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consent_record_id       UUID NOT NULL REFERENCES consent_record (id) ON DELETE CASCADE,
    session_id              VARCHAR(128) UNIQUE,              -- AA "FI data session" id from Digio
    status                  VARCHAR(32) NOT NULL DEFAULT 'PENDING',  -- PENDING, READY, DELIVERED, FAILED, EXPIRED
    idempotency_key         VARCHAR(128) UNIQUE,
    requested_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    fetched_at              TIMESTAMPTZ,
    error_reason            VARCHAR(512),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_fi_request_consent ON fi_data_request (consent_record_id);
CREATE INDEX idx_fi_request_status ON fi_data_request (status);

-- ----------------------------------------------------------------------------
-- Canonical normalized models — mapped from heterogeneous FIP payloads
-- ----------------------------------------------------------------------------

CREATE TABLE account (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fi_data_request_id      UUID NOT NULL REFERENCES fi_data_request (id) ON DELETE CASCADE,
    fip_id                  VARCHAR(128) NOT NULL,
    fi_type                 VARCHAR(64)  NOT NULL,            -- DEPOSIT, TERM_DEPOSIT, RECURRING_DEPOSIT, ...
    masked_account_number   VARCHAR(64)  NOT NULL,             -- e.g. XXXXXXXX1234 — never store full account number
    account_type            VARCHAR(64),
    ifsc                    VARCHAR(16),
    currency                VARCHAR(8) DEFAULT 'INR',
    current_balance         NUMERIC(18,2),
    balance_as_of            TIMESTAMPTZ,
    status                  VARCHAR(32),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (fi_data_request_id, masked_account_number, fip_id)
);

CREATE INDEX idx_account_fi_request ON account (fi_data_request_id);
CREATE INDEX idx_account_fip ON account (fip_id);

CREATE TABLE transaction (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id              UUID NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    txn_ref_id              VARCHAR(128),                     -- FIP-provided transaction ref, if any
    txn_type                VARCHAR(16) NOT NULL,              -- DEBIT / CREDIT
    amount                  NUMERIC(18,2) NOT NULL,
    balance_after_txn       NUMERIC(18,2),
    txn_timestamp            TIMESTAMPTZ NOT NULL,
    value_date              DATE,
    narration               VARCHAR(512),
    mode                    VARCHAR(32),                       -- UPI, NEFT, IMPS, CASH, CARD, ...
    category                VARCHAR(64),                        -- derived/normalized category, nullable
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_txn_account ON transaction (account_id);
CREATE INDEX idx_txn_timestamp ON transaction (txn_timestamp);
CREATE INDEX idx_txn_account_timestamp ON transaction (account_id, txn_timestamp DESC);

CREATE TABLE loan_account (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id              UUID NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    loan_type               VARCHAR(64),
    sanctioned_amount       NUMERIC(18,2),
    outstanding_principal   NUMERIC(18,2),
    interest_rate           NUMERIC(6,3),
    tenure_months           INTEGER,
    next_emi_amount         NUMERIC(18,2),
    next_emi_date           DATE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_loan_account ON loan_account (account_id);

-- ----------------------------------------------------------------------------
-- Audit log — every consent/fetch state transition and API access is recorded
-- ----------------------------------------------------------------------------

CREATE TABLE audit_log (
    id                      BIGSERIAL PRIMARY KEY,
    event_type              VARCHAR(64) NOT NULL,   -- CONSENT_CREATED, CONSENT_STATUS_UPDATED, FI_FETCH_INITIATED, FI_DATA_RECEIVED, WEBHOOK_RECEIVED, ACCESS_DENIED, ...
    entity_type              VARCHAR(64),             -- CONSENT, FI_DATA_REQUEST
    entity_id                VARCHAR(128),
    actor                   VARCHAR(128),            -- service/user/system that triggered the event
    correlation_id           VARCHAR(64),
    detail                  TEXT,                    -- human-readable, PII-scrubbed detail
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_created_at ON audit_log (created_at);
CREATE INDEX idx_audit_correlation ON audit_log (correlation_id);

-- ----------------------------------------------------------------------------
-- Idempotency tracking for inbound POSTs (belt-and-suspenders alongside the
-- unique idempotency_key columns above)
-- ----------------------------------------------------------------------------

CREATE TABLE idempotency_record (
    idempotency_key         VARCHAR(128) PRIMARY KEY,
    request_path            VARCHAR(256) NOT NULL,
    response_status         INTEGER,
    response_body           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_idempotency_created_at ON idempotency_record (created_at);
