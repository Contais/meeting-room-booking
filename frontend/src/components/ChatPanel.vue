<template>
  <div class="chat-float">
    <transition name="chat-slide">
      <div v-if="visible" class="chat-panel">
        <div class="chat-header">
          <span class="chat-title">AI 助手</span>
          <div class="chat-actions">
            <el-button text size="small" @click="clearChat"><el-icon><Delete /></el-icon></el-button>
            <el-button text size="small" @click="togglePanel"><el-icon><Close /></el-icon></el-button>
          </div>
        </div>

        <div class="chat-messages" ref="messagesContainer">
          <div v-if="messages.length === 0" class="chat-empty">
            <el-icon :size="32" color="#c0c4cc"><ChatDotRound /></el-icon>
            <p>你好！我是 AI 助手，可以帮你查询会议室、查看预约等。</p>
          </div>
          <div v-for="(msg, idx) in messages" :key="idx" class="chat-msg" :class="msg.role">
            <div
              class="msg-bubble"
              :class="{ 'thinking-bubble': msg.role === 'assistant' && !msg.content && loading && idx === messages.length - 1 }"
            >
              <div v-if="msg.role === 'user'" class="msg-text">{{ msg.content }}</div>
              <div v-else class="msg-text markdown-content">
                <template v-if="msg.content">
                  <div v-html="renderMarkdown(msg.content)"></div>
                  <span v-if="loading && idx === messages.length - 1" class="streaming-cursor"></span>
                </template>
                <div v-else-if="loading && idx === messages.length - 1" class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
            <div v-if="msg.role === 'assistant' && msg.content && getSuggestions(msg.content).length" class="suggest-chips">
              <el-button
                v-for="(s, si) in getSuggestions(msg.content)"
                :key="si"
                size="small"
                round
                @click="sendSuggestion(s)"
              >{{ s }}</el-button>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <div class="input-wrapper">
            <el-input
              v-model="inputText"
              placeholder="输入消息，按 Enter 发送..."
              :disabled="loading"
              @keydown.enter="onEnterPress"
              @compositionstart="onCompositionStart"
              @compositionend="onCompositionEnd"
              class="chat-input-field"
            />
            <el-button
              v-if="!loading"
              class="send-btn"
              type="primary"
              :icon="Promotion"
              circle
              :disabled="!inputText.trim()"
              @click="sendMessage"
            />
            <el-button
              v-else
              class="send-btn stop-btn"
              :icon="VideoPause"
              circle
              @click="stopGenerating"
            />
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ChatDotRound, Close, Delete, Promotion, VideoPause } from '@element-plus/icons-vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { useUserStore } from '@/stores/user'

marked.setOptions({ breaks: true, gfm: true })

const userStore = useUserStore()
const visible = ref(false)
const inputText = ref('')
const loading = ref(false)
const messages = ref([])
const messagesContainer = ref(null)
const sessionId = ref('')
const hasStreamingContent = ref(false)
// 中文输入法 composition 状态：true 表示正在选字，回车仅用于确认候选词，不应发送
const isComposing = ref(false)

let abortController = null

function onCompositionStart() {
  isComposing.value = true
}

function onCompositionEnd() {
  isComposing.value = false
}

/**
 * 回车发送：composition 中（中文输入法选字阶段）不发送，
 * 仅在 composition 结束后才允许 Enter 触发发送。
 * 使用 keydown 而非 keyup，避免候选词上屏与发送事件的时序错乱。
 */
function onEnterPress(e) {
  if (isComposing.value) return
  e.preventDefault()
  sendMessage()
}

async function togglePanel() {
  visible.value = !visible.value
  // 打开窗口时默认滚动到最底部消息
  if (visible.value) {
    await nextTick()
    scrollToBottom()
  }
}

/**
 * 从 AI 回复中解析引导提问块（:::suggest ... :::），返回建议列表
 */
function getSuggestions(content) {
  if (!content) return []
  const match = content.match(/:::suggest\n([\s\S]*?)\n:::/)
  if (!match) return []
  return match[1].split('\n').map(s => s.trim()).filter(Boolean)
}

