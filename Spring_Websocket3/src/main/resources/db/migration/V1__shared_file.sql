-- 分享的檔案。檔案內容存在磁碟（stored_name），這裡記錄它的資料。
-- 修正前下載時不查這張表，而是到「今天、昨天、前天」的日期目錄找，3 天前的檔案就下載不到
CREATE TABLE shared_file (
    id            UUID         PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_name   VARCHAR(64)  NOT NULL UNIQUE,
    size          BIGINT       NOT NULL,
    uploaded_by   VARCHAR(50)  NOT NULL,
    uploaded_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
