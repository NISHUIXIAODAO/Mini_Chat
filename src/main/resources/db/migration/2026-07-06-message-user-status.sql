CREATE TABLE IF NOT EXISTS chat_message_user_status (
    message_id BIGINT NOT NULL COMMENT '消息ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id INT NOT NULL COMMENT '接收用户ID',
    status INT NOT NULL DEFAULT 1 COMMENT '状态：1-已发送，2-已送达，3-已读',
    delivered_time BIGINT NULL COMMENT '送达时间',
    read_time BIGINT NULL COMMENT '已读时间',
    PRIMARY KEY (message_id, user_id),
    KEY idx_session_user_message (session_id, user_id, message_id)
) COMMENT='消息接收用户状态表';

INSERT INTO chat_message_user_status (message_id, session_id, user_id, status, delivered_time, read_time)
SELECT
    m.message_id,
    m.session_id,
    u.user_id,
    m.status,
    m.delivered_time,
    m.read_time
FROM chat_message m
INNER JOIN chat_session_user u ON u.session_id = m.session_id
WHERE u.user_id <> m.send_user_id
ON DUPLICATE KEY UPDATE
    session_id = VALUES(session_id),
    status = GREATEST(chat_message_user_status.status, VALUES(status)),
    delivered_time = COALESCE(chat_message_user_status.delivered_time, VALUES(delivered_time)),
    read_time = COALESCE(chat_message_user_status.read_time, VALUES(read_time));