function renderMarkdown(text) {
  if (!text) return ''
  let cleaned = text.replace(/<system-reminder>[\s\S]*?<\/system-reminder>/g, '')
  cleaned = cleaned.replace(/<system-reminder>[\s\S]*$/, '')
  // 移除引导提问块，不在正文渲染
  cleaned = cleaned.replace(/:::suggest\n[\s\S]*?\n:::/g, '').trim()
  const html = marked.parse(cleaned)
  // FORBID_TAGS: ['del'] 禁用删除线（~~text~~），避免 AI 输出的 ~~ 被误渲染
  return DOMPurify.sanitize(html, {
    ADD_TAGS: ['think', 'code', 'pre', 'span'],
    ADD_ATTR: ['class', 'language'],
    FORBID_TAGS: ['del', 's', 'strike'],
  })
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  await doSend(text)
}

async function sendSuggestion(text) {
  if (loading.value) return
  await doSend(text)
}

async function doSend(text) {
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  hasStreamingContent.value = false
  await nextTick()
  scrollToBottom()

  messages.value.push({ role: 'assistant', content: '' })
  const msgIdx = messages.value.length - 1
  // push 思考气泡后再次滚动到底，避免窗口满时思考气泡被输入框遮挡
  await nextTick()
  scrollToBottom()

  abortController = new AbortController()

  try {
    if (!sessionId.value) {
      sessionId.value = Math.random().toString(36).substring(2, 15)
    }

    const response = await fetch('/api/meeting/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Session-Id': sessionId.value,
        'Authorization': userStore.token ? 'Bearer ' + userStore.token : '',
      },
      body: JSON.stringify({ message: text }),
      signal: abortController.signal,
    })

    if (!response.ok) {
      messages.value[msgIdx].content = `[请求失败（${response.status}），请稍后重试]`
      return
    }

    const reader = response.body?.getReader()
    if (!reader) {
      messages.value[msgIdx].content = '[连接异常，请稍后重试]'
      return
    }

    const decoder = new TextDecoder('utf-8')
    let accumulatedContent = ''
    while (true) {
      try {
        const { value, done } = await reader.read()
        if (done) break
        accumulatedContent += decoder.decode(value, { stream: true })
        messages.value[msgIdx].content = accumulatedContent
        hasStreamingContent.value = true
        await nextTick()
        scrollToBottom()
      } catch (readError) {
        if (readError.name === 'AbortError') break
        console.error('读取流错误:', readError)
        break
      }
    }
    // 流结束后如果无任何内容，给出提示
    if (!accumulatedContent.trim()) {
      messages.value[msgIdx].content = '[未收到回复，请稍后重试]'
    }
  } catch (e) {
    if (e.name === 'AbortError') {
      // 用户主动停止，保留已接收内容
      if (!messages.value[msgIdx].content.trim()) {
        messages.value[msgIdx].content = '[已停止生成]'
      }
    } else {
      messages.value[msgIdx].content += '\n\n[连接异常，请稍后重试]'
    }
  } finally {
    loading.value = false
    hasStreamingContent.value = false
    abortController = null
    await nextTick()
    scrollToBottom()
  }
}

function stopGenerating() {
  if (abortController) {
    abortController.abort()
  }
}

