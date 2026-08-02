-- Stable reference data only. No user or administrator account is created here.

INSERT INTO mh_role (id, code, name, description, built_in) VALUES
    (1, 'USER', '普通用户', '管理自己的日记、会话和个人资料', 1),
    (2, 'CONTENT_ADMIN', '内容管理员', '维护知识分类、文章和文章文件', 1),
    (3, 'SAFETY_ADMIN', '安全运营管理员', '按最小权限处理风险事件和查看安全指标', 1),
    (4, 'USER_ADMIN', '用户管理员', '管理账户状态和角色授权', 1);

INSERT INTO mh_permission (id, code, name, description) VALUES
    (1,  'user:self:read',      '查看个人资料',       '查看当前登录用户的个人资料'),
    (2,  'user:self:write',     '修改个人资料',       '修改当前登录用户允许变更的字段'),
    (3,  'diary:self:read',     '查看个人日记',       '查看自己的日记和趋势'),
    (4,  'diary:self:write',    '管理个人日记',       '新建、修改和删除自己的日记'),
    (5,  'chat:self:read',      '查看个人会话',       '查看自己的会话和消息'),
    (6,  'chat:self:write',     '管理个人会话',       '创建会话、发送消息和删除自己的会话'),
    (7,  'article:manage',      '管理文章',           '创建和编辑知识分类、文章与标签'),
    (8,  'article:publish',     '发布文章',           '发布、下线和归档知识文章'),
    (9,  'file:manage',         '管理业务文件',       '上传、绑定和删除有权管理的业务文件'),
    (10, 'analytics:read',      '查看聚合指标',       '查看不包含敏感正文的聚合运营指标'),
    (11, 'risk:read',           '查看风险事件',       '查看风险事件脱敏列表'),
    (12, 'risk:read-sensitive', '查看风险敏感详情',   '在记录理由和审计后查看授权的敏感详情'),
    (13, 'risk:resolve',        '处置风险事件',       '接单、解决或标记风险事件误报'),
    (14, 'user:read',           '查看用户',           '查看脱敏用户列表与状态'),
    (15, 'user:manage',         '管理用户',           '修改账户状态和受控角色授权'),
    (16, 'audit:read',          '查看审计日志',       '查询授权范围内的安全审计记录');

INSERT INTO mh_role_permission (role_id, permission_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
    (2, 7), (2, 8), (2, 9), (2, 10),
    (3, 10), (3, 11), (3, 12), (3, 13), (3, 16),
    (4, 10), (4, 14), (4, 15), (4, 16);

INSERT INTO mh_knowledge_category (id, parent_id, name, slug, sort_order, status) VALUES
    (1, NULL, '情绪管理', 'emotion-management', 10, 'ACTIVE'),
    (2, NULL, '焦虑与压力', 'anxiety-and-stress', 20, 'ACTIVE'),
    (3, NULL, '睡眠健康', 'sleep-health', 30, 'ACTIVE'),
    (4, NULL, '人际关系', 'relationships', 40, 'ACTIVE'),
    (5, NULL, '自我成长', 'self-growth', 50, 'ACTIVE'),
    (6, NULL, '心理科普', 'mental-health-basics', 60, 'ACTIVE');

INSERT INTO mh_tag (id, name, slug) VALUES
    (1,  '情绪管理', 'emotion-management'),
    (2,  '焦虑', 'anxiety'),
    (3,  '抑郁', 'depression'),
    (4,  '压力', 'stress'),
    (5,  '睡眠', 'sleep'),
    (6,  '冥想', 'meditation'),
    (7,  '正念', 'mindfulness'),
    (8,  '放松', 'relaxation'),
    (9,  '心理健康', 'mental-health'),
    (10, '自我成长', 'self-growth'),
    (11, '人际关系', 'relationships'),
    (12, '工作压力', 'work-stress'),
    (13, '学习方法', 'learning-methods'),
    (14, '生活技巧', 'life-skills');
