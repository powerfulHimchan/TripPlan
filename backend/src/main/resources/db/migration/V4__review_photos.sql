CREATE TABLE review_photos (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES itinerary_reviews(id) ON DELETE CASCADE,
    stored_name VARCHAR(80) NOT NULL UNIQUE,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(80) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_review_photos_review ON review_photos(review_id, created_at);
