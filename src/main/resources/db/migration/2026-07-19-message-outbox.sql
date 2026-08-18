CREATE TABLE IF NOT EXISTS message_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id VARCHAR(64) NOT NULL COMMENT '事件ID',
    aggregate_id BIGINT NOT NULL COMMENT '聊天消息ID',
    event_type VARCHAR(32) NOT NULL COMMENT '事件类型',
    payload LONGTEXT NOT NULL COMMENT '事件JSON',
    status VARCHAR(16) NOT NULL COMMENT 'PENDING/PROCESSING/PUBLISHED/FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '失败重试次数',
    next_retry_at BIGINT NOT NULL COMMENT '下次可投递时间',
    lease_until BIGINT NULL COMMENT '发布租约截止时间',
    published_at BIGINT NULL COMMENT 'Kafka确认时间',
    last_error VARCHAR(1000) NULL COMMENT '最近失败摘要',
    trace_id VARCHAR(64) NOT NULL COMMENT '链路追踪ID',
    create_time BIGINT NOT NULL COMMENT '创建时间',
    update_time BIGINT NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_outbox_event_id (event_id),
    KEY idx_message_outbox_scan (status, next_retry_at, lease_until)
) COMMENT='聊天消息可靠投递Outbox';
