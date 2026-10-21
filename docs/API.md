# MindHaven API 契约基线

2026-10-21更新：Bearer凭证改为服务端随机会话，退出撤销、刷新轮换；注册必填 `privacyConsentVersion=2026-10-21`。管理端日记读取要求 `risk:read-sensitive`、删除要求 `risk:resolve`。风险详情增加必填查询参数 `reason`（5–200字），处置必填 `note`（1–500字）。文件上传要求 `article:manage`，仅接受ARTICLE用途的PNG/JPEG，并重新编码。聊天每条最长4000字，已完成requestId复用结果。详见 [完善记录](./IMPROVEMENTS_2026-10-21.md)；下方旧基线与此冲突时以本更新为准。

> 版本：v1-implemented-draft · 2026-08-03
> 状态：`/api/v1` 主体接口已实现并与前端联调；仍缺 OpenAPI 与完整契约测试。

## 1. 通用约定

- 基础路径：`/api/v1`。迁移期间可保留旧 `/api` 适配，但新实现统一版本化。
- 内容类型：JSON 使用 `application/json; charset=utf-8`，上传使用 `multipart/form-data`，流式对话使用 `text/event-stream`。
- 字段命名：请求与响应统一 `camelCase`。
- 时间：ISO 8601，包含时区，例如 `2026-08-01T10:30:00+08:00`。
- ID：当前服务端使用 MySQL `BIGINT`，JSON 中返回数字；若 ID 可能超过 JavaScript 安全整数，发布前改为字符串序列化。
- 认证：当前使用 `Authorization: Bearer <access-token>` 和本地 HMAC-SHA256 令牌；生产方案仍需补充撤销/刷新与 ADR。
- 分页：统一 `page`（从 1 开始）、`size`（默认 20，最大 100）。

### 1.1 成功响应

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "requestId": "01J..."
}
```

分页 `data`：

```json
{
  "items": [],
  "page": 1,
  "size": 20,
  "total": 0,
  "totalPages": 0
}
```

### 1.2 错误响应

```json
{
  "code": "VALIDATION_ERROR",
  "message": "请求参数不合法",
  "data": {
    "fieldErrors": [{ "field": "moodScore", "message": "必须在 1 到 10 之间" }]
  },
  "requestId": "01J..."
}
```

| HTTP | 业务码示例 | 含义 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 参数、状态流转或业务规则错误 |
| 401 | `UNAUTHENTICATED`、`TOKEN_EXPIRED` | 未登录或凭证失效 |
| 403 | `FORBIDDEN` | 角色或对象级权限不足 |
| 404 | `RESOURCE_NOT_FOUND` | 资源不存在或无权感知其存在 |
| 409 | `RESOURCE_CONFLICT`、`DUPLICATE_REQUEST` | 唯一约束、重复提交或状态冲突 |
| 413 | `FILE_TOO_LARGE`、`MESSAGE_TOO_LARGE` | 负载超过限制 |
| 429 | `RATE_LIMITED` | 频率或额度超限 |
| 502/503 | `AI_PROVIDER_UNAVAILABLE` | 上游模型不可用且无法正常降级 |

## 2. 认证与用户

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/auth/register` | 公开 | 注册普通用户；忽略任何角色字段 |
| POST | `/auth/login` | 公开 | 登录并返回服务端身份摘要 |
| POST | `/auth/logout` | 登录 | 当前仅返回成功，客户端清理令牌；服务端撤销未实现 |
| POST | `/auth/refresh` | 刷新凭证 | 当前返回 401，要求重新登录；轮换未实现 |
| GET | `/users/me` | 登录 | 当前用户资料和权限 |
| PATCH | `/users/me` | 登录 | 修改允许的个人资料字段 |
| GET | `/admin/users` | 用户管理员 | 分页查询用户，不默认返回敏感正文 |

注册请求：

```json
{
  "username": "mind_user",
  "email": "user@example.com",
  "password": "user-entered-secret",
  "nickname": "小晴",
  "phone": "13800000000",
  "privacyConsentVersion": "2026-08-01"
}
```

