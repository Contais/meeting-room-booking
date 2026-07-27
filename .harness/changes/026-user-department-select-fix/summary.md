# 用户管理选择部门报错修复

## 问题现象

用户管理页面新增/编辑用户时，选择部门后会报错。

## 根因分析

### 问题1：el-tree-select 缺少 node-key 属性

所有 `el-tree-select` 组件使用 `:props="{ label: 'name', value: 'id', children: 'children' }"` 配置，
但 Element Plus 的 `el-tree` 组件的 `props` 不支持 `value` 字段。值字段应通过 `node-key` 属性指定。

**影响**：不设 `node-key` 时，el-tree-select 可能使用整个节点对象作为 v-model 值，
导致 `departmentId` 变成对象而非数字/字符串，提交时后端反序列化失败。

### 问题2：后端 Long 序列化为 String

`JacksonConfig.java` 配置了 `Long.class → ToStringSerializer`，所有 Long 字段以字符串返回。
前端 `Department.id` 类型定义为 `number`，但实际收到的是 `string`。

**验证**：后端 API 测试确认 `departmentId` 无论是数字 `3` 还是字符串 `"3"` 都能正常创建用户，
Jackson 可正确反序列化字符串到 Long。

## 修复方案

为所有 `el-tree-select` 组件添加 `node-key="id"` 属性，移除 `props` 中无效的 `value` 字段：

```html
<!-- 修复前 -->
<el-tree-select :props="{ label: 'name', value: 'id', children: 'children' }" />

<!-- 修复后 -->
<el-tree-select node-key="id" :props="{ label: 'name', children: 'children' }" />
```

## 技术变更清单

| 文件 | 说明 |
|------|------|
| `frontend/src/views/admin/UserManage.vue` | el-tree-select 添加 node-key |
| `frontend/src/views/admin/UserDetail.vue` | el-tree-select 添加 node-key |
| `frontend/src/views/admin/MenuManage.vue` | el-tree-select 添加 node-key |
| `frontend/src/views/admin/DeptManage.vue` | el-tree-select 添加 node-key |

## 验证

- TypeScript 编译通过
- 后端 API 测试：departmentId 为数字和字符串均可正常创建用户
- 浏览器测试：控制台无红色错误
