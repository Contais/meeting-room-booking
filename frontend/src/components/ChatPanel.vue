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
              <div class="msg-content" v-html="renderMarkdown(msg.content)"></div>
            </div>
          </div>
          <div v-if="loading" class="chat-msg assistant">
            <div class="msg-bubble"><div class="msg-content typing">思考中...</div></div>
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

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ChatDotRound, Close, Delete, Promotion } from '@element-plus/icons-vue'

const visible = ref(false)
const inputText = ref('')
const loading = ref(false)
const messages = ref<{ role: string; content: string }[]>([])
const messagesContainer = ref<HTMLElement>()
const sessionId = ref('')

function togglePanel() { visible.value = !visible.value }

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
      },
      body: JSON.stringify({ message: text }),
    })

    const reader = response.body?.getReader()
    if (!reader) return

    const decoder = new TextDecoder()
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const chunk = decoder.decode(value)
      const lines = chunk.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          if (data === '[DONE]') break
          if (data) {
            messages.value[msgIdx].content += data
            await nextTick()
            scrollToBottom()
          }
        }
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

function renderMarkdown(text: string): string {
  return text.replace(/\n/g, '<br>')
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
  width: 380px; height: 520px;
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

.typing { color: #9ca3af; font-style: italic; }

.chat-input { padding: 12px 16px; border-top: 1px solid #f0f0f0; }

.chat-slide-enter-active, .chat-slide-leave-active { transition: all 0.3s ease; }
.chat-slide-enter-from, .chat-slide-leave-to { opacity: 0; transform: translateY(20px); }
</style>
