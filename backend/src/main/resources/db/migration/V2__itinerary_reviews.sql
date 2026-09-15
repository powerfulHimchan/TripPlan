CREATE TABLE itinerary_reviews (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL UNIQUE REFERENCES itinerary_items(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

