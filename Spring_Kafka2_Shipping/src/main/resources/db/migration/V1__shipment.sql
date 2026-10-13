-- 出貨：每筆訂單一筆，SHIPPED 或 REJECTED
CREATE TABLE shipment (
    order_id    UUID PRIMARY KEY,
    status      VARCHAR(20)  NOT NULL,
    reason      VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 處理過的事件：和出貨寫在同一個交易中，同一個事件送達兩次也只處理一次
CREATE TABLE processed_event (
    event_id      UUID PRIMARY KEY,
    processed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
