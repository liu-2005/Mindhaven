# 心晴情绪 AI 助手后端

本地开发版后端：Spring Boot 4 / Java 21 / MySQL 9.2 / Flyway。
已实现 `/api/v1` 业务接口（认证、日记、会话 SSE、知识库、文件、聚合看板、风险事件），
AI 回复使用内置本地规则式适配器（`local-rule-v1`），无需外部 API Key 即可联调。

## 环境要求

- Java 21
- Maven 3.6.3+
- MySQL 8+（本机 9.2 已验证）

## 初始化 MySQL

1. 使用具备建库权限的 root 账号执行建库与建账号（一次性）：

```bash
mysql -u root -p < database/init.sql
```

2. 创建专用应用账号并授权（按需修改密码）：

```sql
CREATE USER IF NOT EXISTS 'mindhaven'@'localhost' IDENTIFIED BY 'mindhaven2026';
CREATE USER IF NOT EXISTS 'mindhaven'@'127.0.0.1' IDENTIFIED BY 'mindhaven2026';
GRANT ALL PRIVILEGES ON mind_haven.* TO 'mindhaven'@'localhost';
GRANT ALL PRIVILEGES ON mind_haven.* TO 'mindhaven'@'127.0.0.1';
FLUSH PRIVILEGES;
```

3. 启动应用，Flyway 自动执行 `src/main/resources/db/migration` 中尚未执行的迁移。

数据库文件的职责、表清单和验证方法见 [database/README.md](./database/README.md)。
生产环境使用 `JPA_DDL_AUTO=validate`，表结构只能通过新增 Flyway 迁移修改。

## 运行

```bash
mvn spring-boot:run
```

服务端口默认 `1235`，健康检查：`GET /api/v1/health`。
默认激活 `local` profile，会在首次启动时写入演示数据。

### 演示账号（local profile 自动创建）

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | USER + CONTENT_ADMIN + SAFETY_ADMIN + USER_ADMIN |
| `demo` | `demo123` | USER |

## 配置

参考 `.env.example`。Spring Boot 不自动读取 `.env` 文件，请将变量配置到系统、IDE 启动配置或终端环境。
关键变量：`DB_USERNAME`/`DB_PASSWORD`、`JWT_SECRET`、`STORAGE_DIR`、`SEED_DEMO_DATA`。

## 打包

```bash
mvn clean package
```

## 测试

```bash
mvn test
```