登录响应的 `data.user` 至少包含 `id`、`username`、`nickname`、`roles`、`permissions`。前端不得自行写入或覆盖角色。

## 3. 情绪日记

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/diaries` | 用户 | 新建日记 |
| GET | `/diaries` | 用户 | 查询自己的日记 |
| GET | `/diaries/{diaryId}` | 所有者 | 日记详情 |
| PUT | `/diaries/{diaryId}` | 所有者 | 更新日记 |
| DELETE | `/diaries/{diaryId}` | 所有者 | 删除日记 |
| GET | `/diaries/trends` | 用户 | 个人趋势聚合 |
| GET | `/admin/diaries` | 安全运营权限 | 脱敏列表，按权限决定可见字段 |
| DELETE | `/admin/diaries/{diaryId}` | 高权限管理员 | 合规/安全删除并审计 |

写入模型：

```json
{
  "diaryDate": "2026-08-01",
  "moodScore": 7,
  "dominantEmotion": "CALM",
  "emotionTriggers": "完成了一项困难的工作",
  "content": "今天比预期顺利。",
  "sleepQuality": 4,
  "stressLevel": 2
}
```

约束：`moodScore` 为 1–10，`sleepQuality`/`stressLevel` 为 1–5。若采用“一天一篇”，重复日期返回 409，并在 `data.existingDiaryId` 指明已有记录。

## 4. AI 会话

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/chat/sessions` | 用户 | 新建会话 |
| GET | `/chat/sessions` | 用户 | 查询自己的会话 |
| GET | `/chat/sessions/{sessionId}` | 所有者 | 会话摘要 |
| PATCH | `/chat/sessions/{sessionId}` | 所有者 | 重命名/归档 |
| DELETE | `/chat/sessions/{sessionId}` | 所有者 | 删除会话 |
| GET | `/chat/sessions/{sessionId}/messages` | 所有者 | 分页查询消息 |
| GET | `/chat/sessions/{sessionId}/emotion` | 所有者 | 最新会话情绪分析 |
| POST | `/chat/stream` | 所有者 | 发送消息并返回 SSE |

流式请求：

```json
{
  "sessionId": "01JSESSION...",
  "requestId": "client-generated-uuid",
  "message": "最近总是睡不好，心里很乱。"
}
```

SSE 固定协议：

```text
event: message
data: {"delta":"我听见你最近很辛苦。"}

event: risk
data: {"level":"WARNING","guidanceCode":"LOCAL_SUPPORT"}

event: done
data: {"messageId":"01JMSG...","finishReason":"STOP"}
```

错误使用 `event: error`，`data` 仍遵循通用错误结构。SSE 响应必须关闭代理缓冲，并发送必要的心跳；客户端仅处理上述字段，不再猜测供应商格式。

2026-10-20 接入更新：默认通过 DeepSeek Chat Completions 生成回复，配置见 `backend/README.md`。当前仍为等待上游完整回复后切片推送 SSE；高风险分流保留本地规则。情绪分析单独使用 DeepSeek JSON Output，只输入最近20条聊天消息中的用户发言，重点参考最新表达。密钥缺失、上游认证失败、余额不足、限流、超时等聊天错误通过 `event: error` 返回 `AI_PROVIDER_UNAVAILABLE` 及可读提示，不回退为普通本地回复。

情绪分析响应增加 `analysisStatus`：`PENDING`（尚无分析）、`SUCCEEDED`（成功）、`FAILED`（本轮分析失败）。失败时 `emotionScore` 为 null，保留正常聊天回复；前端不能把 null 当0分或展示上一轮分数。分数范围包含0，越高代表表达越积极稳定，不是临床量表。

情绪分析响应：

