# 心晴情绪 AI 助手

MindHaven 面向成年普通用户提供情绪日记、AI 陪伴式对话和心理健康科普。它不是医疗、心理诊断或紧急救援服务。

> 当前阶段：本地可完整联调。后端已实现 `/api/v1` 业务接口（认证/RBAC、情绪日记、AI 会话 SSE、知识库、文件、聚合看板、风险事件），AI 使用本地规则式适配器；前端已对齐 `/api/v1` 契约。

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

3. 参考 `backend/.env.example` 将数据库等变量配置到系统、IDE 启动配置或终端环境。Spring Boot 默认不会自动读取 `.env` 文件。
4. 运行：

```bash
cd backend
mvn spring-boot:run
```

服务默认监听 `1235` 端口，健康检查地址为 `http://localhost:1235/api/v1/health`。
默认激活 `local` profile，首次启动会创建演示账号：管理员 `admin/admin123`、普通用户 `demo/demo123`。
AI 对话使用内置本地规则式回复（`local-rule-v1`），无需外部 API Key。

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
