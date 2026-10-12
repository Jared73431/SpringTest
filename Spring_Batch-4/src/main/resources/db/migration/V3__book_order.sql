-- 訂單：出貨批次（orderShippingJob）只處理 status = 'PENDING' 的訂單，處理後改成 'SHIPPED'
CREATE TABLE book_order (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id    BIGINT      NOT NULL REFERENCES book (id),
    quantity   INTEGER     NOT NULL CHECK (quantity > 0),
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ordered_at TIMESTAMP   NOT NULL DEFAULT now(),
    shipped_at TIMESTAMP
);

-- 批次每次都用 status 篩選：沒有索引時，訂單越多越慢（全表掃描）
CREATE INDEX idx_book_order_status ON book_order (status);

-- 範例資料：10 筆待出貨的訂單
INSERT INTO book_order (book_id, quantity)
SELECT (n % 8) + 1, (n % 3) + 1 FROM generate_series(1, 10) AS n;
