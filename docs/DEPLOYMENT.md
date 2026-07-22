# 会议室预约系统 - 部署指南

## 架构总览

```
                          ┌─────────────────┐
                          │   用户浏览器     │
                          └────────┬────────┘
                                   │
                          ┌────────▼────────┐
                          │  Nginx (80)     │
                          │  静态资源 + 反代  │
                          └────────┬────────┘
                     ┌─────────────┼─────────────┐
                     │             │             │
              ┌──────▼──────┐     │      ┌──────▼──────┐
              │  前端 dist   │     │      │  /api/**    │
              │  Vue 静态文件 │     │      │  反向代理    │
              └─────────────┘     │      └──────┬──────┘
                                  │             │
                          ┌───────▼─────────────▼───────┐
                          │     Gateway (8080)           │
                          │  路由转发 + Token 校验        │
                          └───────┬──────────┬──────────┘
                                  │          │
                     ┌────────────▼──┐  ┌────▼───────────┐
                     │ User (8081)   │  │ Auth (8082)    │
                     └───────┬───────┘  └───────┬────────┘
                             │                  │
                     ┌───────▼──────────────────▼───────┐
                     │         Meeting (8083)            │
                     └───────┬──────────┬──────────┬────┘
                             │          │          │
                     ┌───────▼──┐ ┌─────▼────┐ ┌──▼──────┐
                     │  MySQL   │ │  Redis   │ │  Nacos  │
                     │  (3306)  │ │  (6379)  │ │  (8848) │
                     └──────────┘ └──────────┘ └─────────┘
```

---

## 方案一：JAR 直接部署（适合初学）

### 1.1 前置条件

- 服务器已安装 JDK 17+
- MySQL、Redis、Nacos 已在 Docker 中运行

### 1.2 本地打包

```bash
# 后端打包
cd backend
mvn clean package -DskipTests

# 前端打包（修改 API 地址）
cd frontend
# 编辑 .env.production
echo "VITE_API_BASE_URL=http://你的服务器IP" > .env.production
npm run build
```

### 1.3 上传到服务器

```bash
# 后端 JAR
scp backend/mrb-gateway/target/mrb-gateway-1.0.0-SNAPSHOT.jar root@IP:/opt/mrb/jar/
scp backend/mrb-user/target/mrb-user-1.0.0-SNAPSHOT.jar root@IP:/opt/mrb/jar/
scp backend/mrb-auth/target/mrb-auth-1.0.0-SNAPSHOT.jar root@IP:/opt/mrb/jar/
scp backend/mrb-meeting/target/mrb-meeting-1.0.0-SNAPSHOT.jar root@IP:/opt/mrb/jar/

# 前端静态文件
scp -r frontend/dist root@IP:/opt/mrb/frontend/
```

### 1.4 服务器配置

#### 目录结构

```
/opt/mrb/
├── jar/                    # JAR 包
│   ├── mrb-gateway-1.0.0-SNAPSHOT.jar
│   ├── mrb-user-1.0.0-SNAPSHOT.jar
│   ├── mrb-auth-1.0.0-SNAPSHOT.jar
│   └── mrb-meeting-1.0.0-SNAPSHOT.jar
├── config/                 # 生产配置（优先级高于 jar 内配置）
│   ├── mrb-user-prod.yml
│   ├── mrb-auth-prod.yml
│   ├── mrb-meeting-prod.yml
│   └── mrb-gateway-prod.yml
├── logs/                   # 日志目录
├── frontend/               # 前端静态文件
│   └── dist/
├── start.sh                # 启动脚本
├── stop.sh                 # 停止脚本
└── deploy.sh               # 一键部署脚本
```

#### 生产配置示例

`/opt/mrb/config/mrb-user-prod.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://你的MySQL:3306/mrb_user?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: root
    password: 你的密码
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: 你的Redis
      port: 6379

mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: auto
```

`/opt/mrb/config/mrb-auth-prod.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://你的MySQL:3306/mrb_auth?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
  data:
    redis:
      host: 你的Redis
      port: 6379

jwt:
  secret: 用一个复杂的随机字符串
  expiration: 86400000
```

