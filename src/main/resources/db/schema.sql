-- users
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    surname       VARCHAR(100)  NOT NULL,
    login         VARCHAR(100)  NOT NULL UNIQUE,  -- TODO: make check for this
    password_hash VARCHAR(255)  NOT NULL,
    role          VARCHAR(20)   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- reference tables(справочники крч)
-- authors
CREATE TABLE IF NOT EXISTS authors (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE
);

-- genres
CREATE TABLE IF NOT EXISTS genres (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);


-- books
CREATE TABLE IF NOT EXISTS books (
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS book_authors (
    book_id   BIGINT NOT NULL REFERENCES books(id)   ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES authors(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);

CREATE TABLE IF NOT EXISTS book_genres (
    book_id  BIGINT NOT NULL REFERENCES books(id)  ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, genre_id)
);


-- rent requests
CREATE TABLE IF NOT EXISTS rent_requests (
    id           BIGSERIAL PRIMARY KEY,
    book_id      BIGINT      NOT NULL REFERENCES books(id),
    user_id      BIGINT      NOT NULL REFERENCES users(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at  TIMESTAMPTZ,
    declined_at  TIMESTAMPTZ,
    expires_at   TIMESTAMPTZ
);


-- indexes
CREATE INDEX IF NOT EXISTS idx_rent_requests_user   ON rent_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_rent_requests_book   ON rent_requests(book_id);
CREATE INDEX IF NOT EXISTS idx_rent_requests_status ON rent_requests(status);

-- unique indexes
-- no more than one rent request on the book from one user
CREATE UNIQUE INDEX IF NOT EXISTS uq_rent_active
    ON rent_requests(book_id, user_id)
    WHERE status IN ('PENDING', 'APPROVED');
