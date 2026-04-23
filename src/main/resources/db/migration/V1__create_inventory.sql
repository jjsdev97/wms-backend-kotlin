CREATE TABLE inventory_item (
    id           BIGSERIAL    PRIMARY KEY,
    sku          VARCHAR(100) NOT NULL,
    warehouse_id VARCHAR(50)  NOT NULL,
    quantity     INTEGER      NOT NULL DEFAULT 0,
    version      BIGINT       NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_inventory_sku_warehouse UNIQUE (sku, warehouse_id),
    CONSTRAINT chk_quantity_non_negative  CHECK (quantity >= 0)
);

INSERT INTO inventory_item (sku, warehouse_id, quantity) VALUES
    ('SKU-001', 'WH-A', 100),
    ('SKU-002', 'WH-A', 250),
    ('SKU-003', 'WH-B', 50);
