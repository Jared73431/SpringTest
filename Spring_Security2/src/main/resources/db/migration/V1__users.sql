-- Spring Security 內建的使用者表（JdbcUserDetailsManager 預設使用的表名與欄位）。
-- 官方的建表 SQL 在 spring-security-core 的 org/springframework/security/core/userdetails/jdbc/users.ddl，
-- 但它是 HSQLDB 的語法（varchar_ignorecase），這裡改成 PostgreSQL。
CREATE TABLE users (
    username VARCHAR(50)  NOT NULL PRIMARY KEY,
    password VARCHAR(500) NOT NULL,
    enabled  BOOLEAN      NOT NULL
);

-- 角色存成 ROLE_ 開頭：hasRole('ADMIN') 檢查的是 ROLE_ADMIN
CREATE TABLE authorities (
    username  VARCHAR(50) NOT NULL REFERENCES users (username),
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT uk_authorities UNIQUE (username, authority)
);

-- 範例帳號，密碼都是 password（存的是 BCrypt 雜湊值；同一個密碼，每個雜湊值都不同，因為有隨機的 salt）。
-- 練習用：正式系統的帳號不會放在 migration 中
INSERT INTO users (username, password, enabled) VALUES
    ('alice', '{bcrypt}$2a$10$DPZmvZtXazgzfXjri3ac1eUi6T6y6s2KZVTxQ3EyNWPbVLcCfQkAS', TRUE),
    ('admin', '{bcrypt}$2a$10$nmiLCiueFgW9FVIp9ud0HuJfGTkYb2AbnElYUY6QWU8lU0PEuIrUS', TRUE),
    -- 停用的帳號：密碼正確也不能登入
    ('bob',   '{bcrypt}$2a$10$5NwsCDomMMYMnleFyjqx...02GNZgbA8A9uBkxjKihR08BqCbLFpi', FALSE);

INSERT INTO authorities (username, authority) VALUES
    ('alice', 'ROLE_USER'),
    ('admin', 'ROLE_USER'),
    ('admin', 'ROLE_ADMIN'),
    ('bob',   'ROLE_USER');
