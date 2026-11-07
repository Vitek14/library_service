CREATE TABLE IF NOT EXISTS book_genres (
    book_id  BIGINT NOT NULL REFERENCES books(id)  ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, genre_id)
);
