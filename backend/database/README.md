# MySQL 数据库说明

## 文件职责

| 文件 | 用途 |
| --- | --- |
| `init.sql` | 创建 `mind_haven` 数据库，不创建业务账户、不写默认密码 |
| `../src/main/resources/db/migration/V1__baseline_schema.sql` | 创建首版业务表、约束和索引 |
| `../src/main/resources/db/migration/V2__seed_reference_data.sql` | 初始化角色、权限与知识分类 |
| `verify.sql` | 只读检查表、迁移记录和基础种子数据 |

## 初始化

```bash
mysql -u root -p < database/init.sql
mvn spring-boot:run
mysql -u root -p mind_haven < database/verify.sql
```

应用启动时 Flyway 自动创建 `flyway_schema_history` 并按版本执行迁移。数据库连接账户需要对 `mind_haven` 具备建表、改表和数据读写权限，但生产环境不建议直接使用 MySQL `root`。

## 迁移规则

1. 已在共享环境执行过的 `V1`、`V2` 文件不可修改。
2. 后续变更新增 `V3__description.sql`，不要依赖 Hibernate 自动改表。
3. 迁移先在空库和现有数据副本上验证，再进入生产环境。
4. 生产环境禁用 Flyway `clean`，执行迁移前先完成备份与回滚评审。
5. 不在 SQL 文件中提交真实密码、令牌、邮箱、手机号或对话/日记内容。

## 基础表分组

- 身份权限：`mh_user`、`mh_role`、`mh_permission`、`mh_user_role`、`mh_role_permission`、`mh_auth_session`
- 情绪日记：`mh_emotion_diary`、`mh_emotion_analysis`
- AI 会话：`mh_chat_session`、`mh_chat_message`
- 知识内容：`mh_knowledge_category`、`mh_article`、`mh_tag`、`mh_article_tag`
- 文件安全：`mh_file_object`
- 心理安全与审计：`mh_risk_event`、`mh_audit_log`

种子数据不会创建管理员账号。管理员应通过受控的部署命令或一次性初始化流程创建，并使用强密码哈希。