`/opt/mrb/config/mrb-gateway-prod.yml`：

```yaml
spring:
  cloud:
    nacos:
      server-addr: 你的Nacos:8848
      discovery:
        namespace: 你的namespace
```

#### 启动脚本 `/opt/mrb/start.sh`

```bash
#!/bin/bash
JAR_DIR="/opt/mrb/jar"
CONFIG_DIR="/opt/mrb/config"
LOG_DIR="/opt/mrb/logs"

mkdir -p $LOG_DIR

start() {
  local name=$1
  local jar=$2
  local port=$3
  local config=$4

  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting $name..."
  nohup java -jar $JAR_DIR/$jar \
    --spring.config.import="optional:file:$CONFIG_DIR/$config" \
    --spring.profiles.active=prod \
    --server.port=$port \
    > $LOG_DIR/$name.log 2>&1 &
  echo "  PID: $! | Port: $port"
}

start mrb-user    mrb-user-1.0.0-SNAPSHOT.jar    8081  mrb-user-prod.yml
sleep 15
start mrb-auth    mrb-auth-1.0.0-SNAPSHOT.jar    8082  mrb-auth-prod.yml
sleep 10
start mrb-meeting mrb-meeting-1.0.0-SNAPSHOT.jar 8083  mrb-meeting-prod.yml
sleep 10
start mrb-gateway mrb-gateway-1.0.0-SNAPSHOT.jar 8080  mrb-gateway-prod.yml

echo ""
echo "[$(date '+%Y-%m-%d %H:%M:%S')] All services started!"
echo "  Gateway:    http://你的IP:8080"
echo "  User:       http://你的IP:8081"
echo "  Auth:       http://你的IP:8082"
echo "  Meeting:    http://你的IP:8083"
```

#### 停止脚本 `/opt/mrb/stop.sh`

```bash
#!/bin/bash
echo "Stopping all services..."
ps aux | grep "mrb-" | grep -v grep | awk '{print $2}' | xargs kill 2>/dev/null
sleep 2
echo "All services stopped."
```

#### 赋予执行权限

```bash
chmod +x /opt/mrb/start.sh
chmod +x /opt/mrb/stop.sh
```

### 1.5 Nginx 配置

`/etc/nginx/conf.d/mrb.conf`：

```nginx
server {
    listen 80;
    server_name 你的域名或IP;

    # 前端静态文件
    location / {
        root /opt/mrb/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理到 Gateway
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_read_timeout 60s;
    }
}
```

```bash
nginx -t && nginx -s reload
```

### 1.6 启动顺序

```
MySQL/Redis/Nacos(已有) → start.sh → 访问 http://IP
```

---

## 方案二：Docker 部署（推荐生产环境）

### 2.1 Docker 核心概念（30秒理解）

| 概念 | 类比 | 说明 |
|------|------|------|
| **镜像 (Image)** | 安装包 | 打包好的运行环境，只读模板 |
| **容器 (Container)** | 运行中的程序 | 镜像的实例，可运行、可停止 |
| **Dockerfile** | 安装说明书 | 如何从源码构建镜像 |
| **docker-compose** | 批量启动脚本 | 一键管理多个容器 |
| **卷 (Volume)** | 外挂硬盘 | 容器数据持久化，重启不丢失 |

**工作流程：**
```
Dockerfile → docker build → 镜像 → docker run → 容器
                                                    ↓
                                    docker-compose up 一键启动所有
```

### 2.2 创建 Dockerfile

每个服务一个 Dockerfile，放在各自模块目录下。

#### 后端 Dockerfile（通用模板）

`backend/Dockerfile`：

