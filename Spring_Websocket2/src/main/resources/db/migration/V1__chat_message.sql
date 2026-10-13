-- 聊天室的訊息（私訊不存）。修正前由 JPA 的 ddl-auto=update 建立
CREATE TABLE chat_message (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room    VARCHAR(50)   NOT NULL,
    sender  VARCHAR(50)   NOT NULL,
    content VARCHAR(500)  NOT NULL,
    sent_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- 查詢某個聊天室最近的訊息
CREATE INDEX idx_chat_message_room_id ON chat_message (room, id);
