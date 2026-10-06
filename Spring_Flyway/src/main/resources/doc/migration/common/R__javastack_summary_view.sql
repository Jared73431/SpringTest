-- 可重複遷移（Repeatable）的正確用法：建立 view、function 這類「重新執行一次也不會出錯」的物件
--
-- 1. 每次啟動時，Flyway 比對這個檔案的 checksum；內容改變了才會重新執行
-- 2. 在所有版本遷移（V）之後執行；有多個 R__ 時依描述（檔名）的字母順序執行
-- 3. CREATE OR REPLACE：重新執行時直接以新定義取代舊的 view，結果與第一次執行相同（冪等）
--
-- 對照 R__update_javastack.sql：用 R__ 更新資料不建議，每次修改都會再更新一次資料，
-- 而且會覆蓋 V1_0_5 Java 遷移計算出的結果（說明見 readme）
CREATE OR REPLACE VIEW v_javastack_summary AS
SELECT note,
       COUNT(*)  AS total,
       MAX(time) AS last_updated
FROM t_javastack
GROUP BY note;
