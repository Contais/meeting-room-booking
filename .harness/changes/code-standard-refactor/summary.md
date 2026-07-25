# 后端代码规范重构 - 阿里巴巴Java编码规范

## 需求描述
按照阿里巴巴Java编码规范，对整个后端代码进行全面重构，重点解决：
1. 魔法数字问题（状态码 0/1/2 硬编码）
2. Javadoc 注解位置不规范
3. 代码行超过120字符
4. 重复常量定义

## 验收标准
1. 所有状态码使用枚举替代魔法数字
2. Javadoc 注解放在注解之前
3. 代码行宽不超过120字符
4. 无重复常量定义

## 技术变更清单

### 1. 创建枚举类（mrb-common）

#### 1.1 ReservationStatusEnum.java（预约状态）
- PENDING(0, "待确认")
- CONFIRMED(1, "已确认")
- CANCELLED(2, "已取消")

#### 1.2 EnableStatusEnum.java（启用/禁用状态）
- DISABLED(0, "禁用")
- ENABLED(1, "启用")

#### 1.3 DeletedEnum.java（删除标记）
- NOT_DELETED(0, "未删除")
- DELETED(1, "已删除")

#### 1.4 ApprovalModeEnum.java（审批模式）
- FREE_APPROVAL(0, "免审批")
- NEED_APPROVAL(1, "需审批")

#### 1.5 VisibleEnum.java（菜单可见性）
- HIDDEN(0, "隐藏")
- VISIBLE(1, "显示")

### 2. 修复魔法数字（按文件）

#### 2.1 ReservationServiceImpl.java
- 第50行: `room.getStatus() == 0` → `EnableStatusEnum.DISABLED.getCode()`
- 第78行: 三元表达式使用枚举
- 第130, 133行: 状态2 → `ReservationStatusEnum.CANCELLED`
- 第191, 259, 270, 277行: 同上
- 第309行: 状态1 → `EnableStatusEnum.ENABLED`
- 第314行: 同上

#### 2.2 MeetingRoomServiceImpl.java
- 第34, 102, 134行: 状态1/0 → `EnableStatusEnum`
- 第101行: needApproval 0 → `ApprovalModeEnum.FREE_APPROVAL`

#### 2.3 HomeServiceImpl.java
- 第38, 84行: 状态1 → `EnableStatusEnum.ENABLED`
- 第47, 54, 61, 69, 91, 117行: 状态2/0 → `ReservationStatusEnum`

#### 2.4 UserServiceImpl.java
- 第56, 74, 129, 155行: deleted 0 → `DeletedEnum.NOT_DELETED`
- 第85, 142, 174行: status 1/0 → `EnableStatusEnum`

#### 2.5 DepartmentServiceImpl.java
- 第39, 56, 59, 82, 88, 141行: parentId 0L → 常量
- 第46, 71行: status 1 → `EnableStatusEnum.ENABLED`

#### 2.6 MenuServiceImpl.java
- 第34, 54, 60, 61, 84行: parentId 0L → 常量
- 第49, 72行: status 1 → `EnableStatusEnum.ENABLED`
- 第50行: visible 1 → `VisibleEnum.VISIBLE`
- 第71行: 默认 visible 1 → `VisibleEnum.VISIBLE`

#### 2.7 AuthServiceImpl.java
- 第40行: status 0 → `EnableStatusEnum.DISABLED`

#### 2.8 MeetingRoomTools.java
- 第30, 49, 81行: status 1 → `EnableStatusEnum.ENABLED`
- 第58, 88行: status 2 → `ReservationStatusEnum.CANCELLED`
- 第67行: status 0 → `ReservationStatusEnum.PENDING`

### 3. 修复 Javadoc 注解位置
将所有 `@Service /** javadoc */` 改为 `/** javadoc */ @Service`

### 4. 修复重复常量
- `AuthGlobalFilter.java`: 使用 `CommonConstant.TOKEN_HEADER`

### 5. 修复超长行
- `UserServiceImpl.java`: 拆分 LambdaQueryWrapper 链式调用
- `SpringAIConfiguration.java`: 使用 import 替代全限定类名

## 涉及文件

### 新建文件
| 文件 | 说明 |
|------|------|
| `mrb-common/src/main/java/com/meetinghub/common/enums/ReservationStatusEnum.java` | 预约状态枚举 |
| `mrb-common/src/main/java/com/meetinghub/common/enums/EnableStatusEnum.java` | 启用/禁用状态枚举 |
| `mrb-common/src/main/java/com/meetinghub/common/enums/DeletedEnum.java` | 删除标记枚举 |
| `mrb-common/src/main/java/com/meetinghub/common/enums/ApprovalModeEnum.java` | 审批模式枚举 |
| `mrb-common/src/main/java/com/meetinghub/common/enums/VisibleEnum.java` | 菜单可见性枚举 |

### 修改文件
| 文件 | 说明 |
|------|------|
| `ReservationServiceImpl.java` | 替换魔法数字 |
| `MeetingRoomServiceImpl.java` | 替换魔法数字 |
| `HomeServiceImpl.java` | 替换魔法数字 |
| `UserServiceImpl.java` | 替换魔法数字 + 修复超长行 |
| `DepartmentServiceImpl.java` | 替换魔法数字 |
| `MenuServiceImpl.java` | 替换魔法数字 |
| `AuthServiceImpl.java` | 替换魔法数字 |
| `MeetingRoomTools.java` | 替换魔法数字 |
| `AuthGlobalFilter.java` | 使用 CommonConstant |
| `SpringAIConfiguration.java` | 使用 import + 修复 Javadoc |
| 所有 Controller/Service/DTO | 修复 Javadoc 注解位置 |

## 提交信息
`refactor: 后端代码规范重构 - 使用枚举替代魔法数字 + 修复 Javadoc 注解位置`
