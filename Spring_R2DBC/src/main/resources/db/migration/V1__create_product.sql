-- 原本專案沒有任何 schema 檔，要在本機手動建表；改由 Flyway 建立，與原本手動建立的資料表相同
CREATE TABLE product (
    id          SERIAL PRIMARY KEY,
    description VARCHAR(255),
    price       DOUBLE PRECISION
);
