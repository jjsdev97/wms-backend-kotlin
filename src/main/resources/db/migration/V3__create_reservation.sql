CREATE TABLE reservation (
    reservation_id VARCHAR(100) PRIMARY KEY,
    inventory_id   BIGINT       NOT NULL REFERENCES inventory_item(id),
    amount         INTEGER      NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'RESERVED',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reservation_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_reservation_status CHECK (status IN ('RESERVED', 'CONFIRMED', 'CANCELLED'))
);

CREATE INDEX idx_reservation_inventory ON reservation (inventory_id);
