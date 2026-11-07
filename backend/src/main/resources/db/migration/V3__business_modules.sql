-- ================= DEVIS =================
CREATE TABLE quotes (
    id            UUID PRIMARY KEY,
    quote_number  VARCHAR(30)   NOT NULL UNIQUE,
    customer_id   UUID          NOT NULL REFERENCES customers (id),
    status        VARCHAR(20)   NOT NULL,
    issue_date    DATE          NOT NULL,
    valid_until   DATE,
    notes         TEXT,
    total_ht      NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_vat     NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_ttc     NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_quotes_customer ON quotes (customer_id);
CREATE INDEX idx_quotes_status ON quotes (status);

CREATE TABLE quote_lines (
    quote_id         UUID          NOT NULL REFERENCES quotes (id) ON DELETE CASCADE,
    line_order       INT           NOT NULL,
    description      VARCHAR(500)  NOT NULL,
    quantity         NUMERIC(12,2) NOT NULL,
    unit_price       NUMERIC(12,2) NOT NULL,
    vat_rate         NUMERIC(5,2)  NOT NULL,
    discount_percent NUMERIC(5,2)  NOT NULL,
    PRIMARY KEY (quote_id, line_order)
);

-- ================= FACTURES =================
CREATE TABLE invoices (
    id              UUID PRIMARY KEY,
    invoice_number  VARCHAR(30)   NOT NULL UNIQUE,
    customer_id     UUID          NOT NULL REFERENCES customers (id),
    quote_id        UUID          REFERENCES quotes (id),
    status          VARCHAR(20)   NOT NULL,
    issue_date      DATE          NOT NULL,
    due_date        DATE          NOT NULL,
    notes           TEXT,
    total_ht        NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_vat       NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_ttc       NUMERIC(14,2) NOT NULL DEFAULT 0,
    amount_paid     NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_invoices_customer ON invoices (customer_id);
CREATE INDEX idx_invoices_status ON invoices (status);
CREATE INDEX idx_invoices_due_date ON invoices (due_date);

CREATE TABLE invoice_lines (
    invoice_id       UUID          NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    line_order       INT           NOT NULL,
    description      VARCHAR(500)  NOT NULL,
    quantity         NUMERIC(12,2) NOT NULL,
    unit_price       NUMERIC(12,2) NOT NULL,
    vat_rate         NUMERIC(5,2)  NOT NULL,
    discount_percent NUMERIC(5,2)  NOT NULL,
    PRIMARY KEY (invoice_id, line_order)
);

CREATE TABLE payments (
    id          UUID PRIMARY KEY,
    invoice_id  UUID          NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    amount      NUMERIC(14,2) NOT NULL,
    paid_at     DATE          NOT NULL,
    method      VARCHAR(30),
    reference   VARCHAR(100),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_payments_paid_at ON payments (paid_at);

-- ================= PROJETS / TACHES =================
CREATE TABLE projects (
    id           UUID PRIMARY KEY,
    name         VARCHAR(255)  NOT NULL,
    description  TEXT,
    customer_id  UUID          NOT NULL REFERENCES customers (id),
    budget       NUMERIC(14,2),
    start_date   DATE,
    due_date     DATE,
    progress     INT           NOT NULL DEFAULT 0,
    status       VARCHAR(20)   NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_projects_customer ON projects (customer_id);
CREATE INDEX idx_projects_status ON projects (status);

CREATE TABLE project_comments (
    id           UUID PRIMARY KEY,
    project_id   UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    author_name  VARCHAR(200) NOT NULL,
    content      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE tasks (
    id              UUID PRIMARY KEY,
    project_id      UUID          NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    title           VARCHAR(255)  NOT NULL,
    description     TEXT,
    priority        VARCHAR(10)   NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    estimate_hours  NUMERIC(8,2),
    spent_hours     NUMERIC(8,2)  NOT NULL DEFAULT 0,
    assignee_id     UUID          REFERENCES users (id),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_tasks_project ON tasks (project_id);
CREATE INDEX idx_tasks_assignee ON tasks (assignee_id);

-- ================= AGENDA =================
CREATE TABLE appointments (
    id           UUID PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    start_at     TIMESTAMP    NOT NULL,
    end_at       TIMESTAMP,
    location     VARCHAR(255),
    notes        TEXT,
    customer_id  UUID         REFERENCES customers (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_appointments_start ON appointments (start_at);

-- ================= NOTIFICATIONS =================
CREATE TABLE notifications (
    id          UUID PRIMARY KEY,
    type        VARCHAR(30)  NOT NULL,
    message     VARCHAR(500) NOT NULL,
    link        VARCHAR(200),
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_created ON notifications (created_at DESC);

-- ================= DOCUMENTS =================
CREATE TABLE documents (
    id            UUID PRIMARY KEY,
    file_name     VARCHAR(255) NOT NULL,
    stored_name   VARCHAR(255) NOT NULL,
    content_type  VARCHAR(150),
    size_bytes    BIGINT       NOT NULL,
    category      VARCHAR(20)  NOT NULL,
    customer_id   UUID         REFERENCES customers (id),
    uploaded_by   VARCHAR(200),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_documents_customer ON documents (customer_id);
