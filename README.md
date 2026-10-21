# 心晴情绪 AI 助手

最新完善及验证：[2026-10-21 完善记录](./docs/IMPROVEMENTS_2026-10-21.md)。认证已升级为可撤销服务端会话，更新前的登录状态需重新登录。

MindHaven 面向成年普通用户提供情绪日记、AI 陪伴式对话和心理健康科普。它不是医疗、心理诊断或紧急救援服务。

> 当前阶段：后端已实现 `/api/v1` 业务接口（认证/RBAC、情绪日记、AI 会话 SSE、知识库、文件、聚合看板、风险事件）；AI 咨询默认通过 DeepSeek API 生成回复和情绪花园评分，需要配置后端密钥。高风险分流保留本地规则；前端已对齐 `/api/v1` 契约。

> 质量状态：2026-08-03 已通过前端生产构建、后端编译、Flyway V1-V4/Schema 校验和代表性接口冒烟；自动化测试目前只有 1 个最小后端测试，尚未达到生产上线门槛。详见 [项目体检与测试报告](./docs/PROJECT_AUDIT.md)。

## 目录结构

```text
MindHaven/
├─ frontend/  Vue 3 + Vite + Element Plus 前端
├─ backend/   Spring Boot + Maven + MySQL 后端
└─ docs/      PRD、架构、API、数据模型和路线图
```

## 项目文档

- [文档中心](./docs/README.md)
- [产品需求文档](./docs/PRD.md)
- [架构分析与演进设计](./docs/ARCHITECTURE.md)
- [API 契约基线](./docs/API.md)
- [数据模型设计](./docs/DATA_MODEL.md)
- [交付路线图](./docs/ROADMAP.md)

## 启动后端

环境要求：Java 21、Maven 3.6.3+、MySQL 8+。

1. 在 MySQL 中执行 `backend/database/init.sql` 创建 `mind_haven` 库。
2. 创建应用账号（或直接使用 root 配置）：

```sql
CREATE USER IF NOT EXISTS 'mindhaven'@'localhost' IDENTIFIED BY 'mindhaven2026';
GRANT ALL PRIVILEGES ON mind_haven.* TO 'mindhaven'@'localhost';
FLUSH PRIVILEGES;
```

3. 参考 `backend/.env.example` 配置变量。使用下方命令从 `backend` 目录启动时，会读取该目录的 `.env`（Java properties 格式，值不要加引号）；系统环境变量可覆盖文件配置。密钥文件已被 Git 忽略。
4. 运行：

```bash
cd backend
mvn spring-boot:run
```

服务默认监听 `1235` 端口，健康检查地址为 `http://localhost:1235/api/v1/health`。
默认激活 `local` profile，首次启动会创建演示账号：管理员 `admin/admin123`、普通用户 `demo/demo123`。
AI 对话默认调用 DeepSeek。在 `backend/.env` 中填写 `DEEPSEEK_API_KEY=你的密钥`，重启后端后生效。默认模型为 `deepseek-flash`，可使用 `DEEPSEEK_MODEL` 调整；配置说明见 [后端文档](./backend/README.md#deepseek-接入)。未配置密钥时普通对话会明确提示配置问题，不会伪装为模型回复。纯本地演示可设置 `AI_PROVIDER=local`。

## 启动前端

环境要求：Node.js 与 npm。

```bash
cd frontend
npm install
npm run dev
```

前端默认访问 `http://localhost:5173`，开发服务器将 `/api` 和 `/files` 转发到 `http://localhost:1235`。如需修改后端地址，可复制 `frontend/.env.example` 为 `.env.local` 并调整配置。

## 构建与测试

```bash
cd frontend
npm run build

cd ../backend
mvn test
```
