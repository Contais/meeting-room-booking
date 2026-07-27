# 菜单管理「是否启用」状态修改无效修复

## 需求描述
菜单管理 > 添加/编辑 > 「是否启用」状态修改无效。前端请求参数缺少 `status` 字段，导致后端 `MenuUpdateDTO.status` 为 `null`，`MenuServiceImpl.update` 中 `if (dto.getStatus() != null)` 条件不成立，状态未被更新。

实际请求示例（缺少 status）：
```json
{"id":"11","name":"部门管理","path":"/admin/departments","icon":"Menu","parentId":"10","sortOrder":11,"visible":1}
```

## 根因分析
1. **`handleSubmit` 未传 `status`**：`updateMenu` / `createMenu` 调用中只传了 `visible`，遗漏 `status` 字段
2. **`showEditDialog` parentId 比较缺陷**：后端 `JacksonConfig` 将 `Long` 序列化为 `String`（防 JS 精度丢失），导致 `row.parentId` 为字符串 `"0"`，`row.parentId === 0` 严格相等比较失败，顶级菜单的 `parentId` 无法被正确识别为 `undefined`

## 验收标准
1. 添加/编辑菜单提交请求体必须包含 `status` 字段（值为 0 或 1）
2. 编辑顶级菜单（parentId=0）时，上级菜单选择框显示为「留空则为顶级菜单」
3. 切换「是否启用」状态后，列表中状态标签正确更新
4. 前端 `vite build` 通过

## 技术变更清单

### 修改
| 文件 | 变更 |
|------|------|
| `frontend/src/views/admin/MenuManage.vue` | `showEditDialog`：parentId 比较改用 `Number(row.parentId) === 0`，兼容字符串 `"0"`；`handleSubmit`：构造统一 `payload`，显式添加 `status: Number(form.status ?? 1)` 与 `visible: Number(form.visible ?? 1)`，确保字段始终发送 |

## 关键代码
```ts
// handleSubmit 修复前（缺失 status）
await updateMenu({ id: form.id!, name: form.name, path: form.path, icon: form.icon,
  parentId: form.parentId || 0, sortOrder: form.sortOrder, visible: form.visible })

// handleSubmit 修复后（统一 payload，显式 Number 转换 + 默认值）
const payload = {
  name: form.name, path: form.path, icon: form.icon,
  parentId: form.parentId || 0, sortOrder: form.sortOrder,
  status: Number(form.status ?? 1),
  visible: Number(form.visible ?? 1)
}
await updateMenu({ id: form.id!, ...payload })
```

```ts
// showEditDialog 修复前（严格相等，字符串 "0" 无法命中）
parentId: row.parentId === 0 ? undefined : row.parentId

// showEditDialog 修复后（Number 转换 + 空值兜底）
const pidNum = Number(row.parentId)
parentId: !row.parentId || pidNum === 0 ? undefined : row.parentId
```

## 冲突与风险
- 纯前端修复，无后端/DB 变更
- `Number(form.status ?? 1)` 保证 status 始终为数字（0 或 1），避免 `undefined` 被 `JSON.stringify` 忽略
- 不影响其它菜单字段（name/path/icon/parentId/sortOrder/visible）的提交

## 提交信息
`fix(menu): 修复菜单添加/编辑「是否启用」状态未提交导致修改无效`
