# 贡献指南

感谢您对成都酒店管理系统项目的关注！我们欢迎所有形式的贡献，包括但不限于：

- 🐛 报告 Bug
- 💡 提出新功能建议
- 📝 改进文档
- 🔧 提交代码修复
- ✨ 开发新功能

## 🚀 开始贡献

### 1. 环境准备

在开始贡献之前，请确保您已经：

- 阅读了项目的 [README.md](README.md)
- 搭建了本地开发环境（参考 [开发环境搭建指南](docs/development-setup.md)）
- Fork 了项目到您的 GitHub 账户

### 2. Fork 和克隆

```bash
# Fork 项目到您的账户，然后克隆
git clone https://github.com/your-username/chengdu-hotel-management.git
cd chengdu-hotel-management

# 添加上游仓库
git remote add upstream https://github.com/original-username/chengdu-hotel-management.git
```

## 📋 贡献流程

### 1. 创建 Issue

在开始编写代码之前，建议先创建一个 Issue 来讨论：

- **Bug 报告**: 使用 Bug 报告模板
- **功能请求**: 使用功能请求模板
- **问题讨论**: 简单描述您想解决的问题

### 2. 创建分支

```bash
# 确保在最新的主分支
git checkout main
git pull upstream main

# 创建功能分支
git checkout -b feature/your-feature-name

# 或者修复分支
git checkout -b fix/your-bug-fix
```

### 3. 开发和提交

在开发过程中，请遵循以下规范：

#### 代码规范

**前端代码 (Vue3 + TypeScript):**
```typescript
// 使用 PascalCase 命名组件
export default defineComponent({
  name: 'UserProfile'
})

// 使用 camelCase 命名方法和变量
const getUserData = async () => {
  // 方法实现
}

// 使用 PascalCase 命名接口和类型
interface UserResponse {
  id: number
  name: string
}
```

**后端代码 (Java + Spring Boot):**
```java
// 使用 camelCase 命名方法和变量
@RestController
public class UserController {

    public ResponseEntity<Result<User>> getUser(@PathVariable Long id) {
        // 方法实现
    }
}

// 使用 PascalCase 命名类
public class UserService {
    // 类实现
}
```

#### 提交规范

使用 [Conventional Commits](https://conventionalcommits.org/) 规范：

```bash
# 功能提交
git commit -m "feat: add user authentication system"

# Bug 修复
git commit -m "fix: resolve login validation issue"

# 文档更新
git commit -m "docs: update API documentation"

# 样式调整
git commit -m "style: improve component layout"

# 重构代码
git commit -m "refactor: optimize database queries"

# 性能优化
git commit -m "perf: improve page load speed"

# 测试相关
git commit -m "test: add unit tests for user service"
```

### 4. 代码质量检查

提交前请确保：

```bash
# 前端代码检查
cd apps/web
pnpm run lint
pnpm run format
pnpm run test

# 后端代码检查
cd apps/api
mvn checkstyle:check
mvn test
```

### 5. 创建 Pull Request

```bash
# 推送到您的 Fork
git push origin feature/your-feature-name

# 在 GitHub 上创建 Pull Request
```

#### PR 要求

- 使用清晰的标题和描述
- 引用相关的 Issue
- 包含测试用例
- 更新相关文档
- 通过所有 CI 检查

## 🐛 Bug 报告

使用以下模板报告 Bug：

```markdown
**Bug 描述**
简要描述遇到的问题

**复现步骤**
1. 进入 '...'
2. 点击 '....'
3. 滚动到 '....'
4. 看到错误

**期望行为**
描述您期望发生的情况

**实际行为**
描述实际发生的情况

**截图**
如果适用，添加截图来帮助解释问题

**环境信息**
- 操作系统: [例如 iOS]
- 浏览器: [例如 chrome, safari]
- 版本: [例如 22]

**附加信息**
添加任何其他关于问题的信息
```

## 💡 功能请求

使用以下模板提出新功能：

```markdown
**功能描述**
简要描述您希望添加的功能

**问题背景**
描述这个功能要解决的问题

**解决方案**
描述您的解决方案

**替代方案**
描述您考虑过的其他解决方案

**附加信息**
添加任何其他关于功能请求的信息
```

## 📝 文档贡献

我们欢迎以下类型的文档贡献：

- API 文档改进
- 教程和指南
- 代码注释
- README 更新
- 架构图和流程图

## 🏷️ 标签使用

在 Issue 和 PR 中使用以下标签：

- `bug`: Bug 报告
- `enhancement`: 功能增强
- `documentation`: 文档相关
- `good first issue`: 适合新贡献者
- `help wanted`: 需要帮助
- `priority/high`: 高优先级
- `priority/medium`: 中优先级
- `priority/low`: 低优先级

## 🤝 行为准则

### 我们的承诺

为了营造一个开放和友好的环境，我们承诺：

- 使用友好和包容的语言
- 尊重不同的观点和经验
- 优雅地接受建设性批评
- 关注对社区最有利的事情
- 对其他社区成员表示同理心

### 不可接受的行为

- 使用性暗示的语言或图像
- 人身攻击或政治攻击
- 公开或私下骚扰
- 未经明确许可发布他人的私人信息
- 其他在专业环境中可能被认为不当的行为

## 📧 联系我们

如果您有任何问题或需要帮助，可以通过以下方式联系我们：

- **Email**: dev@hotel.com
- **GitHub Issues**: [项目 Issues 页面](https://github.com/your-repo/issues)
- **讨论区**: [GitHub Discussions](https://github.com/your-repo/discussions)

## 🙏 致谢

感谢所有为项目做出贡献的开发者！

---

💡 请记住，即使是小的贡献也是有价值的。我们期待您的参与！