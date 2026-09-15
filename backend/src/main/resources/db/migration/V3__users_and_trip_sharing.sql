CREATE TABLE app_users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE trip_members (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_trip_member UNIQUE (trip_id, user_id)
);

CREATE INDEX idx_trip_members_user ON trip_members(user_id);

CREATE TABLE trip_invitations (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    inviter_id VARCHAR(36) NOT NULL REFERENCES app_users(id),
    invitee_id VARCHAR(36) NOT NULL REFERENCES app_users(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')),
    created_at TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ
);

CREATE INDEX idx_invitations_invitee_status ON trip_invitations(invitee_id, status, created_at);
CREATE UNIQUE INDEX uk_pending_trip_invitation
    ON trip_invitations(trip_id, invitee_id) WHERE status = 'PENDING';

