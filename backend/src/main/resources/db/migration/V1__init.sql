CREATE TABLE users (
    id              UUID PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE customers (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    company         VARCHAR(255),
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(50),
    address         VARCHAR(500),
    notes           TEXT,
    archived        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_customers_name ON customers (name);
CREATE INDEX idx_customers_archived ON customers (archived);
