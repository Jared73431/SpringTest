-- 匯入的目標表。修正前由 JPA 的 ddl-auto=update 依照 Entity 自動建立，欄位長度、索引都無法控制，也沒有變更紀錄
CREATE TABLE person (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL
);
