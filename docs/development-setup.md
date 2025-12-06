# 开发环境搭建指南

本文档详细说明如何在本地搭建成都酒店管理系统的开发环境。

## 📋 环境要求

### 必需软件
- **Node.js**: 18.0+ (推荐使用 LTS 版本)
- **Java**: 11+ (推荐使用 OpenJDK 11)
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **Git**: 2.30+

### 可选软件
- **Docker**: 20.0+ (用于容器化开发)
- **Docker Compose**: 2.0+
- **IDE**: 推荐使用 VS Code 或 IntelliJ IDEA
- **数据库工具**: MySQL Workbench, DBeaver, Navicat 等

## 🔧 软件安装

### 1. Node.js 安装

#### Windows
```bash
# 使用 Chocolatey
choco install nodejs

# 或从官网下载安装包
# https://nodejs.org/
```

#### macOS
```bash
# 使用 Homebrew
brew install node

# 或使用 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install 18
nvm use 18
```

#### Linux (Ubuntu/Debian)
```bash
# 使用 NodeSource 仓库
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# 或使用 nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
```

验证安装：
```bash
node --version  # 应该显示 v18.x.x
npm --version   # 应该显示 9.x.x
```

### 2. Java 11 安装

#### Windows
```bash
# 使用 Chocolatey
choco install openjdk11

# 或从官网下载安装包
# https://adoptium.net/temurin/releases/?version=11
```

#### macOS
```bash
# 使用 Homebrew
brew install openjdk@11
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install openjdk-11-jdk
```

验证安装：
```bash
java -version  # 应该显示 openjdk version "11.0.x"
javac -version # 应该显示 javac 11.0.x
```

### 3. pnpm 安装

```bash
npm install -g pnpm
```

验证安装：
```bash
pnpm --version
```

### 4. MySQL 8.0 安装

#### Windows
```bash
# 使用 Chocolatey
choco install mysql

# 或从官网下载安装包
# https://dev.mysql.com/downloads/mysql/
```

#### macOS
```bash
# 使用 Homebrew
brew install mysql@8.0
brew services start mysql@8.0
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

### 5. Redis 安装

#### Windows
```bash
# 使用 Chocolatey
choco install redis-64

# 或下载 Windows 版本
# https://github.com/microsoftarchive/redis/releases
```

#### macOS
```bash
# 使用 Homebrew
brew install redis
brew services start redis
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install redis-server
sudo systemctl start redis-server
sudo systemctl enable redis-server
```

## 🚀 项目搭建

### 1. 克隆项目

```bash
git clone <repository-url>
cd chengdu-hotel-management
```

### 2. 数据库配置

#### 方式一：使用 Docker (推荐)

```bash
cd infrastructure/docker
docker-compose up -d mysql redis
```

这将启动 MySQL 和 Redis 容器，并自动初始化数据库。

#### 方式二：手动配置

**MySQL 配置：**
```sql
-- 连接到MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE hotel_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户（可选）
CREATE USER 'hotel_user'@'localhost' IDENTIFIED BY 'hotel_password';
GRANT ALL PRIVILEGES ON hotel_management.* TO 'hotel_user'@'localhost';
FLUSH PRIVILEGES;

-- 导入数据库结构
USE hotel_management;
SOURCE infrastructure/docker/mysql/init.sql;
```

**Redis 配置：**
```bash
# 启动 Redis 服务
redis-server

# 或使用系统服务
sudo systemctl start redis-server
```

### 3. 环境变量配置

在项目根目录创建 `.env` 文件：

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=hotel_management
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT配置
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=86400

# 应用配置
NODE_ENV=development
VITE_API_URL=http://localhost:8080/api
```

### 4. 前端开发环境

```bash
cd apps/web

# 安装依赖
pnpm install

# 启动开发服务器
pnpm run dev
```

前端服务将在 http://localhost:3000 启动

### 5. 后端开发环境

```bash
cd apps/api

# 安装依赖并编译
mvn clean install

# 启动开发服务器
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

## 🔧 IDE 配置

### VS Code 配置

安装推荐扩展：

```json
{
  "recommendations": [
    "vue.volar",
    "ms-vscode.vscode-typescript-next",
    "bradlc.vscode-tailwindcss",
    "esbenp.prettier-vscode",
    "dbaeumer.vscode-eslint",
    "ms-vscode.vscode-java",
    "vscjava.vscode-java-pack",
    "ms-vscode.vscode-spring-boot-dashboard-pack"
  ]
}
```

创建 `.vscode/settings.json`：

```json
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "[vue]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[typescript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[java]": {
    "editor.formatOnSave": true
  }
}
```

### IntelliJ IDEA 配置

1. **插件安装：**
   - Vue.js Plugin
   - Lombok Plugin
   - Spring Boot Plugin
   - MyBatis Plugin

2. **代码格式化：**
   - 导入项目根目录的 `.editorconfig`
   - 配置 Java 代码风格

## 🧪 验证环境

### 1. 前端验证

访问 http://localhost:3000，应该能看到：
- 首页正常加载
- 路由导航工作正常
- 登录/注册页面可以访问

### 2. 后端验证

访问 http://localhost:8080/api，应该能看到：
- API 服务正常响应
- Swagger UI 可访问：http://localhost:8080/swagger-ui.html

### 3. 数据库验证

```bash
# 连接数据库验证表结构
mysql -u root -p -e "USE hotel_management; SHOW TABLES;"

# 验证测试数据
mysql -u root -p -e "USE hotel_management; SELECT COUNT(*) FROM hotels;"
```

### 4. Redis 验证

```bash
redis-cli ping
# 应该返回 PONG
```

## 🐛 常见问题

### 1. Node.js 版本问题

```bash
# 如果版本不匹配，使用 nvm 管理
nvm install 18
nvm use 18
nvm alias default 18
```

### 2. Java 版本问题

```bash
# 检查 JAVA_HOME 设置
echo $JAVA_HOME

# 如果需要，设置环境变量
export JAVA_HOME=/path/to/java-11
```

### 3. MySQL 连接问题

```bash
# 检查 MySQL 服务状态
sudo systemctl status mysql

# 启动 MySQL 服务
sudo systemctl start mysql
```

### 4. 端口冲突

如果端口被占用，可以修改配置：

- 前端：修改 `apps/web/vite.config.ts`
- 后端：修改 `apps/api/src/main/resources/application.yml`
- MySQL：修改 `infrastructure/docker/docker-compose.yml`

### 5. 依赖安装失败

```bash
# 清除 npm 缓存
npm cache clean --force

# 清除 pnpm 缓存
pnpm store prune

# 重新安装依赖
rm -rf node_modules package-lock.json
pnpm install
```

## 📚 开发工具推荐

### 前端开发
- **浏览器**: Chrome + Vue DevTools
- **API测试**: Postman 或 Insomnia
- **UI组件**: Element Plus 官方文档

### 后端开发
- **API测试**: Postman, Insomnia, or curl
- **数据库管理**: MySQL Workbench, DBeaver
- **Redis管理**: RedisInsight

### 版本控制
- **Git客户端**: SourceTree, GitKraken
- **Git GUI**: VS Code Git 插件

---

💡 如果在搭建过程中遇到问题，请查看项目 Issues 或联系开发团队。