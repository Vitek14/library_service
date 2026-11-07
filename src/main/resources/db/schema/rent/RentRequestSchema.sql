CREATE TABLE IF NOT EXISTS rent_requests (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id    BIGINT      NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '1 day'),

    CONSTRAINT rent_requests_status_check
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED'))
);

-- prevents two finalized rows for the same (book, user)
CREATE UNIQUE INDEX IF NOT EXISTS rent_requests_finalized_uniq
    ON rent_requests (book_id, user_id)
    WHERE status IN ('ACCEPTED', 'REJECTED');
