# 数据统计 + 日程视图 - 技术方案

## 一、数据统计

### 1.1 后端接口

**HomeController 新增接口**：

| 接口 | 说明 |
|------|------|
| GET /home/stats | 首页统计（今日/本周/总预约数、待审批数） |
| GET /home/room-usage | 会议室使用率（今日各会议室使用时长/可用时长） |
| GET /home/peak-hours | 高峰时段分布（各时段预约数量） |

**VO 设计**：
```java
// 首页统计
HomeStatsVO: todayCount, weekCount, totalCount, pendingCount

// 会议室使用率
RoomUsageVO: roomId, roomName, usedMinutes, totalMinutes, usageRate

// 高峰时段
PeakHourVO: hour, count
```

### 1.2 SQL 查询

- 今日/本周/总预约数：`meeting_room_reservation` 按时间范围 COUNT
- 待审批数：`status=0` 的 COUNT
- 会议室使用率：每个会议室今日已预约时长 / 可用总时长
- 高峰时段：按 `start_time` 的小时 GROUP BY COUNT

## 二、日程视图

### 2.1 后端接口

| 接口 | 说明 |
|------|------|
| GET /reservation/schedule?date=2026-07-24&view=day | 日视图数据 |
| GET /reservation/schedule?startDate=2026-07-20&endDate=2026-07-26&view=week | 周视图数据 |

**返回数据**：
```json
{
  "rooms": [{ "id": 1, "name": "大会议室A", "capacity": 20 }],
  "reservations": [
    { "id": 1, "roomId": 1, "subject": "项目评审", "startTime": "...", "endTime": "...", "status": 1, "userName": "张三" }
  ]
}
```

### 2.2 前端 ScheduleView.vue

- 使用 CSS Grid 布局
- 纵轴：时间刻度（8:00-20:00，每30分钟一行）
- 横轴：会议室（日视图）或日期（周视图）
- 预约色块：绝对定位，根据 start/end time 计算 top/height
- 支持日/周切换、前后翻页
- 点击色块弹出预约详情

### 2.3 路由 + 菜单
- 路由：`schedule`（需登录）
- 菜单：添加到顶级菜单（用户可见）

## 三、图表库选择
使用 ECharts（轻量、Vue 3 支持好），通过 `vue-echarts` 组件集成。
