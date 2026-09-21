ALTER TABLE itinerary_items
    ADD COLUMN cost_won BIGINT NOT NULL DEFAULT 0;

ALTER TABLE itinerary_items
    ADD CONSTRAINT chk_itinerary_cost_non_negative CHECK (cost_won >= 0);
