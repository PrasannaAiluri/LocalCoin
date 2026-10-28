-- Local Coin assessment schema - MySQL 8+
-- Run this file against a development database only. Do not use demo credentials in production.
CREATE DATABASE IF NOT EXISTS localcoin_banking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE localcoin_banking;

CREATE TABLE branch (
    branch_id BIGINT NOT NULL AUTO_INCREMENT,
    branch_code VARCHAR(20) NOT NULL,
    name VARCHAR(120) NOT NULL,
    address_line1 VARCHAR(160) NOT NULL,
    address_line2 VARCHAR(160) NULL,
    city VARCHAR(80) NOT NULL,
    province_state VARCHAR(80) NULL,
    postal_code VARCHAR(20) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (branch_id),
    UNIQUE KEY uk_branch_code (branch_code),
    KEY idx_branch_status_city (status, city),
    CONSTRAINT chk_branch_status CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE legal_person (
    legal_person_id BIGINT NOT NULL AUTO_INCREMENT,
    person_type VARCHAR(10) NOT NULL DEFAULT 'INDIVIDUAL',
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100) NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NULL,
    email VARCHAR(254) NULL,
    phone_e164 VARCHAR(20) NULL,
    government_id_type VARCHAR(40) NULL,
    government_id_value_encrypted VARBINARY(512) NULL,
    government_id_last4 VARCHAR(4) NULL,
    identity_verified BOOLEAN NOT NULL DEFAULT FALSE,
    identity_verified_at TIMESTAMP(6) NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (legal_person_id),
    KEY idx_person_name_dob (last_name, first_name, date_of_birth),
    KEY idx_person_email (email),
    CONSTRAINT chk_person_type CHECK (person_type IN ('INDIVIDUAL','BUSINESS')),
    CONSTRAINT chk_person_status CHECK (status IN ('ACTIVE','RESTRICTED','CLOSED'))
) ENGINE=InnoDB;

CREATE TABLE legal_person_address (
    address_id BIGINT NOT NULL AUTO_INCREMENT,
    legal_person_id BIGINT NOT NULL,
    address_type VARCHAR(15) NOT NULL DEFAULT 'RESIDENTIAL',
    address_line1 VARCHAR(160) NOT NULL,
    address_line2 VARCHAR(160) NULL,
    city VARCHAR(80) NOT NULL,
    province_state VARCHAR(80) NULL,
    postal_code VARCHAR(20) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (address_id),
    KEY idx_address_person_current (legal_person_id, is_current),
    CONSTRAINT fk_address_person FOREIGN KEY (legal_person_id) REFERENCES legal_person (legal_person_id),
    CONSTRAINT chk_address_dates CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT chk_address_type CHECK (address_type IN ('RESIDENTIAL','MAILING','BUSINESS','OTHER'))
) ENGINE=InnoDB;

CREATE TABLE bank_account (
    account_id BIGINT NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(34) NOT NULL,
    legal_person_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    account_type VARCHAR(10) NOT NULL DEFAULT 'CURRENT',
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    opened_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    closed_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (account_id),
    UNIQUE KEY uk_account_number (account_number),
    KEY idx_account_person_status (legal_person_id, status),
    KEY idx_account_branch (branch_id),
    CONSTRAINT fk_account_person FOREIGN KEY (legal_person_id) REFERENCES legal_person (legal_person_id),
    CONSTRAINT fk_account_branch FOREIGN KEY (branch_id) REFERENCES branch (branch_id),
    CONSTRAINT chk_account_currency CHECK (currency REGEXP '^[A-Z]{3}$'),
    CONSTRAINT chk_account_type CHECK (account_type IN ('CURRENT','SAVINGS','BUSINESS')),
    CONSTRAINT chk_account_status CHECK (status IN ('PENDING','ACTIVE','FROZEN','CLOSED')),
    CONSTRAINT chk_account_closed CHECK (
        (status <> 'CLOSED' AND closed_at IS NULL) OR
        (status = 'CLOSED' AND closed_at IS NOT NULL)
    )
) ENGINE=InnoDB;

