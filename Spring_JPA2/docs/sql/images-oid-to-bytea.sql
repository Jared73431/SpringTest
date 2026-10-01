-- =============================================================
-- images.data：oid（Large Object）→ bytea
--
-- 背景：舊版 Image.data 使用 @Lob，Hibernate 在 PostgreSQL 會存成 oid，
--      實際的圖片內容放在 pg_largeobject。新版改為一般的 byte[]，對應 bytea。
--      spring.jpa.hibernate.ddl-auto=update 不會修改既有欄位的型別，
--      因此既有資料庫需要手動執行這份 SQL。
--
-- 執行時機：在啟動新版應用程式「之前」執行
-- 執行方式：psql -U postgres -d test -f images-oid-to-bytea.sql
-- 建議先備份：pg_dump -U postgres -d test -t images -b -Fc -f images-backup.dump
--            （使用 -t 指定資料表時，Large Object 預設不會被備份，必須加上 -b；
--             可用 pg_restore -l images-backup.dump 確認清單中有 BLOB 項目）
-- =============================================================

BEGIN;

-- 1. 新增 bytea 欄位，把 Large Object 的內容複製過去
ALTER TABLE images ADD COLUMN data_bytea bytea;
UPDATE images SET data_bytea = lo_get(data) WHERE data IS NOT NULL;

-- 2. 刪除不再使用的 Large Object（刪除資料列不會自動刪除它們，否則會一直佔用空間）
SELECT lo_unlink(data) FROM images WHERE data IS NOT NULL;

-- 3. 移除舊欄位，新欄位改名為 data
ALTER TABLE images DROP COLUMN data;
ALTER TABLE images RENAME COLUMN data_bytea TO data;

COMMIT;
