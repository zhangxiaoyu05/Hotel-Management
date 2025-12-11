# 成都酒店管理系统

一个现代化的酒店管理系统，采用前后端分离架构，提供完整的酒店预订、管理和评价功能。

## 🏨 项目概述

成都酒店管理系统是一个基于Vue3 + Spring Boot的全栈应用，旨在为用户提供便捷的酒店预订服务，为酒店管理者提供高效的酒店管理工具。

### 主要功能

- 🏨 **酒店管理**: 酒店信息维护、房型管理、房间状态管理
- 📝 **预订系统**: 在线预订、订单管理、入住登记
- ⭐ **评价系统**: 用户评价、评分统计、反馈管理
- 👥 **用户管理**: 用户注册登录、个人资料管理
- 🔍 **搜索筛选**: 多条件搜索酒店、价格筛选、位置搜索
- 📊 **数据统计**: 预订数据分析、收入统计、入住率分析

## 🏗️ 技术架构

### 前端技术栈
- **框架**: Vue 3.5+ (Composition API)
- **语言**: TypeScript 5.0+
- **构建工具**: Vite 4.0+
- **UI框架**: Element Plus 2.0+
- **路由**: Vue Router 4.0+
- **状态管理**: Pinia 2.0+
- **HTTP客户端**: Axios
- **包管理**: pnpm
- **代码规范**: ESLint + Prettier

### 后端技术栈
- **框架**: Spring Boot 2.7+
- **语言**: Java 11
- **数据库**: MySQL 8.0
- **ORM**: MyBatis Plus 3.5+
- **缓存**: Redis 6.0+
- **认证**: Spring Security + JWT
- **API文档**: Springdoc OpenAPI 3
- **构建工具**: Maven 3.8+

### 基础设施
- **容器化**: Docker + Docker Compose
- **CI/CD**: GitHub Actions
- **版本控制**: Git
- **代码质量**: SonarQube

## 📁 项目结构

```
chengdu-hotel-management/
├── apps/                          # 应用目录
│   ├── web/                       # 前端Vue3应用
│   │   ├── src/
│   │   │   ├── components/        # UI组件
│   │   │   ├── pages/             # 页面组件
│   │   │   ├── services/          # API客户端服务
│   │   │   ├── stores/            # Pinia状态管理
│   │   │   ├── utils/             # 前端工具
│   │   │   ├── types/             # TypeScript类型
│   │   │   └── router/            # 路由配置
│   │   ├── package.json
│   │   └── vite.config.ts
│   └── api/                       # 后端Spring Boot应用
│       ├── src/
│       │   └── main/java/com/hotel/
│       │       ├── controller/     # API控制器
│       │       ├── service/        # 业务逻辑
│       │       ├── repository/     # 数据访问层
│       │       ├── entity/         # 数据模型
│       │       ├── dto/            # 数据传输对象
│       │       ├── config/         # Spring配置
│       │       └── security/       # 安全配置
│       └── pom.xml
├── packages/                      # 共享类型和工具
│   └── shared/                     # 共享代码
├── infrastructure/                # 基础设施配置
│   └── docker/                    # Docker配置
│       ├── docker-compose.yml
│       ├── Dockerfile
│       ├── mysql/
│       └── redis/
├── docs/                          # 项目文档
│   ├── stories/                   # 用户故事
│   ├── architecture/              # 架构文档
│   └── prd/                       # 产品需求文档
├── .github/workflows/             # GitHub Actions工作流
├── README.md
└── .gitignore
```

## 🚀 快速开始

### 环境要求

- **Node.js**: 18.0+
- **Java**: 11+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **Docker**: 20.0+ (可选)
- **Git**: 2.30+

### 本地开发环境搭建

#### 1. 克隆项目
```bash
git clone <repository-url>
cd chengdu-hotel-management
```

#### 2. 数据库准备
```bash
# 启动MySQL和Redis (使用Docker)
cd infrastructure/docker
docker-compose up -d mysql redis

# 或手动安装MySQL和Redis
# 创建数据库hotel_management并执行infrastructure/docker/mysql/init.sql
```

#### 3. 前端开发环境
```bash
cd apps/web
pnpm install
pnpm run dev
```
前端服务将在 http://localhost:3000 启动

#### 4. 后端开发环境
```bash
cd apps/api
mvn clean install
mvn spring-boot:run
```
后端API服务将在 http://localhost:8080 启动

### Docker开发环境

```bash
# 启动完整的开发环境
cd infrastructure/docker
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 环境变量配置

创建 `.env` 文件配置环境变量：

```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=hotel_management
DB_USERNAME=root
DB_PASSWORD=password

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT配置
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400

# 应用配置
NODE_ENV=development
VITE_API_URL=http://localhost:8080/api
```

## 📖 API文档

启动后端服务后，可以访问API文档：
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## 🧪 测试

### 前端测试
```bash
cd apps/web
pnpm run test
pnpm run test:coverage
```

### 后端测试
```bash
cd apps/api
mvn test
mvn test-coverage
```

### 代码质量检查
```bash
# 前端
cd apps/web
pnpm run lint
pnpm run format

# 后端
cd apps/api
mvn checkstyle:check
```

## 🚀 部署

### 生产环境部署

1. **使用Docker部署**
```bash
# 构建镜像
docker build -t hotel-management ./infrastructure/docker

# 运行容器
docker run -d -p 8080:8080 hotel-management
```

2. **使用Docker Compose部署**
```bash
cd infrastructure/docker
docker-compose -f docker-compose.prod.yml up -d
```

3. **传统部署**
```bash
# 构建前端
cd apps/web
pnpm run build

# 构建后端
cd apps/api
mvn clean package -DskipTests

# 运行后端JAR文件
java -jar target/hotel-api-1.0.0.jar
```

## 📊 项目状态

- ✅ 项目初始化
- ✅ 基础架构搭建
- ✅ 前端项目结构
- ✅ 后端项目结构
- ✅ 数据库设计
- ✅ CI/CD配置
- 🚧 功能开发中

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 开发规范

- 前端遵循Vue3 + TypeScript最佳实践
- 后端遵循Spring Boot + RESTful API规范
- 代码提交前必须通过所有测试和代码检查
- 提交信息遵循[Conventional Commits](https://conventionalcommits.org/)规范

## 📝 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👥 团队

- **项目负责人**: -
- **前端开发**: -
- **后端开发**: -
- **UI/UX设计**: -

## 📞 联系我们

- **邮箱**: dev@hotel.com
- **问题反馈**: [GitHub Issues](https://github.com/your-repo/issues)
- **文档**: [项目Wiki](https://github.com/your-repo/wiki)

---

⭐ 如果这个项目对你有帮助，请给我们一个星标！