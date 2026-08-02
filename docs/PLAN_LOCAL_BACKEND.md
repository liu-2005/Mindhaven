# 本地后端 API 与数据库搭建计划

> 版本：v0.1 · 2026-08-01
> 目标：用本机运行的 MySQL + 本地 Spring Boot 后端替换原远程后端/数据库，使前端可在本地完整联调，并按 `docs/API.md` 的 `/api/v1` 目标契约统一前后端。
> 已确认决策：① 实现 `/api/v1` 目标契约并同步改造前端；② AI 会话使用内置本地规则式回复；③ 由用户提供 MySQL root 密码做数据库初始化；④ 基于现有 Spring Boot 4 / Java 21 / Flyway 骨架扩展。

## 1. 背景与现状

### 1.1 仓库事实基线

- `frontend/`：Vue 3 + Vite + Element Plus 原型，页面与 API 调用层齐全，但调用的是**旧版接口**（`/user/login`、`/psychological-chat/stream` 等），且存在大量多字段、多路径兜底和调试日志。
- `backend/`：Spring Boot 4.1.0 / Java 21 / Maven，仅实现 `GET /api/health`；已有 `ApiResponse`、CORS、Flyway 迁移 `V1`（17 张表）+ `V2`（角色/权限/分类/标签种子）。
- `docs/`：已定义目标契约 `API.md`（`/api/v1`，统一响应 `{code,message,data,requestId}`、分页 `{items,page,size,total,totalPages}`）与数据模型 `DATA_MODEL.md`。
- 本机：Java 21、Maven 3.9.14、Node 24 已就绪；MySQL 9.2 服务 `MYSQL92_1` 已运行在 `3306` 端口。

### 1.2 现状与目标契约的差距（关键债务）

| 项 | 前端现状 | 目标 `/api/v1` |
| --- | --- | --- |
| 认证 | 自定义 `token` 头 + localStorage，登录页可自选 `userType` | `Authorization: Bearer`，角色由服务端签发 |
| 响应体 | `code` 兼容 `0/200/success`，无 requestId | `{code:"OK", message, data, requestId}` |
| 分页 | `current/currentPage/pageNum` + `size/pageSize`，`records/list/rows` | `page/size` + `items` |
| ID | 会话出现 `session_` 前缀与数字混用 | 统一字符串 ID，无前缀 |
| 文章状态 | 裸数字 `0/1/2` | `DRAFT/PUBLISHED/OFFLINE/ARCHIVED` |
| SSE | 客户端猜测 `content/text/delta/...` | 固定 `message/risk/done/error` 事件 |
| 看板 | 浏览器拉取 500 条估算 | 后端聚合接口 |

## 2. 目标架构

```text
Browser (Vue3) --/api/v1 REST + SSE--> Spring Boot (localhost:1235) --> MySQL 9.2 (localhost:3306/mind_haven)
                                        |-- RuleBased AiAdapter (本地规则式 AI，无外部依赖)
                                        |-- 本地文件存储 backend/storage/
```

- 认证：JWT（HMAC-SHA256 签名）`Authorization: Bearer`；密码 BCrypt 哈希；`local` profile 内置演示账号。
- 会话令牌采用短期访问令牌（本计划不实现刷新令牌轮换，`/auth/refresh` 置为占位，前端不依赖）。
- 数据库沿用现有 `V1/V2` 迁移，`ddl-auto=validate`，业务变更只通过新增 Flyway 迁移。
- AI 通过 `AiAdapter` 接口隔离；默认 `LocalRuleBasedAiAdapter`（关键词情绪/风险识别 + 固定安全指引 + 模板化流式回复）。

## 3. 数据库计划

### 3.1 初始化步骤（需用户提供 root 密码）

1. 用户提供 MySQL root 密码（设为本机环境变量，不写入仓库）。
2. 执行 `backend/database/init.sql` 创建 `mind_haven` 库（utf8mb4）。
3. 创建专用应用账号（示例，最终口令由用户设定或生成）：

```sql
CREATE USER 'mindhaven'@'localhost' IDENTIFIED BY '<app-password>';
GRANT ALL PRIVILEGES ON mind_haven.* TO 'mindhaven'@'localhost';
FLUSH PRIVILEGES;
```

