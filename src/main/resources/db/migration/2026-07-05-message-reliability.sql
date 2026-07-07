ALTER TABLE chat_message
    ADD COLUMN delivered_time BIGINT NULL COMMENT '消息送达时间' AFTER status,
    ADD COLUMN read_time BIGINT NULL COMMENT '消息已读时间' AFTER delivered_time;

ALTER TABLE chat_session_user
    ADD COLUMN unread_count INT NOT NULL DEFAULT 0 COMMENT '当前用户会话未读数' AFTER contact_name,
    ADD COLUMN last_read_message_id BIGINT NULL COMMENT '最后已读消息ID' AFTER unread_count,
    ADD COLUMN last_read_time BIGINT NULL COMMENT '最后已读时间' AFTER last_read_message_id;
