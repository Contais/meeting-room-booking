# 聊天机器人 - 需求分析

## 需求描述
在会议室预约系统中集成 AI 聊天机器人，使用 Spring AI + DeepSeek 模型，支持流式输出（SSE）和 Function Calling（AI 可调用后端接口操作业务）。

## 核心功能
| 功能 | 说明 |
|------|------|
| 流式对话 | AI 回答逐字输出（SSE），前端实时渲染 |
| 通用问答 | 回答任何问题（闲聊、知识问答） |
| 业务操作 | 通过 Function Calling 查询会议室、查询预约、创建预约等 |
| 浮动面板 | 页面右下角悬浮按钮，点击展开聊天面板 |
| 会话上下文 | 维护当前会话的对话历史（内存中，刷新清空） |

## 验收标准
| 编号 | 验收条件 |
|------|----------|
| AC-1 | 用户点击右下角悬浮按钮展开聊天面板 |
| AC-2 | 输入消息后，AI 以流式方式逐字返回回答 |
| AC-3 | AI 能回答通用问题（如"你好"、"今天天气怎么样"） |
| AC-4 | AI 能查询会议室："有哪些会议室可用？" → 调用查询接口返回结果 |
| AC-5 | AI 能查询预约："帮我看看今天的预约" → 调用查询接口 |
| AC-6 | AI 能创建预约："帮我预约大会议室A，明天下午2-3点" → 调用创建接口 |
| AC-7 | 创建预约前需用户确认（Function Calling 结果展示后询问是否执行） |
| AC-8 | 流式输出过程中可随时中断（Stop 按钮） |
| AC-9 | 新建对话按钮可清空当前会话 |
| AC-10 | 未登录用户也可使用（仅通用问答，不可操作业务） |

## 技术变更
| 变更项 | 说明 |
|--------|------|
| Spring AI 依赖 | 新增 spring-ai-starter、spring-ai-starter-mcp-client 或 spring-ai-open-ai-spring-boot-starter |
| DeepSeek 集成 | 配置 DeepSeek API（兼容 OpenAI 协议） |
| ChatService | 新增聊天服务，管理对话上下文 + Function Calling |
| Function Tools | 定义业务操作函数（查询会议室、查询预约、创建预约） |
| ChatController | SSE 流式接口 |
| ChatView.vue | 浮动聊天面板组件 |
| ChatMessage | 消息实体（可选持久化） |

## 涉及模块
- mrb-common（新增 Spring AI 依赖）
- 新模块或 mrb-meeting（ChatService + Function Tools）
- frontend（聊天面板组件）

## 完成时间
2026-07-24
