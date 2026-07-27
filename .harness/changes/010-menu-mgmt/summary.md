# 菜单管理 - 需求分析

## 需求描述
实现动态菜单管理功能，管理员可配置侧边栏菜单项（增删改查、排序、显示/隐藏），侧边栏从数据库动态加载。

## 验收标准
| 编号 | 验收条件 |
|------|----------|
| AC-1 | 管理员可查看菜单树形列表，支持展开/折叠 |
| AC-2 | 管理员可新增菜单项（名称、路由路径、图标、父菜单、排序、是否显示） |
| AC-3 | 管理员可编辑菜单项 |
| AC-4 | 管理员可删除菜单项（需无子菜单） |
| AC-5 | 侧边栏根据数据库菜单动态渲染 |
| AC-6 | 菜单支持多级（树形）结构 |
| AC-7 | 隐藏的菜单项不在侧边栏显示 |

## 技术变更
| 变更项 | 文件 | 说明 |
|--------|------|------|
| menu 表 | 新建 | 菜单表（树形结构） |
| Menu Entity | 新建 | 菜单实体 |
| MenuRepository | 新建 | MyBatis-Plus Mapper |
| MenuCreateDTO/MenuUpdateDTO | 新建 | 请求 DTO |
| MenuVO | 新建 | 返回 VO（含 children） |
| MenuService | 新建 | 菜单服务 |
| MenuController | 新建 | 菜单接口 |
| ErrorCode | 修改 | 新增菜单相关错误码 |
| MainLayout.vue | 修改 | 从 API 动态加载菜单 |
| MenuManage.vue | 新建 | 菜单管理页面 |
| router/index.ts | 修改 | 添加菜单管理路由 |
| menu.ts | 新建 | 前端 API |
| menu.d.ts | 新建 | TypeScript 类型 |

## 冲突与风险
| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 删除有关联子菜单的菜单 | 数据不一致 | 删除前校验无子菜单 |
| 菜单路径与路由不匹配 | 页面无法访问 | 管理员自行维护路径正确性 |
| 默认菜单丢失 | 侧边栏为空 | init.sql 预置默认菜单数据 |

## 涉及模块
- mrb-common（ErrorCode）
- mrb-meeting（Menu Entity/Service/Controller，菜单属于系统配置）
- frontend（页面/API/类型/路由/布局）

## 完成时间
2026-07-24
