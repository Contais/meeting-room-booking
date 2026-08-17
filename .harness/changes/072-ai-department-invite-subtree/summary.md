# AI 邀请部门时包含启用子部门

## 需求描述

用户通过 AI 助手说“邀请研发部全员”时，研发部下存在多个子部门。当前实现只邀请
`department_id = 3` 的直属成员，未邀请子部门成员，导致结果不符合“全员”语义。

同时，`listDepartments` 工具只返回 `{id, name}`，模型无法看到部门父子关系。

## 验收标准

1. 按部门邀请时，范围为目标部门 + 其所有启用状态的后代子部门成员。
2. 禁用状态的子部门及其整棵子树不进入邀请范围。
3. 用户本身仍保持“启用状态”过滤。
4. `listDepartments` 返回结果包含 `parentId`，模型可判断部门父子关系。
5. 前端选人组件行为不变，仍为“勾选部门 = 含子部门”。

## 技术变更清单

- mrb-user：新增“计算启用部门子树 ID”能力。
- mrb-user：新增“按部门子树查询启用用户”能力。
- mrb-user：`/uc/user/internal/list-by-department` 改为返回子树成员。
- mrb-meeting：AI 工具 `listDepartments` 返回 `parentId`。
- mrb-meeting：更新 AI 工具说明与 system prompt。
- 测试：新增部门子树、子树用户查询单元测试，并补充 mrb-user 测试
  `mock-maker-subclass` 配置以匹配其他服务。

## 冲突与风险

- `/uc/user/internal/list-by-department` 为内部 Feign 接口，仅被 mrb-meeting 的 AI
  邀请流程使用，无前端或其他服务调用，语义变更影响面较小。
- 无数据库结构变更。
