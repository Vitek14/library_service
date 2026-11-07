-- Mock data for development. Safe to run multiple times.

-- Authors
INSERT INTO authors (name) SELECT 'Leo Tolstoy'
    WHERE NOT EXISTS (SELECT 1 FROM authors WHERE name = 'Leo Tolstoy');
INSERT INTO authors (name) SELECT 'George Orwell'
    WHERE NOT EXISTS (SELECT 1 FROM authors WHERE name = 'George Orwell');

-- Genres
INSERT INTO genres (name) SELECT 'Novel'
    WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Novel');
INSERT INTO genres (name) SELECT 'Dystopia'
    WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Dystopia');

-- Books
INSERT INTO books (title) SELECT 'War and Peace'
    WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = 'War and Peace');
INSERT INTO books (title) SELECT '1984'
    WHERE NOT EXISTS (SELECT 1 FROM books WHERE title = '1984');

-- Links: book <-> author (1 author per book)
INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id FROM books b, authors a
WHERE b.title = 'War and Peace' AND a.name = 'Leo Tolstoy'
  AND NOT EXISTS (SELECT 1 FROM book_authors ba WHERE ba.book_id = b.id AND ba.author_id = a.id);

INSERT INTO book_authors (book_id, author_id)
SELECT b.id, a.id FROM books b, authors a
WHERE b.title = '1984' AND a.name = 'George Orwell'
  AND NOT EXISTS (SELECT 1 FROM book_authors ba WHERE ba.book_id = b.id AND ba.author_id = a.id);

-- Links: book <-> genre (1 genre per book)
INSERT INTO book_genres (book_id, genre_id)
SELECT b.id, g.id FROM books b, genres g
WHERE b.title = 'War and Peace' AND g.name = 'Novel'
  AND NOT EXISTS (SELECT 1 FROM book_genres bg WHERE bg.book_id = b.id AND bg.genre_id = g.id);

INSERT INTO book_genres (book_id, genre_id)
SELECT b.id, g.id FROM books b, genres g
WHERE b.title = '1984' AND g.name = 'Dystopia'
  AND NOT EXISTS (SELECT 1 FROM book_genres bg WHERE bg.book_id = b.id AND bg.genre_id = g.id);
