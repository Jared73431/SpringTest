-- ShedLock 的鎖：每個排程一列，記錄目前由誰持有、持有到什麼時候
CREATE TABLE shedlock (
    name       VARCHAR(64)  PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);

-- 排程要清理的資料：登入 session，過期後由每天的排程刪除
CREATE TABLE user_session (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    expires_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_user_session_expires_at ON user_session (expires_at);
