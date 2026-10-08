-- 課程使用（見 readme）：分類的主鍵是程式自己指定的代碼（第 4 課）、狀態以一個字元的代碼儲存（第 5 課）、
-- 商品屬於某個分類（第 7 課）
CREATE TABLE category (
    code    VARCHAR(20) PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    status  CHAR(1) NOT NULL,
    version INTEGER
);

ALTER TABLE product ADD COLUMN category_code VARCHAR(20) REFERENCES category (code);
