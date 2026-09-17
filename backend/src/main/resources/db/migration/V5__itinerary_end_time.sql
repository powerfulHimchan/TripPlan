ALTER TABLE itinerary_items ADD COLUMN ends_at TIMESTAMPTZ;

UPDATE itinerary_items
SET ends_at = scheduled_at + INTERVAL '1 hour'
WHERE ends_at IS NULL;

ALTER TABLE itinerary_items ALTER COLUMN ends_at SET NOT NULL;

CREATE INDEX idx_items_timeline ON itinerary_items(trip_id, scheduled_at, ends_at);