4. 启动后端，Flyway 自动执行 `V1`/`V2` 建表与种子。

### 3.2 迁移策略

- 不修改已执行的 `V1/V2`。
- 若需补充索引或业务字段，新增 `V3__xxx.sql`。
- 演示数据（管理员账号、示例文章）**不写入迁移**，由 `local` profile 的启动数据初始化器写入，避免污染生产路径。
- 验证：`mysql -u mindhaven -p mind_haven < backend/database/verify.sql` 应输出 17 张业务表与角色/权限/分类计数。

## 4. 后端实施计划（`/api/v1`，基于现有骨架）

### 4.1 通用层（common）

- 统一响应升级为 `{code, message, data, requestId}`（`code="OK"` 成功，其余为业务码），`requestId` 由 `MDC`/过滤器生成并贯穿日志。
- 全局异常处理器：参数校验 `VALIDATION_ERROR`(400)、未认证 `UNAUTHENTICATED`(401)、越权 `FORBIDDEN`(403)、不存在 `RESOURCE_NOT_FOUND`(404)、冲突 `RESOURCE_CONFLICT`(409)、限流 `RATE_LIMITED`(429)。不向客户端返回堆栈/SQL。
- 分页统一：请求 `page`(从 1)/`size`(默认 20，最大 100)；响应 `{items,page,size,total,totalPages}`。

### 4.2 认证与用户（auth/user）

- 依赖：`spring-security-crypto`（仅 BCrypt 编码器）或等效密码哈希实现；自研 JWT 工具 + `HandlerInterceptor` 做认证/授权（不引入完整 Spring Security，控制复杂度）。
- 接口：`POST /auth/register`（忽略客户端角色）、`POST /auth/login`、`POST /auth/logout`（前端清理本地凭证）、`POST /auth/refresh`（占位/受限）、`GET /users/me`、`PATCH /users/me`、`GET /admin/users`（脱敏分页）。
- 角色校验：`USER` 为默认注册角色；管理员账户由 `local` 数据初始化器创建并绑定 `USER_ADMIN/CONTENT_ADMIN/SAFETY_ADMIN`。
- 权限表达用资源动作（`article:publish` 等），拦截器按 `user → roles → permissions` 判权；对象级所有权校验在每个 Service 内完成（日记/会话/消息/文件）。

### 4.3 情绪日记（diary）

- 接口：`POST /diaries`、`GET /diaries`、`GET /diaries/{id}`、`PUT /diaries/{id}`、`DELETE /diaries/{id}`、`GET /diaries/trends`、`GET /admin/diaries`（脱敏分页，支持 `userId`/评分范围筛选）、`DELETE /admin/diaries/{id}`（审计）。
- 约束：`moodScore 1-10`、`sleepQuality/stressLevel 1-5`；同用户同一天默认一篇，重复返回 409 并带 `existingDiaryId`。
- 前端字段 `diaryContent` 与表字段 `content` 由 DTO 层映射（当前 `Diary.vue` 提交 `diaryContent`，需与前端迁移一并收敛为 `content`）。

### 4.4 AI 会话（chat）

- 接口：`POST/GET /chat/sessions`、`GET/PATCH/DELETE /chat/sessions/{id}`、`GET /chat/sessions/{id}/messages`（分页）、`GET /chat/sessions/{id}/emotion`、`POST /chat/stream`（SSE）。
- 会话/消息落到 `mh_chat_session`/`mh_chat_message`；消息幂等键 `clientRequestId` 命中唯一约束去重。
- SSE 固定协议：`event: message`(delta)、`event: risk`(level/guidanceCode)、`event: done`(messageId/finishReason)、`event: error`；关闭代理缓冲，定期心跳。
- 情绪分析：调用 `AiAdapter` 生成 `mh_emotion_analysis`（`source_type=CHAT_SESSION`），保留 `model_version="local-rule-v1"` 与 `policy_version`。
- 会话标题：首轮后按规则生成，允许 `PATCH` 重命名。

### 4.5 知识库（knowledge）