```dockerfile
# 第一阶段：构建
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 先复制 pom.xml，利用 Docker 缓存层（依赖不变时不重新下载）
COPY pom.xml .
COPY mrb-common/pom.xml mrb-common/
COPY mrb-gateway/pom.xml mrb-gateway/
COPY mrb-user/pom.xml mrb-user/
COPY mrb-auth/pom.xml mrb-auth/
COPY mrb-meeting/pom.xml mrb-meeting/
RUN mvn dependency:go-offline -B

# 再复制源码并构建
COPY . .
RUN mvn clean package -DskipTests -B

# 第二阶段：运行（使用更小的 JRE 镜像）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 从构建阶段复制 JAR
COPY --from=builder /app/mrb-gateway/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令，通过环境变量覆盖配置
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> **为什么要两个阶段？** 构建阶段需要 Maven + JDK（~800MB），运行阶段只需要 JRE（~200MB）。分阶段可以让最终镜像更小。

#### 前端 Dockerfile

`frontend/Dockerfile`：

```dockerfile
# 第一阶段：构建
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 第二阶段：Nginx 服务
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

`frontend/nginx.conf`：

```nginx
server {
    listen 80;
    server_name localhost;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://gateway:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 2.3 Docker Compose 编排

`docker-compose.yml`（放在项目根目录）：

```yaml
version: '3.8'

services:
  # ========== 基础设施 ==========
  mysql:
    image: mysql:8.0
    container_name: mrb-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD:-root}
      TZ: Asia/Shanghai
    volumes:
      - mysql-data:/var/lib/mysql
      - ./backend/sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - mrb-net

  redis:
    image: redis:7-alpine
    container_name: mrb-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - mrb-net

  nacos:
    image: nacos/nacos-server:v2.3.1
    container_name: mrb-nacos
    ports:
      - "8848:8848"
      - "9848:9848"
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: ""
      JVM_XMS: 256m
      JVM_XMX: 256m
    networks:
      - mrb-net

  # ========== 后端服务 ==========
  mrb-user:
    build:
      context: ./backend
      args:
        SERVICE_NAME: mrb-user
    container_name: mrb-user
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-}
      MYSQL_HOST: mysql
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-root}
      REDIS_HOST: redis
      JWT_SECRET: ${JWT_SECRET:-your-secret-key}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
      nacos:
        condition: service_started
    networks:
      - mrb-net
    restart: unless-stopped

  mrb-auth:
    build:
      context: ./backend
      args:
        SERVICE_NAME: mrb-auth
    container_name: mrb-auth
    ports:
      - "8082:8082"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-}
      MYSQL_HOST: mysql
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-root}
      REDIS_HOST: redis
      JWT_SECRET: ${JWT_SECRET:-your-secret-key}
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
      nacos:
        condition: service_started
    networks:
      - mrb-net
    restart: unless-stopped

  mrb-meeting:
    build:
      context: ./backend
      args:
        SERVICE_NAME: mrb-meeting
    container_name: mrb-meeting
    ports:
      - "8083:8083"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-}
      MYSQL_HOST: mysql
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-root}
      REDIS_HOST: redis
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
      nacos:
        condition: service_started
    networks:
      - mrb-net
    restart: unless-stopped

  mrb-gateway:
    build:
      context: ./backend
      args:
        SERVICE_NAME: mrb-gateway
    container_name: mrb-gateway
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-}
    depends_on:
      nacos:
        condition: service_started
    networks:
      - mrb-net
    restart: unless-stopped

  # ========== 前端 ==========
  nginx:
    build:
      context: ./frontend
    container_name: mrb-nginx
    ports:
      - "80:80"
    depends_on:
      - mrb-gateway
    networks:
      - mrb-net
    restart: unless-stopped

volumes:
  mysql-data:
  redis-data:

networks:
  mrb-net:
    driver: bridge
```

### 2.4 通用后端 Dockerfile（多服务复用）

由于四个服务用同一个 Dockerfile，通过构建参数选择：

`backend/Dockerfile`（改进版）：

```dockerfile
# 构建参数：指定服务名
ARG SERVICE_NAME

# === 阶段 1：构建 ===
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 先复制所有 pom.xml（利用缓存层）
COPY pom.xml .
COPY mrb-common/pom.xml mrb-common/
COPY mrb-gateway/pom.xml mrb-gateway/
COPY mrb-user/pom.xml mrb-user/
COPY mrb-auth/pom.xml mrb-auth/
COPY mrb-meeting/pom.xml mrb-meeting/
RUN mvn dependency:go-offline -B

# 复制源码并构建指定模块
COPY . .
RUN mvn clean package -DskipTests -pl ${SERVICE_NAME} -am -B

