# 心晴情绪 AI 助手

## 目录结构

```text
心情情绪AI助手/
├─ frontend/  Vue 3 前端
└─ backend/   Spring Boot 后端
```

## 启动后端

1. 在 MySQL 中执行 `backend/database/init.sql`。
2. 根据本机环境设置 `DB_USERNAME`、`DB_PASSWORD` 等环境变量。
3. 进入 `backend` 并运行：

```bash
mvn spring-boot:run
```

健康检查地址：`http://localhost:1235/api/health`。

## 启动前端

进入 `frontend` 并运行：

```bash
npm install
npm run dev
```

前端默认访问 `http://localhost:5173`，开发代理指向本地后端 `http://localhost:1235`。