- 公开：`GET /knowledge/categories`（树）、`GET /knowledge/articles`（仅 `PUBLISHED`，支持 `keyword/categoryId/page/size`）、`GET /knowledge/articles/{id}`（仅已发布，或权限允许的草稿）。
- 管理：`POST/PUT /admin/knowledge/articles`、`PUT /admin/knowledge/articles/{id}/status`（`DRAFT→PUBLISHED→OFFLINE→ARCHIVED` 状态机）、`DELETE`。
- 正文：写入前做 HTML 白名单清洗（本地实现基础清洗，详见风险）；`contentHtml` 返回已清洗内容。
- 查询返回供前端展示的聚合字段：`categoryName`、`authorName`、`viewCount/readCount`、`coverUrl`、`publishTime`。

### 4.6 文件（file）

- 接口：`POST /files`（multipart：`file/businessType/businessId/businessField`，校验大小/类型/文件头，服务端生成存储键，落 `mh_file_object`，物理文件存 `backend/storage/`）、`GET /files/{fileId}`（鉴权下载）、`DELETE /files/{fileId}`。
- 返回 `{fileId, filePath/url}`；前端编辑器插入 `filePath`（以 `/files/{id}` 形式回填 `coverUrl`）。

### 4.7 本地规则式 AI 适配器（AiAdapter）

- 接口 `AiAdapter`：`streamChat(...)`、`analyzeEmotion(...)`、`checkRisk(...)`。
- `LocalRuleBasedAiAdapter`：
  - 流式回复：基于关键词模板组织中文句子，按词/字符切片 SSE 输出，可仿真打字效果。
  - 情绪分析：关键词词典映射 `emotionScore/label/isNegative`，输出 `suggestion`、`improvementSuggestions`、`keywords`。
  - 风险识别：固定危机关键词规则 → `risk_level`（`NORMAL/CONCERN/WARNING/CRISIS`），CRISIS 返回本地固定安全指引（`guidanceCode`），不依赖模型可用性。
  - `model_version="local-rule-v1"`，提示词/规则版本写入 `policy_version`，便于回溯。
- 后续接真实模型：新增实现类即可，不改前端协议。

### 4.8 管理端聚合与风险（analytics/risk/audit）

- `GET /admin/analytics/overview`：总用户、日记、会话、平均情绪分等汇总。
- `GET /admin/analytics/trends`：按 `from/to/granularity` 聚合（替代前端拉 500 条估算）。
- `GET /admin/risk-events`（脱敏分页）、`GET /admin/risk-events/{id}`（敏感详情）、`PUT /admin/risk-events/{id}/status`（状态机）。
- `mh_audit_log` 写入：管理员敏感查看、文章发布/删除、风险处置、删除日记等。

## 5. 前端迁移计划（对齐 `/api/v1`）

1. `src/utils/request.js`：`baseURL` 改为 `/api/v1`；认证头改 `Authorization: Bearer`；成功判定 `code==="OK"`；保留 `silent` 能力；移除 401 时仅清 token 的旧逻辑（改为跳登录并清 userInfo）。
2. `src/api/*`：
   - `user.js` → `/auth/login`、`/auth/register`、`/auth/logout`、`/users/me`、`/admin/users`。
   - `emotionDiary.js` → `/diaries`、`/diaries/{id}`、`/diaries/trends`、`/admin/diaries`。
   - `consultation.js` → `/chat/sessions`、`/chat/sessions/{id}/messages`、`/chat/sessions/{id}/emotion`、`/chat/stream`；删除双路径兜底与 `session_` 前缀处理。
   - `knowledge.js` → `/knowledge/categories`、`/knowledge/articles`、`/knowledge/articles/{id}`、`/admin/knowledge/articles`。
   - `file.js` → `/files`。
3. 视图层：
   - `Login.vue`：移除 `userType` 单选，落地页由服务端角色决定。
   - `Diary.vue`：提交字段收敛为 `content`。
   - `Consultation.vue`/`Consultations.vue`：按 `items` 与固定 SSE 事件解析；`senderType` 用 `USER/ASSISTANT`。
   - `Knowledge.vue`/`ArticleDetail.vue`/`KnowledgeFront.vue`：状态枚举映射中文；`items` 分页。
   - `Dashboard.vue`：改用 `/admin/analytics/*`。
   - `route/index.js`：后台访问依据服务端 `roles`，不再依赖前端 `userType`。
