-- Read-only smoke checks for an initialized MindHaven database.

USE mind_haven;

SELECT version, description, type, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT table_name, engine, table_collation
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name LIKE 'mh\_%'
ORDER BY table_name;

SELECT 'roles' AS item, COUNT(*) AS row_count FROM mh_role
UNION ALL
SELECT 'permissions', COUNT(*) FROM mh_permission
UNION ALL
SELECT 'categories', COUNT(*) FROM mh_knowledge_category;