function clearChat() {
  // 清空会话时重置所有状态，避免输入框卡在禁用状态
  if (abortController) {
    abortController.abort()
  }
  loading.value = false
  hasStreamingContent.value = false
  messages.value = []
  if (sessionId.value) {
    fetch(`/api/meeting/chat/session/${sessionId.value}`, { method: 'DELETE' }).catch(() => {})
    sessionId.value = ''
  }
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

defineExpose({
  togglePanel
})
</script>

<style scoped>
.chat-float { position: fixed; bottom: 24px; right: 24px; z-index: 9999; }

.chat-panel {
  position: fixed; bottom: 24px; right: 24px;
  width: 520px; height: 640px;
  background: #fff; border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
  display: flex; flex-direction: column;
  overflow: hidden; border: 1px solid #f0f0f0;
}

.chat-header {
  padding: 14px 16px; display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid #f0f0f0; background: #fafbfc;
}
.chat-title { font-size: 14px; font-weight: 600; color: #374151; }
.chat-actions { display: flex; gap: 2px; }

.chat-messages {
  flex: 1; overflow-y: auto; padding: 16px 16px 24px;
  display: flex; flex-direction: column; gap: 12px;
  scroll-behavior: smooth;
}

.chat-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%; color: #9ca3af; gap: 8px;
}
.chat-empty p { font-size: 13px; text-align: center; margin: 0; }

.chat-msg { display: flex; flex-direction: column; gap: 6px; }
.chat-msg.user { align-items: flex-end; }
.chat-msg.assistant { align-items: flex-start; }

.msg-bubble {
  max-width: 80%; padding: 10px 14px; border-radius: 12px;
  font-size: 13px; line-height: 1.6; word-break: break-word;
}
.chat-msg.user .msg-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2); color: #fff;
  border-bottom-right-radius: 4px;
}
.chat-msg.assistant .msg-bubble {
  background: #f3f4f6; color: #374151;
  border-bottom-left-radius: 4px;
}

.msg-text { white-space: pre-wrap; }

/* 流式输出光标 */
.streaming-cursor {
  display: inline-block; width: 7px; height: 14px; vertical-align: text-bottom;
  margin-left: 2px; background: #667eea; border-radius: 1px;
  animation: blink 0.8s step-end infinite;
}
@keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }

/* 引导提问芯片：inline-flex 让容器宽度随内容，多行对齐起始边，保证换行时行列对齐 */
.suggest-chips {
  display: inline-flex; flex-wrap: wrap; gap: 6px 8px;
  max-width: 100%;
  justify-content: flex-start; align-content: flex-start; align-items: flex-start;
}
.suggest-chips :deep(.el-button) {
  flex: 0 0 auto;          /* 禁止伸缩，避免被压缩后错位 */
  box-sizing: border-box;
  max-width: 100%;         /* 单个按钮不超过容器宽度 */
  font-size: 12px; padding: 6px 12px; height: auto; line-height: 1.4;
  white-space: normal;     /* 允许内部换行，防止超长按钮撑破容器导致收缩错位 */
  text-align: left;
  background: rgba(102, 126, 234, 0.08); color: #667eea; border-color: transparent;
}
.suggest-chips :deep(.el-button:hover) {
  background: rgba(102, 126, 234, 0.15); color: #5568d3;
}

/* Markdown 内容样式 */
.markdown-content :deep(p) { margin: 0.35rem 0; }
.markdown-content :deep(p:first-child) { margin-top: 0; }
.markdown-content :deep(p:last-child) { margin-bottom: 0; }
.markdown-content :deep(ul), .markdown-content :deep(ol) { margin: 0.35rem 0; padding-left: 1.5rem; }
.markdown-content :deep(li) { margin: 0.15rem 0; }
.markdown-content :deep(strong) { font-weight: 600; }
.markdown-content :deep(code) { background: rgba(0,0,0,0.06); padding: 0.1em 0.35em; border-radius: 4px; font-size: 0.85em; }
.markdown-content :deep(pre code) { background: transparent; padding: 0; }
.markdown-content :deep(pre) { background: #1d1d1f; color: #f5f5f7; padding: 0.75rem 1rem; border-radius: 10px; overflow-x: auto; margin: 0.5rem 0; }
.markdown-content :deep(table) { border-collapse: collapse; margin: 0.5rem 0; width: 100%; font-size: 0.875rem; }
.markdown-content :deep(th), .markdown-content :deep(td) { border: 1px solid #d2d2d7; padding: 0.35rem 0.6rem; text-align: left; }
.markdown-content :deep(th) { background: rgba(0,0,0,0.03); font-weight: 600; }
.markdown-content :deep(blockquote) { margin: 0.4rem 0; padding-left: 0.75rem; border-left: 2px solid #d2d2d7; color: #86868b; }
.markdown-content :deep(hr) { border: none; border-top: 1px solid #d2d2d7; margin: 0.5rem 0; }

/* 思考中气泡：柔和渐变背景，区别于普通回复 */
.chat-msg.assistant .thinking-bubble {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.10), rgba(118, 75, 162, 0.10));
  border-bottom-left-radius: 4px;
  min-height: 42px;              /* 足够高度，避免被输入框顶边"压线" */
  display: flex;
  align-items: center;
}

/* 打字动画 */
.typing-indicator { display: flex; align-items: center; gap: 6px; padding: 2px 0; }
.typing-indicator span {
  width: 8px; height: 8px; border-radius: 50%;
  background: #667eea;
  animation: typing-bounce 1.2s ease-in-out infinite;
}
.typing-indicator span:nth-child(2) { animation-delay: 0.18s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.36s; }
@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.35; }
  30% { transform: translateY(-5px); opacity: 1; }
}

