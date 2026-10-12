-- 每個測試開始前恢復成 Flyway 範例資料的狀態（內容與 V2、V3 的 INSERT 相同）
TRUNCATE book_order, book, batch_step_execution_context, batch_job_execution_context, batch_step_execution, batch_job_execution_params, batch_job_execution, batch_job_instance RESTART IDENTITY;
INSERT INTO book (isbn, title, author, published_year, publisher, price) VALUES
    ('9781617297571', 'Spring in Action',          'Craig Walls',    2022, 'Manning',       59.00),
    ('9781491983461', 'Spring Boot: Up and Running', 'Mark Heckler',  2021, 'O''Reilly',     45.00),
    ('9780134685991', 'Effective Java',            'Joshua Bloch',   2018, 'Addison-Wesley', 55.00),
    ('9780321356680', 'Java Puzzlers',             'Joshua Bloch',   2005, 'Addison-Wesley', 25.00),
    ('9780132350884', 'Clean Code',                'Robert C. Martin', 2008, 'Prentice Hall', 35.00),
    ('9781617293566', 'Modern Java in Action',     'Raoul-Gabriel Urma', 2018, 'Manning',  120.00),
    ('9781449373320', 'Designing Data-Intensive Applications', 'Martin Kleppmann', 2017, 'O''Reilly', 80.00),
    ('9780201633610', 'Design Patterns',           'Erich Gamma',    1994, 'Addison-Wesley', 65.00);
INSERT INTO book_order (book_id, quantity)
SELECT (n % 8) + 1, (n % 3) + 1 FROM generate_series(1, 10) AS n;
