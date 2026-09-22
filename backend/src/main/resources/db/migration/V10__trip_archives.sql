CREATE TABLE trip_archives (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL,
    archived_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_trip_archive UNIQUE (trip_id, user_id)
);

CREATE INDEX idx_trip_archives_user ON trip_archives(user_id, archived_at DESC);
