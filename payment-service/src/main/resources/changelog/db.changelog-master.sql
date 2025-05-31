--liquibase formatted sql

--changeset giampaolo:02-create-payment-table
CREATE TABLE payment (
    id VARCHAR(36) PRIMARY KEY, -- UUIDs are often stored as VARCHAR(36) in databases
    payment_id VARCHAR(36) NOT NULL, -- As specified in Java entity
    sender_account_id VARCHAR(255) NOT NULL,
    receiver_account_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL, -- Storing enum as String
    idempotency_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Automatically set on insert
    updated_at TIMESTAMP -- Can be null initially, updated on modification
);

--rollback DROP TABLE payment;
