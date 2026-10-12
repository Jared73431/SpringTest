-- Baseline：book 表沒有任何建立或匯入資料的機制（讀的是其他模組留下的表），測試自己建立並放入範例資料
CREATE SEQUENCE book_id_seq START WITH 100;
CREATE TABLE book (
    id        INTEGER PRIMARY KEY,
    isbn      INTEGER,
    title     VARCHAR(255),
    author    VARCHAR(255),
    year      INTEGER,
    publisher VARCHAR(255),
    cost      DOUBLE PRECISION
);
INSERT INTO book VALUES
    (1, 1001, 'Spring in Action',     'Craig Walls',  2022, 'Manning',  59.0),
    (2, 1002, 'Spring Boot Up',       'Craig Walls',  2018, 'Manning',  45.0),
    (3, 1003, 'Effective Java',       'Joshua Bloch', 2018, 'Addison',  55.0),
    (4, 1004, 'Java Puzzlers',        'Joshua Bloch', 2005, 'Addison',  25.0),
    (5, 1005, 'Clean Code',           'Robert Martin', 2008, 'Prentice', 35.0),
    (6, 1006, 'Modern Java',          'Raoul Urma',   2021, 'Manning',  120.0);
