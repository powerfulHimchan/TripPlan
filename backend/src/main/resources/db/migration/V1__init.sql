CREATE TABLE trips (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    title VARCHAR(120) NOT NULL,
    destination VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    timezone VARCHAR(60) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_trips_user_id ON trips(user_id);

CREATE TABLE itinerary_items (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    place VARCHAR(200),
    memo TEXT,
    scheduled_at TIMESTAMPTZ NOT NULL,
    notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notification_minutes_before INTEGER NOT NULL DEFAULT 0,
    notification_sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_items_trip_time ON itinerary_items(trip_id, scheduled_at);
CREATE INDEX idx_items_notification_due ON itinerary_items(notification_enabled, notification_sent_at, scheduled_at);

CREATE TABLE device_tokens (
    token VARCHAR(512) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);

CREATE TABLE trip_reviews (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL UNIQUE REFERENCES trips(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

