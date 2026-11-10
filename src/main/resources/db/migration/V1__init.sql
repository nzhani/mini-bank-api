CREATE TABLE accounts (
    id              BIGSERIAL PRIMARY KEY,
    account_number  VARCHAR(20)    NOT NULL UNIQUE,
    owner_name      VARCHAR(100)   NOT NULL,
    currency        VARCHAR(3)     NOT NULL,
    balance         NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status          VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version         BIGINT         NOT NULL DEFAULT 0
);

CREATE TABLE transactions (
    id               BIGSERIAL PRIMARY KEY,
    from_account_id  BIGINT REFERENCES accounts (id),
    to_account_id    BIGINT NOT NULL REFERENCES accounts (id),
    amount           NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    type             VARCHAR(10)    NOT NULL,
    idempotency_key  VARCHAR(64) UNIQUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_tx_from ON transactions (from_account_id, created_at DESC);
CREATE INDEX idx_tx_to   ON transactions (to_account_id, created_at DESC);
