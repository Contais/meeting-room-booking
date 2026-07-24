<template>
  <div class="chat-float">
    <div class="chat-trigger" @click="togglePanel">
      <el-icon :size="22"><ChatDotRound /></el-icon>
    </div>

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
            <div class="msg-bubble">
              <div v-if="msg.role === 'user'" class="msg-text">{{ msg.content }}</div>
              <div v-else class="msg-text markdown-content" v-html="renderMarkdown(msg.content)"></div>
            </div>
          </div>
          <div v-if="loading" class="chat-msg assistant">
            <div class="msg-bubble">
              <div class="msg-text">
                <div class="typing-indicator"><span></span><span></span><span></span></div>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input v-model="inputText" placeholder="输入消息..." @keyup.enter="sendMessage" :disabled="loading">
            <template #append>
              <el-button :icon="Promotion" @click="sendMessage" :disabled="loading || !inputText.trim()" />
            </template>
          </el-input>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ChatDotRound, Close, Delete, Promotion } from '@element-plus/icons-vue'
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

function togglePanel() { visible.value = !visible.value }

function renderMarkdown(text) {
  if (!text) return ''
  let cleaned = text.replace(/<system-reminder>[\s\S]*?<\/system-reminder>/g, '')
  cleaned = cleaned.replace(/<system-reminder>[\s\S]*$/, '')
  const html = marked.parse(cleaned)
  return DOMPurify.sanitize(html, { ADD_TAGS: ['think', 'code', 'pre', 'span'], ADD_ATTR: ['class', 'language'] })
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  await nextTick()
  scrollToBottom()

  messages.value.push({ role: 'assistant', content: '' })
  const msgIdx = messages.value.length - 1

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
    })

    const reader = response.body?.getReader()
    if (!reader) return

    const decoder = new TextDecoder('utf-8')
    let accumulatedContent = ''
    while (true) {
      try {
        const { value, done } = await reader.read()
        if (done) break
        accumulatedContent += decoder.decode(value, { stream: true })
        messages.value[msgIdx].content = accumulatedContent
        await nextTick()
        scrollToBottom()
      } catch (readError) {
        console.error('读取流错误:', readError)
        break
      }
    }
  } catch (e) {
    messages.value[msgIdx].content += '\n\n[连接异常，请稍后重试]'
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

function clearChat() {
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
</script>

<style scoped>
.chat-float { position: fixed; bottom: 24px; right: 24px; z-index: 9999; }

.chat-trigger {
  width: 52px; height: 52px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; display: flex; align-items: center; justify-content: center;
  cursor: pointer; box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  transition: transform 0.2s;
}
.chat-trigger:hover { transform: scale(1.1); }

.chat-panel {
  position: absolute; bottom: 64px; right: 0;
  width: 440px; height: 640px;
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
  flex: 1; overflow-y: auto; padding: 16px;
  display: flex; flex-direction: column; gap: 12px;
}

.chat-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%; color: #9ca3af; gap: 8px;
}
.chat-empty p { font-size: 13px; text-align: center; margin: 0; }

.chat-msg { display: flex; }
.chat-msg.user { justify-content: flex-end; }
.chat-msg.assistant { justify-content: flex-start; }

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

/* 打字动画 */
.typing-indicator { display: flex; align-items: center; gap: 5px; padding: 4px 0; }
.typing-indicator span { width: 7px; height: 7px; border-radius: 50%; background: #86868b; animation: typing-bounce 1.2s ease-in-out infinite; }
.typing-indicator span:nth-child(2) { animation-delay: 0.15s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.3s; }
@keyframes typing-bounce { 0%, 60%, 100% { transform: translateY(0); opacity: 0.4; } 30% { transform: translateY(-5px); opacity: 1; } }

.chat-input { padding: 12px 16px; border-top: 1px solid #f0f0f0; }

.chat-slide-enter-active, .chat-slide-leave-active { transition: all 0.3s ease; }
.chat-slide-enter-from, .chat-slide-leave-to { opacity: 0; transform: translateY(20px); }
</style>
