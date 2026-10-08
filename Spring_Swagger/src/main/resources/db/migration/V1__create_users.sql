-- 修正前由 Hibernate ddl-auto: update 自動建立；改由 Flyway 管理，欄位長度與限制和 API 的驗證一致
CREATE TABLE users (
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(255) NOT NULL UNIQUE,
    age      INTEGER      NOT NULL CHECK (age BETWEEN 0 AND 150)
);
