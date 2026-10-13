CREATE TABLE book (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    author      VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO book (title, author) VALUES
    ('Effective Java', 'Joshua Bloch'),
    ('Spring in Action', 'Craig Walls');
