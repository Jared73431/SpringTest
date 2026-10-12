-- 書籍。修正前的 book 表沒有任何建立機制，讀的是其他模組留下的表；Entity 的欄位叫 Id、ISBN（大寫）
CREATE TABLE book (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    isbn           VARCHAR(13)    NOT NULL UNIQUE,
    title          VARCHAR(200)   NOT NULL,
    author         VARCHAR(100)   NOT NULL,
    published_year INTEGER        NOT NULL,
    publisher      VARCHAR(100)   NOT NULL,
    price          NUMERIC(10, 2) NOT NULL
);

-- 範例資料（練習用；正式專案的資料不會放在 migration 中）
INSERT INTO book (isbn, title, author, published_year, publisher, price) VALUES
    ('9781617297571', 'Spring in Action',          'Craig Walls',    2022, 'Manning',       59.00),
    ('9781491983461', 'Spring Boot: Up and Running', 'Mark Heckler',  2021, 'O''Reilly',     45.00),
    ('9780134685991', 'Effective Java',            'Joshua Bloch',   2018, 'Addison-Wesley', 55.00),
    ('9780321356680', 'Java Puzzlers',             'Joshua Bloch',   2005, 'Addison-Wesley', 25.00),
    ('9780132350884', 'Clean Code',                'Robert C. Martin', 2008, 'Prentice Hall', 35.00),
    ('9781617293566', 'Modern Java in Action',     'Raoul-Gabriel Urma', 2018, 'Manning',  120.00),
    ('9781449373320', 'Designing Data-Intensive Applications', 'Martin Kleppmann', 2017, 'O''Reilly', 80.00),
    ('9780201633610', 'Design Patterns',           'Erich Gamma',    1994, 'Addison-Wesley', 65.00);
