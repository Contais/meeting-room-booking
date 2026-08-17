# AI 邀请部门包含启用子部门 — 技术方案

## 背景

部门表 `uc_department` 通过 `parent_id` 表达树形结构。当前 AI 工具
`listDepartments` 使用扁平列表并丢弃 `parentId`，同时 `inviteDepartment` 通过内部
接口按单一 `department_id` 查询用户，只覆盖直属成员。

## 方案

### 1. 启用部门子树

在 mrb-user 的 `DepartmentService` 中新增：

```java
Set<Long> listEnabledDescendantIds(Long departmentId);
```

实现时只加载启用部门，构建 `parentId -> children` 映射，从目标部门开始 BFS。
禁用节点不进入队列，从而排除禁用子部门及其整棵子树。使用 `visited` 防止环。

### 2. 按子树查询用户

在 mrb-user 的 `UserService` 中新增：

```java
List<UserVO> listContactsByDepartmentTree(Long departmentId);
```

先计算启用子树 ID，再通过 `LambdaQueryWrapper` 查询：

```java
deleted = 0
status = 1
department_id IN (subtreeIds)
```

排序保持 `department_id ASC, username ASC`。

### 3. 内部接口语义

`UserInternalController.listByDepartment` 改为调用
`listContactsByDepartmentTree`，Feign 路径保持不变。

### 4. AI 工具结果

`ReservationToolResults.DepartmentBrief` 增加 `parentId`，`ReservationTool.listDepartments`
映射该字段，使模型能够根据 `parentId` 判断部门层级。

## 边界

- 仅启用部门及其启用后代进入邀请范围。
- 目标部门不存在或禁用时，返回空成员列表。
- 用户查询仍过滤 `status = 1` 和 `deleted = 0`。
