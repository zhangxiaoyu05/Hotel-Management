# 多阶段构建Dockerfile

# 阶段1: 构建前端
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend

# 复制前端依赖文件
COPY apps/web/package*.json ./
RUN npm ci --only=production

# 复制前端源码并构建
COPY apps/web/ .
RUN npm run build

# 阶段2: 构建后端
FROM maven:3.8.6-openjdk-11-slim AS backend-builder
WORKDIR /app/backend

# 复制后端源码
COPY apps/api/pom.xml .
COPY apps/api/src ./src

# 构建后端应用
RUN mvn clean package -DskipTests

# 阶段3: 创建最终镜像
FROM openjdk:11-jre-slim

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    && rm -rf /var/lib/apt/lists/*

# 创建应用目录
WORKDIR /app

# 复制后端JAR文件
COPY --from=backend-builder /app/backend/target/*.jar app.jar

# 复制前端构建文件到nginx目录
RUN mkdir -p /app/frontend
COPY --from=frontend-builder /app/frontend/dist /app/frontend

# 创建启动脚本
RUN echo '#!/bin/bash\n\
set -e\n\
\n\
# 等待数据库就绪\n\
echo "Waiting for database..."\n\
until curl -f http://$DB_HOST:$DB_PORT || [ $? -eq 7 ]; do\n\
    echo "Database is unavailable - sleeping"\n\
    sleep 2\n\
done\n\
\n\
echo "Database is up - starting application"\n\
\n\
# 启动应用\n\
exec java $JAVA_OPTS -jar app.jar\n\
' > /app/start.sh && chmod +x /app/start.sh

# 创建健康检查脚本
RUN echo '#!/bin/bash\n\
curl -f http://localhost:8080/api/actuator/health || exit 1\n\
' > /app/healthcheck.sh && chmod +x /app/healthcheck.sh

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD /app/healthcheck.sh

# 设置环境变量
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"

# 启动应用
CMD ["/app/start.sh"]