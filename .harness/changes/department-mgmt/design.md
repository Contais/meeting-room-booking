x# 部门管理 - 技术方案设计

## 1. 数据库设计

### 1.1 部门表 (department)
```sql
CREATE TABLE IF NOT EXISTS `department` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    `name` VARCHAR(64) NOT NULL COMMENT '部门名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父部门ID, 0为顶级',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';
```

### 1.2 user 表添加 department_id
```sql
ALTER TABLE `user` ADD COLUMN `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID' AFTER `real_name`;
```

## 2. 后端架构

### 2.1 Entity: Department.java
- 继承风格与 User 一致
- 字段：id, name, parentId, sortOrder, status, createTime, updateTime, deleted

### 2.2 Repository: DepartmentRepository.java
- extends BaseMapper<Department>

### 2.3 DTO
- DepartmentCreateDTO: name, parentId, sortOrder
- DepartmentUpdateDTO: id, name, parentId, sortOrder

### 2.4 VO: DepartmentVO.java
- id, name, parentId, sortOrder, status, createTime
- children: List<DepartmentVO>（树形结构）

### 2.5 Service: DepartmentService.java
| 方法 | 说明 |
|------|------|
| listTree() | 查询全部，组装树形 |
| create(DepartmentCreateDTO) | 新增，校验同级名称唯一 |
| update(DepartmentUpdateDTO) | 更新，校验循环引用 |
| delete(Long id) | 删除，校验无子部门、无用户 |
| listFlat() | 简单列表（供选择） |

### 2.6 Controller: DepartmentController.java
| 接口 | 方法 | 说明 |
|------|------|------|
| GET /department/tree | listTree | 获取部门树 |
| GET /department/list | listFlat | 简单列表 |
| POST /department/admin/create | create | 新增（admin） |
| PUT /department/admin/update | update | 更新（admin） |
| DELETE /department/admin/delete/{id} | delete | 删除（admin） |

### 2.7 ErrorCode 新增
```java
DEPARTMENT_NOT_FOUND(1012, "部门不存在"),
DEPARTMENT_NAME_DUPLICATE(1013, "部门名称已存在"),
DEPARTMENT_HAS_CHILDREN(1014, "存在子部门，不允许删除"),
DEPARTMENT_HAS_USERS(1015, "部门下有用户，不允许删除"),
DEPARTMENT_CIRCULAR(1016, "不能将部门移动到其子部门下"),
```

## 3. 前端方案

### 3.1 类型: department.d.ts
```ts
export interface Department {
  id: number
  name: string
  parentId: number
  sortOrder: number
  status: number
  createTime: string
  children?: Department[]
}
```

### 3.2 API: department.ts
- getDepartmentTree()
- listDepartments()
- createDepartment(data)
- updateDepartment(data)
- deleteDepartment(id)

### 3.3 页面: DeptManage.vue
- el-table 树形展示（row-key + tree-props）
- 操作列：编辑、删除、添加子部门
- 新增/编辑对话框：名称、父部门（el-tree-select）、排序号
- 风格与 UserManage 一致

### 3.4 路由 + 菜单
- 路由：admin/departments
- 菜单：管理员可见

### 3.5 UserManage 关联
- 新增/编辑对话框添加部门选择（el-tree-select）
