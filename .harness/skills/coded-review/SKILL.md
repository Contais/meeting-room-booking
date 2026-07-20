# 代码审查技能 (Code Review)

## 概述
从安全性、性能、可维护性三个维度对代码进行自动化与智能化审查，拦截低级错误和反模式。

## 触发条件
当 Pull Request (PR) 创建或更新时触发。

## 审查维度

### 1. 安全红线检查 (Security Check)
- **SQL 注入**：检查是否使用了字符串拼接 SQL，必须使用 MyBatis-Plus 参数化查询。
- **XSS 防护**：检查 Controller 返回数据是否进行了 HTML 转义，前端 Vue 默认已防 XSS，需确认后端未破坏。
- **敏感信息泄露**：检查日志打印中是否包含 Password、Token、手机号等敏感字段。
- **权限校验**：检查 Controller 层是否遗漏 `@PreAuthorize` 或相关权限注解。

### 2. 性能与规范检查 (Performance & Style)
- **N+1 查询问题**：检查 MyBatis-Plus 中是否在循环中调用单个查询，必须使用 `in` 批量查询。
- **数据库索引**：检查 WHERE 条件、JOIN 字段是否建立了索引。
- **大事务风险**：检查 `@Transactional` 标注的方法是否包含远程调用（Feign/RPC），防止长事务。
- **资源未释放**：检查 IO 流、数据库连接是否在 finally 块或 try-with-resources 中关闭。

### 3. 代码坏味道检查 (Code Smell)
- **魔法值**：检查代码中是否直接出现未经定义的常量（如 `if (status == 1)`），必须定义枚举。
- **过长的参数列表**：检查方法参数是否超过 3 个，超过则必须封装为 DTO 或 Builder 模式。
- **重复代码**：识别相似的代码片段（DRY 原则），提示提取为公共方法。
- **注释缺失**：检查 public 方法是否缺少 JavaDoc 注释，解释入参和返回值含义。

## 输出
生成审查报告，列出 `ERROR`（必须修改）、`WARNING`（建议修改）、`INFO`（提示信息）三级分类。