4. 清理：删除页面中多字段兜底、调试 `console.log`、字段自选逻辑。

## 6. 配置与启动

- `application.yml`：默认 `DB_USERNAME=mindhaven`、`DB_PASSWORD=`、`CORS_ALLOWED_ORIGINS=http://localhost:5173`、`JPA_DDL_AUTO=validate`、`FLYWAY_ENABLED=true`。
- 新增 `application-local.yml`：`spring.profiles` 激活 `local`，写入演示账号初始化开关、本地 AI 适配器开关、文件存储目录。
- 运行方式（终端设置环境变量，Spring Boot 不自动读 `.env`）：
  - 后端：`cd backend && mvn spring-boot:run`
  - 前端：`cd frontend && npm install && npm run dev`
- 前端 `vite.config.js` 已把 `/api` 代理到 `localhost:1235`，无需改动。

## 7. 里程碑与验收

| 阶段 | 内容 | 退出条件 |
| --- | --- | --- |
| P0 | 数据库初始化 + 后端通用层（响应/异常/requestId/分页） | `mvn spring-boot:run` 成功，Flyway 版本化建表，`GET /api/health` 与 `/api/v1` 健康检查通过 |
| P1 | 认证/用户/角色（register/login/me/admin users + 拦截器授权） | 注册→登录→携带 Bearer 访问受保护接口成功；无 token/越权返回 401/403；`local` 演示管理员可登录 |
| P2 | 日记、知识库、文件闭环 | 用户日记 CRUD/趋势、文章 CRUD/状态机/公开浏览、封面上传下载均通过 API 验收 |
| P3 | 会话 + SSE + 本地 AI + 风险 | 新建会话→流式对话（message/done）→情绪分析→CRISIS 关键词触发固定安全指引并落风险事件 |
| P4 | 聚合看板 + 审计 + 风险处置 | Dashboard 使用 `/admin/analytics/*` 出图；风险事件状态流转；敏感操作写审计 |
| P5 | 前端迁移 `/api/v1` 并联调 | 全页面无字段兜底；登录/日记/会话/知识/看板全流程跑通；`npm run build` 通过 |

## 8. 验证方式

- 后端：`cd backend && mvn test`（含既有测试，补充最小集成测试）；手工 `curl` 冒烟脚本。
- 数据库：`backend/database/verify.sql` 输出表清单与种子计数。
- 前端：`npm run build`；浏览器全流程手工 E2E（注册/登录/日记/会话/知识/看板/风险提示）。
- 越权抽查：用户 A 访问用户 B 的日记/会话返回 403 或 404。

## 9. 风险与待办确认

| 风险/待办 | 影响 | 缓解 |
| --- | --- | --- |
| MySQL 9.2 与 V1 迁移（CHECK/生成列/`mysql-connector-j` 认证插件）兼容性 | 中 | 初始化后立即用 verify.sql 验证；不兼容则新增兼容迁移 |
| 前端迁移 `/api/v1` 工作量与回归 | 中 | 先冻结 OpenAPI 契约；按模块逐步迁移并用页面回归 |
| 本地规则式 AI 的回复质量与风险覆盖面有限 | 低（本地目标） | 关键词词典集中配置；明确 `local-rule-v1` 仅为开发用途 |
| JWT 无刷新/撤销，登出仅清前端凭证 | 中（生产） | 本地阶段接受；生产按 ADR-001 引入服务端会话/刷新令牌 |
| 文章 HTML 清洗完整度 | 中 | 本地先做基础白名单；文档标注生产需接入成熟清洗库 |
| root 密码提供方式 | 待办 | 用户需提供可用的 root 密码用于一次性建库建账号 |

## 10. 交付物清单

- `docs/PLAN_LOCAL_BACKEND.md`（本计划）
- `backend/`：新增 `/api/v1` 控制器/Service/实体/仓储、JWT 拦截器、全局异常、`AiAdapter` 本地实现、`local` profile 演示数据、补充迁移（如需）、集成测试
- `frontend/src/api/*` 与相关视图迁移到 `/api/v1`
- 数据库：`mind_haven` 库、`mindhaven` 应用账号、Flyway 表与种子、验证脚本
- 启动说明（后端环境变量、前端代理、演示账号）