```json
{
  "emotionScore": 42,
  "label": "ANXIOUS",
  "isNegative": true,
  "riskLevel": "CONCERN",
  "riskDescription": "检测到持续焦虑表达，但未发现即时危机信号。",
  "suggestion": "先尝试放慢呼吸，并考虑向可信任的人说明近况。",
  "improvementSuggestions": ["短暂离开屏幕", "记录今晚的睡眠情况"],
  "analyzedAt": "2026-08-01T10:30:00+08:00",
  "modelVersion": "internal-alias-v1"
}
```

## 5. 知识库

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/knowledge/categories` | 公开 | 分类树 |
| GET | `/knowledge/articles` | 公开 | 仅返回已发布文章，支持 `keyword`、`categoryId`、分页 |
| GET | `/knowledge/articles/{articleId}` | 公开 | 已发布文章详情 |
| POST | `/admin/knowledge/articles` | 内容管理员 | 新建草稿 |
| PUT | `/admin/knowledge/articles/{articleId}` | 内容管理员 | 更新文章 |
| PUT | `/admin/knowledge/articles/{articleId}/status` | 内容管理员 | 状态流转 |
| DELETE | `/admin/knowledge/articles/{articleId}` | 内容管理员 | 归档/按策略删除 |

文章模型的稳定字段：`id`、`title`、`summary`、`contentHtml`、`coverUrl`、`category`、`tags`、`author`、`status`、`viewCount`、`publishedAt`、`createdAt`、`updatedAt`。服务端返回的 `contentHtml` 必须已清洗。

## 6. 文件

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/files` | 按业务权限 | 上传文件并返回文件 ID 与受控 URL |
| GET | `/files/{fileId}` | 按资源权限 | 私有文件鉴权下载或重定向到短期签名 URL |
| DELETE | `/files/{fileId}` | 资源所有者/管理员 | 解绑并按引用计数清理 |

表单字段统一为 `file`、`businessType`、`businessId`、`businessField`。服务端校验调用者是否有权关联该业务对象，不能信任客户端提供的业务 ID。

## 7. 管理端聚合与风险

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/admin/analytics/overview` | 运营查看 | 用户、日记、会话、情绪汇总 |
| GET | `/admin/analytics/trends` | 运营查看 | 按 `from`、`to`、`granularity` 聚合趋势 |
| GET | `/admin/risk-events` | 安全运营 | 风险事件脱敏分页 |
| GET | `/admin/risk-events/{eventId}` | 敏感查看权限 | 详情，记录访问理由和审计 |
| PUT | `/admin/risk-events/{eventId}/status` | 安全运营 | `OPEN → REVIEWING → RESOLVED/FALSE_POSITIVE` |

## 8. 当前实现偏差与兼容边界

| 项目 | 当前实现 | 收敛方向 |
| --- | --- | --- |
| ID | JSON 数字 | 超过安全整数前统一字符串化，并补契约测试 |
| 登出/刷新 | 登出不撤销；刷新返回未认证 | 引入服务端会话或可撤销刷新令牌轮换 |
| AI | DeepSeek 适配器（默认 `deepseek-flash`）生成回复与情绪评分；高风险指引保留本地规则 | 自有 SSE 协议；配置 `DEEPSEEK_API_KEY`；可用 `AI_PROVIDER=local` 切换纯本地演示 |
| OpenAPI | 未提供 | 生成规范并让前端客户端/类型与 CI 以其为基线 |
| 协议异常 | 不支持的 `Content-Type` 会被映射成 500 | 映射为 415/400，纳入 Controller 契约测试 |
| 文件下载 | `GET /files/{id}` 当前作为公开路径放行 | 按业务对象可见性区分公开封面与私有文件 |
| 权限集合 | 角色合并后可能返回重复 permission code | 服务端去重并稳定排序 |

## 9. 契约测试要求

- 后端为所有接口提供 OpenAPI 规范，并由 CI 校验格式。
- 前端类型/客户端从 OpenAPI 生成或集中维护，禁止页面各自兼容字段别名。
- 每个 P0 接口覆盖成功、校验失败、未登录、越权、资源不存在和重复请求。
- SSE 覆盖分片边界、UTF-8 多字节、心跳、客户端断开、上游超时、风险事件和空回复。
