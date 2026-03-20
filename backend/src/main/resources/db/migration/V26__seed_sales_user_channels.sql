-- V26: 預設業務帳號 sales 綁定 12 條通路（與 UserService.ALLOWED_CHANNELS / 前端 CHANNELS 一致）

DELETE FROM sales_channels_users
WHERE user_id IN (SELECT id FROM users WHERE username = 'sales');

INSERT INTO sales_channels_users (user_id, channel)
SELECT u.id, t.channel
FROM users u
CROSS JOIN (
    SELECT 'PX + 大全聯' AS channel
    UNION ALL SELECT '家樂福'
    UNION ALL SELECT '愛買'
    UNION ALL SELECT '7-11'
    UNION ALL SELECT '全家'
    UNION ALL SELECT 'Ok+萊爾富'
    UNION ALL SELECT '好市多'
    UNION ALL SELECT '楓康'
    UNION ALL SELECT '美聯社'
    UNION ALL SELECT '康是美'
    UNION ALL SELECT '電商'
    UNION ALL SELECT '市面經銷'
) t
WHERE u.username = 'sales';