# === 阶段 2：运行 ===
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 从构建阶段复制 JAR（用通配符匹配）
COPY --from=builder /app/${SERVICE_NAME}/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-jar", "app.jar", \
  "--spring.config.import=optional:file:/config/application-prod.yml"]
```

### 2.5 环境变量文件

`backend/.env.prod`（不提交到 git）：

```bash
# MySQL
MYSQL_PASSWORD=your-secure-password

# JWT
JWT_SECRET=your-very-long-random-secret-key-at-least-32-chars

# Nacos
NACOS_NAMESPACE=your-namespace-id
```

使用方式：
```bash
# 启动时指定环境变量文件
docker compose --env-file backend/.env.prod up -d
```

### 2.6 常用命令

```bash
# 启动所有服务
docker compose up -d

# 查看运行状态
docker compose ps

# 查看日志（实时跟踪）
docker compose logs -f mrb-gateway

# 查看某个服务日志
docker compose logs -f mrb-auth

# 重启某个服务
docker compose restart mrb-user

# 停止所有服务
docker compose down

# 停止并删除数据卷（慎用！会删除数据库数据）
docker compose down -v

# 重新构建某个服务（代码更新后）
docker compose build mrb-user
docker compose up -d mrb-user

# 进入容器内部调试
docker exec -it mrb-user sh

# 查看容器资源占用
docker stats
```

### 2.7 部署流程

```bash
# 1. 上传代码到服务器
git clone 你的仓库地址 /opt/mrb-project

# 2. 进入项目目录
cd /opt/mrb-project

# 3. 配置环境变量
cp backend/.env.prod.example backend/.env.prod
vim backend/.env.prod  # 编辑密码等敏感信息

# 4. 启动所有服务（首次会自动构建镜像，耗时较长）
docker compose --env-file backend/.env.prod up -d --build

# 5. 查看状态
docker compose ps

# 6. 访问测试
curl http://localhost:8080/api/meeting/room/list
```

---

## 方案对比

| 维度 | JAR 部署 | Docker 部署 |
|------|----------|-------------|
| **环境一致性** | 依赖服务器 JDK 版本 | 镜像自带环境，100% 一致 |
| **部署复杂度** | 需手动安装 JDK、配置路径 | 一条命令启动 |
| **资源占用** | 较低 | 每个容器有独立运行时 |
| **扩容** | 手动复制 JAR + 改端口 | docker compose scale 或 K8s |
| **日志管理** | 需手动配置 logback | docker compose logs 统一查看 |
| **数据持久化** | 手动管理 | Volume 自动管理 |
| **适用场景** | 开发/测试、小规模 | 生产环境、团队协作 |

---

## 故障排查

### 服务启动失败

```bash
# 查看日志
tail -100 /opt/mrb/logs/mrb-user.log        # JAR 部署
docker compose logs mrb-user                  # Docker 部署

# 常见问题
# 1. 端口被占用 → lsof -i :8081
# 2. MySQL 连不上 → 检查地址、端口、密码
# 3. Redis 连不上 → docker compose ps 查看状态
# 4. Nacos 连不上 → 检查 server-addr 和 namespace
```

### 接口 500 错误

```bash
# 查看具体异常
grep "Exception" /opt/mrb/logs/mrb-auth.log | tail -20

# 常见原因
# 1. 数据库表不存在 → 执行 init.sql
# 2. 密码错误 → 检查 MySQL/Redis 密码
# 3. JWT 密钥问题 → 检查 jwt.secret 配置
```

### Docker 容器重启循环

```bash
# 查看退出原因
docker inspect mrb-user --format='{{.State.ExitCode}}'
docker compose logs --tail=50 mrb-user
```

---

## 安全建议

1. **修改默认密码**：MySQL root 密码、JWT secret 必须修改
2. **限制端口暴露**：MySQL/Redis/Nacos 不要映射到公网
3. **使用 HTTPS**：配置 SSL 证书（Let's Encrypt 免费）
4. **定期备份**：MySQL 数据卷定期导出
5. **日志清理**：配置 logrotate 或 Docker 日志驱动限制大小