-- Supported currencies in the Java app all have two minor-unit decimal places.
-- A real multi-currency system should model precision and rounding policy per currency.
CREATE TABLE account_balance (
    account_id BIGINT NOT NULL,
    available_balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    ledger_balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (account_id),
    CONSTRAINT fk_balance_account FOREIGN KEY (account_id) REFERENCES bank_account (account_id),
    CONSTRAINT chk_balance_nonnegative CHECK (available_balance >= 0.00 AND ledger_balance >= 0.00)
) ENGINE=InnoDB;

CREATE TABLE banking_transaction (
    transaction_id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_reference VARCHAR(36) NOT NULL,
    transaction_type VARCHAR(10) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    source_account_id BIGINT NULL,
    destination_account_id BIGINT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255) NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    external_reference VARCHAR(100) NULL,
    initiated_by_legal_person_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    posted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    failure_reason VARCHAR(255) NULL,
    PRIMARY KEY (transaction_id),
    UNIQUE KEY uk_transaction_reference (transaction_reference),
    UNIQUE KEY uk_tx_account_idempotency (source_account_id, idempotency_key),
    KEY idx_tx_source_created (source_account_id, created_at),
    KEY idx_tx_destination_created (destination_account_id, created_at),
    KEY idx_tx_status_created (status, created_at),
    CONSTRAINT fk_tx_source_account FOREIGN KEY (source_account_id) REFERENCES bank_account (account_id),
    CONSTRAINT fk_tx_destination_account FOREIGN KEY (destination_account_id) REFERENCES bank_account (account_id),
    CONSTRAINT fk_tx_initiator FOREIGN KEY (initiated_by_legal_person_id) REFERENCES legal_person (legal_person_id),
    CONSTRAINT chk_tx_amount CHECK (amount > 0.00),
    CONSTRAINT chk_tx_currency CHECK (currency REGEXP '^[A-Z]{3}$'),
    CONSTRAINT chk_tx_type CHECK (transaction_type IN ('TRANSFER','WITHDRAWAL','DEPOSIT','PAYMENT')),
    CONSTRAINT chk_tx_status CHECK (status IN ('PENDING','POSTED','FAILED','REVERSED')),
    CONSTRAINT chk_tx_accounts CHECK (
        source_account_id IS NULL OR destination_account_id IS NULL OR source_account_id <> destination_account_id
    ),
    CONSTRAINT chk_tx_posted_time CHECK (status <> 'POSTED' OR posted_at IS NOT NULL)
) ENGINE=InnoDB;

-- Ledger rows are history; do not edit or delete posted entries in normal workflows.
CREATE TABLE ledger_entry (
    ledger_entry_id BIGINT NOT NULL AUTO_INCREMENT,
    transaction_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    entry_type VARCHAR(6) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_after DECIMAL(19,2) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (ledger_entry_id),
    KEY idx_ledger_account_created (account_id, created_at),
    KEY idx_ledger_transaction (transaction_id),
    CONSTRAINT fk_ledger_tx FOREIGN KEY (transaction_id) REFERENCES banking_transaction (transaction_id),
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES bank_account (account_id),
    CONSTRAINT chk_ledger_amount CHECK (amount > 0.00),
    CONSTRAINT chk_ledger_currency CHECK (currency REGEXP '^[A-Z]{3}$'),
    CONSTRAINT chk_ledger_type CHECK (entry_type IN ('DEBIT','CREDIT'))
) ENGINE=InnoDB;

CREATE TABLE audit_event (
    audit_event_id BIGINT NOT NULL AUTO_INCREMENT,
    actor_legal_person_id BIGINT NULL,
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80) NOT NULL,
    outcome VARCHAR(10) NOT NULL,
    correlation_id VARCHAR(100) NULL,
    metadata VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (audit_event_id),
    KEY idx_audit_entity (entity_type, entity_id, created_at),
    KEY idx_audit_actor_created (actor_legal_person_id, created_at),
    KEY idx_audit_event_created (event_type, created_at),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_legal_person_id) REFERENCES legal_person (legal_person_id),
    CONSTRAINT chk_audit_outcome CHECK (outcome IN ('SUCCESS','FAILURE','DENIED'))
) ENGINE=InnoDB;
