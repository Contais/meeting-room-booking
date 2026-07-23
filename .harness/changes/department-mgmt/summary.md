# 部门管理 - 需求分析

## 需求描述
实现树形部门管理功能，支持多级父子部门结构。管理员可对部门进行增删改查，用户可归属到部门。

## 验收标准
| 编号 | 验收条件 |
|------|----------|
| AC-1 | 管理员可查看部门树形列表，支持展开/折叠 |
| AC-2 | 管理员可新增顶级部门 |
| AC-3 | 管理员可新增子部门（选择父部门） |
| AC-4 | 管理员可编辑部门名称、排序号、父部门 |
| AC-5 | 管理员可删除部门（需满足：无子部门、无关联用户） |
| AC-6 | 新增/编辑部门时校验同级部门名称唯一 |
| AC-7 | 用户管理新增/编辑时可选择所属部门 |
| AC-8 | 部门禁用后不影响已关联用户，但新用户不可选择该部门 |

## 技术变更
| 变更项 | 文件 | 说明 |
|--------|------|------|
| department 表 | 新建 | 树形结构（parent_id），支持多级 |
| user.department_id | 新增字段 | 关联部门 |
| Department Entity | 新建 | 部门实体 |
| DepartmentRepository | 新建 | MyBatis-Plus Mapper |
| DepartmentCreateDTO | 新建 | 新增请求参数 |
| DepartmentUpdateDTO | 新建 | 更新请求参数 |
| DepartmentVO | 新建 | 返回视图对象（含 children） |
| DepartmentService | 新建 | 部门服务接口 |
| DepartmentServiceImpl | 新建 | 部门服务实现 |
| DepartmentController | 新建 | 部门管理接口 |
| ErrorCode | 修改 | 新增部门相关错误码 |
| UserCreateDTO | 修改 | 添加 departmentId |
| UserUpdateDTO | 修改 | 添加 departmentId |
| UserVO | 修改 | 添加 departmentId/Name |
| User entity | 修改 | 添加 departmentId 字段 |
| UserServiceImpl | 修改 | 创建/编辑时保存 departmentId |
| DeptManage.vue | 新建 | 部门管理页面 |
| department.ts | 新建 | 前端 API |
| department.d.ts | 新建 | TypeScript 类型 |
| router/index.ts | 修改 | 添加部门管理路由 |
| MainLayout.vue | 修改 | 添加菜单项 |
| UserManage.vue | 修改 | 添加部门选择 |

## 冲突与风险
| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 删除部门时有关联用户 | 数据不一致 | 删除前校验无关联用户 |
| 移动部门导致循环引用 | 树结构损坏 | 更新时校验目标父节点不是当前节点的后代 |
| 同级部门名称重复 | 数据混乱 | 创建/更新时校验同级唯一性 |

## 涉及模块
- mrb-common（ErrorCode）
- mrb-user（Entity/Service/Controller）
- frontend（页面/API/类型/路由）

## 完成时间
2026-07-24
