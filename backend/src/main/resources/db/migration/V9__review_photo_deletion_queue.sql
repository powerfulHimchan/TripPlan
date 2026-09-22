CREATE TABLE review_photo_deletions (
    id UUID PRIMARY KEY,
    stored_name VARCHAR(80) NOT NULL UNIQUE,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_review_photo_deletions_created_at
    ON review_photo_deletions(created_at);
