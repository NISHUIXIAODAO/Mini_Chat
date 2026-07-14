CREATE TEMPORARY TABLE tmp_group_session_mapping AS
SELECT DISTINCT
    m.session_id AS old_session_id,
    CONCAT('G_', m.contact_id) AS new_session_id,
    m.contact_id AS group_id
FROM chat_message m
WHERE m.contact_type = 1
  AND m.session_id <> CONCAT('G_', m.contact_id)
UNION
SELECT DISTINCT
    u.session_id AS old_session_id,
    CONCAT('G_', u.contact_id) AS new_session_id,
    u.contact_id AS group_id
FROM chat_session_user u
INNER JOIN group_info g ON g.group_id = u.contact_id
WHERE u.session_id <> CONCAT('G_', u.contact_id);

INSERT INTO chat_session (session_id, last_message, last_receive_time)
SELECT
    mapping.new_session_id,
    SUBSTRING_INDEX(
        GROUP_CONCAT(s.last_message ORDER BY s.last_receive_time DESC SEPARATOR '\n'),
        '\n',
        1
    ) AS last_message,
    MAX(s.last_receive_time) AS last_receive_time
FROM chat_session s
INNER JOIN tmp_group_session_mapping mapping ON mapping.old_session_id = s.session_id
GROUP BY mapping.new_session_id
ON DUPLICATE KEY UPDATE
    last_message = VALUES(last_message),
    last_receive_time = GREATEST(chat_session.last_receive_time, VALUES(last_receive_time));

UPDATE chat_message m
INNER JOIN tmp_group_session_mapping mapping ON mapping.old_session_id = m.session_id
SET m.session_id = mapping.new_session_id
WHERE m.contact_type = 1;

UPDATE chat_message_user_status s
INNER JOIN tmp_group_session_mapping mapping ON mapping.old_session_id = s.session_id
SET s.session_id = mapping.new_session_id;

UPDATE chat_session_user u
INNER JOIN tmp_group_session_mapping mapping
    ON mapping.old_session_id = u.session_id
   AND mapping.group_id = u.contact_id
SET u.session_id = mapping.new_session_id;

DELETE s
FROM chat_session s
INNER JOIN tmp_group_session_mapping mapping ON mapping.old_session_id = s.session_id
WHERE NOT EXISTS (
    SELECT 1 FROM chat_message m WHERE m.session_id = s.session_id
)
AND NOT EXISTS (
    SELECT 1 FROM chat_session_user u WHERE u.session_id = s.session_id
)
AND NOT EXISTS (
    SELECT 1 FROM chat_message_user_status status WHERE status.session_id = s.session_id
);

DROP TEMPORARY TABLE tmp_group_session_mapping;
