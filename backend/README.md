# 心晴情绪 AI 助手后端

2026-10-21：已升级为数据库可撤销Bearer会话，退出即撤销、刷新轮换，旧JWT登录状态需重新登录。使用既有 `mh_auth_session` 表，无需手工改表。新增权限、限流、文件与风险闭环，详见 [完善记录](../docs/IMPROVEMENTS_2026-10-21.md)。

本地开发版后端：Spring Boot 4 / Java 21 / MySQL 9.2 / Flyway。
已实现 `/api/v1` 业务接口（认证、日记、会话 SSE、知识库、文件、聚合看板、风险事件），
AI 咨询回复和情绪花园评分默认使用 DeepSeek API；高风险固定指引仍使用本地规则。

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

参考 `.env.example`。从 `backend` 目录启动时，应用自动读取该目录的 `.env`（Java properties 格式，值不要加引号），也支持系统、IDE 或终端环境变量覆盖。
关键变量：`DB_USERNAME`/`DB_PASSWORD`、`JWT_SECRET`、`STORAGE_DIR`、`SEED_DEMO_DATA`。

## DeepSeek 接入

1. 在 [DeepSeek 控制台](https://platform.deepseek.com/) 创建 API Key。
2. 在本地 `backend/.env` 填写 `DEEPSEEK_API_KEY=你的密钥`，不要加引号。此文件已被 Git 忽略，不要把密钥写入前端或提交仓库。
3. 从 `backend` 目录重新运行 `mvn spring-boot:run`。
4. 登录用户端，在 AI 咨询中创建会话并发送消息。

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `AI_PROVIDER` | `deepseek` | `deepseek` 调用外部 API；`local` 使用原本的本地演示规则 |
| `DEEPSEEK_API_KEY` | 空 | 后端密钥；为空时普通对话提示尚未配置 |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | 基础地址，后端自动追加 `/chat/completions`；外部地址必须使用 HTTPS |
| `DEEPSEEK_MODEL` | `deepseek-flash` | 模型名称，可按账户可用模型修改 |
| `DEEPSEEK_TIMEOUT` | `60s` | 请求超时，最大 `90s` |

接口格式和默认模型依据 [DeepSeek 官方文档](https://api-docs.deepseek.com/zh-cn/)。请求关闭思考模式，最多生成 1024 token。

前端仍调用 `/api/v1/chat/stream`，使用 `message/risk/done/error` SSE 协议。当前先等待 DeepSeek 返回完整回复，再由后端切片推送；尚未透传上游实时 token 流。只向上游发送当前会话最近 20 条消息和当前问题，不发送账户信息、其他会话或日记。

缺少密钥、认证失败、余额不足、限流和超时会返回明确错误，不自动重试付费请求，也不自动回退为普通规则回复。高风险输入沿用固定本地指引，无需外部服务。

情绪花园在每轮对话后单独调用 DeepSeek JSON Output，根据最近20条聊天消息中的用户发言评估情绪，排除助手内容，重点参考最新表达。返回0–100分（越高越积极稳定）、标签、评分依据、建议和改善行动，并保存模型来源与分析时间。高风险规则不能被模型降级。单次分析超时最多30秒；异常或非法结果记为 `FAILED`，不覆盖成固定分数、不显示上一轮旧分，但正常聊天回复仍保留。空会话显示待评分，0分可正常展示。每轮普通对话通常包含一次回复请求和一次分析请求，会额外消耗分析额度。

本机 Java 21 若出现 `Unable to establish loopback connection`，可用项目目录存放临时套接字：

```powershell
mvn spring-boot:run '-Dspring-boot.run.jvmArguments=-Djdk.net.unixdomain.tmpdir=D:/project/code/MindHaven/backend/target'
```

## 打包

```bash
mvn clean package
```

## 测试

```bash
mvn test
```
