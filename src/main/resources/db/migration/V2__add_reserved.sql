ALTER TABLE inventory_item
    ADD COLUMN reserved INTEGER NOT NULL DEFAULT 0;

ALTER TABLE inventory_item
    ADD CONSTRAINT chk_reserved_non_negative CHECK (reserved >= 0),
    ADD CONSTRAINT chk_reserved_le_quantity  CHECK (reserved <= quantity);