/* 输入框：圆角容器 + 圆形发送按钮 */
.chat-input { padding: 12px 16px; border-top: 1px solid #f0f0f0; background: #fff; }
.input-wrapper {
  display: flex; align-items: center; gap: 8px;
  background: #f3f4f6; border-radius: 22px; padding: 5px 5px 5px 16px;
  transition: background 0.2s, box-shadow 0.2s;
}
.input-wrapper:focus-within {
  background: #fff;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.25);
}
.chat-input-field { flex: 1; }
.chat-input-field :deep(.el-input__wrapper) {
  background: transparent; box-shadow: none !important; padding: 0;
}
.chat-input-field :deep(.el-input__inner) {
  border: none; background: transparent; font-size: 13px;
  height: 32px; line-height: 32px; box-shadow: none !important;
}
.chat-input-field :deep(.el-input.is-disabled .el-input__inner) { background: transparent; }
.send-btn {
  flex-shrink: 0; width: 32px; height: 32px; min-height: 32px;
  padding: 0; font-size: 14px;
}
.send-btn.stop-btn { background: #f56c6c; border-color: #f56c6c; color: #fff; }
.send-btn.stop-btn:hover { background: #f23c3c; border-color: #f23c3c; }

.chat-slide-enter-active, .chat-slide-leave-active { transition: all 0.3s ease; }
.chat-slide-enter-from, .chat-slide-leave-to { opacity: 0; transform: translateY(20px); }

/* 暗色模式适配 */
html.dark .chat-panel { background: var(--bg-card); border-color: var(--border); box-shadow: 0 8px 32px rgba(0,0,0,0.4); }
html.dark .chat-header { background: #161628; border-color: var(--border); }
html.dark .chat-title { color: var(--text-primary); }
html.dark .chat-empty { color: var(--text-muted); }
html.dark .chat-msg.assistant .msg-bubble { background: #252542; color: var(--text-primary); }
html.dark .chat-msg.assistant .thinking-bubble {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.22), rgba(118, 75, 162, 0.22));
}
html.dark .chat-input { border-color: var(--border); background: var(--bg-card); }
html.dark .input-wrapper { background: #252542; }
html.dark .input-wrapper:focus-within { background: #252542; box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.45); }
html.dark .chat-input-field :deep(.el-input__inner) { color: var(--text-primary); }
html.dark .chat-input-field :deep(.el-input__inner::placeholder) { color: var(--text-muted); }
html.dark .markdown-content :deep(code) { background: rgba(255,255,255,0.1); }
html.dark .markdown-content :deep(th), html.dark .markdown-content :deep(td) { border-color: var(--border); }
html.dark .markdown-content :deep(th) { background: rgba(255,255,255,0.05); }
html.dark .markdown-content :deep(blockquote) { border-left-color: var(--border); color: var(--text-muted); }
html.dark .markdown-content :deep(hr) { border-top-color: var(--border); }
</style>
