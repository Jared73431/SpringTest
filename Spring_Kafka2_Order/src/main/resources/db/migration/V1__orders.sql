-- 訂單：建立時是 PENDING，收到 Shipping 服務回覆的事件後變成 SHIPPED 或 REJECTED
CREATE TABLE orders (
    id          UUID PRIMARY KEY,
    customer    VARCHAR(100) NOT NULL,
    item        VARCHAR(100) NOT NULL,
    quantity    INT          NOT NULL CHECK (quantity > 0),
    status      VARCHAR(20)  NOT NULL,
    reason      VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 處理過的事件：和訂單的更新寫在同一個交易中，同一個事件送達兩次也只處理一次
CREATE TABLE processed_event (
    event_id      UUID PRIMARY KEY,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
