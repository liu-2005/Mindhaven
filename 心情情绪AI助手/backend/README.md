# 心晴情绪 AI 助手后端

## 环境要求

- Java 21
- Maven 3.6.3+
- MySQL 8+

## 初始化

先执行 `database/init.sql` 创建数据库，再根据 `.env.example` 设置环境变量。Spring Boot 默认不会自动读取 `.env` 文件，应将变量配置到系统、IDE 启动配置或终端环境中。

## 运行

```bash
mvn spring-boot:run
```

服务端口默认为 `1235`，健康检查接口为 `GET /api/health`。

## 打包

```bash
mvn clean package
```
