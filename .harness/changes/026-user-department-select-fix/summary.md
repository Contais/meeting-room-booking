# 新增/编辑用户选择部门后报错

## 需求描述

在「用户管理」页面新增或编辑用户时，选择「所属部门」后会报错。需定位根因并修复。

## 现状分析（静态代码审查）

### 前端
- `UserManage.vue` 与 `UserDetail.vue` 使用 `el-tree-select`：
  ```html
  <el-tree-select v-model="form.departmentId" :data="deptTree"
    :props="{ label: 'name', value: 'id', children: 'children' }"
    check-strictly clearable ... />
  ```
- 该配置与 `DeptManage.vue`、`MenuManage.vue` 中「上级部门/上级菜单」完全一致，且后两者工作正常 → el-tree-select 配置本身基本可排除
- 部门树数据来自 `getDepartmentTree()` → `GET /api/uc/department/tree`，后端 `DepartmentServiceImpl.listTree()` 构建树结构正确（含 `children`）

### 后端
- `UserCreateDTO` / `UserUpdateDTO` 均含 `Long departmentId` 字段
- `UserServiceImpl.createUser` / `updateUser` 正确 `setDepartmentId(dto.getDepartmentId())`
- `user` 表 `department_id` 列存在（init.sql + V1.2），无外键约束

### 错误显示路径
- `utils/request.ts` 响应拦截器：后端返回 `code !== 200` 或 HTTP 非 2xx 时，弹出 `ElMessage.error(message)` —— 这即是用户可见的「报错」来源
- 前端 `handleSubmit` 的 `catch {}` 静默吞错，但拦截器已先行弹出 ElMessage

## 根因假设（按概率排序，需运行时确认）

| # | 假设 | 说明 | 验证方式 |
|---|------|------|---------|
| 1 | departmentId 值类型异常 | el-tree-select 在某些边界场景下发出非 number 值（对象/数组），导致后端 Jackson 反序列化 `Long` 失败 → 400 | 查看 Network 请求体 `departmentId` 的实际类型 |
| 2 | 后端业务异常 | 提交时触发的异常（如手机号唯一约束 `uk_phone_active`、角色校验等）恰好与选部门同时发生 | 查看响应 body 的 `message` 与状态码 |
| 3 | 部门树数据异常 | 某部门节点 `id` 缺失/类型异常，选中即触发 JS 错误 | Console 查看选中瞬间的 JS 异常 |

> 注：因 el-tree-select 配置与可用模板一致，假设 3 概率较低；重点验证假设 1、2

## 验收标准

| AC | 描述 |
|----|------|
| AC-1 | 新增用户时选择任意部门并提交，不再报错，用户创建成功且 department_id 正确入库 |
| AC-2 | 编辑用户时切换/清空部门并提交，不再报错，department_id 正确更新 |
| AC-3 | 不选部门（留空）提交仍正常工作（回归不破坏） |
| AC-4 | departmentId 始终以 number 类型传输 |

## 验证计划（需在浏览器执行）

1. 登录 admin → /admin/users → 点「新增用户」
2. 填写用户名/密码/角色，选择一个部门
3. 打开 Console + Network，点确定提交
4. 记录：Console JS 错误、Network 请求体 `departmentId` 字段类型、响应状态码与 body
5. 编辑已有用户切换部门，重复记录 `/api/uc/user/admin/update` 的请求体与响应

## 修复方向（待确认根因后实施）

- 若假设 1：在提交前 `form.departmentId = form.departmentId ? Number(form.departmentId) : undefined` 强制数值化；或检查 el-tree-select 是否误开 `multiple`
- 若假设 2：根据后端 message 修正对应校验/约束
- 若假设 3：修正部门树数据构建

## 冲突与风险

- 风险：在未确认根因前避免盲目改动 el-tree-select 配置（DeptManage/MenuManage 共用同模式，改动可能波及）
- 红线检查：无违反

## 任务拆分建议

1. 浏览器复现，定位确切错误（请求体 + 响应）
2. 按根因实施修复
3. 回归：新增/编辑/清空部门三种场景
