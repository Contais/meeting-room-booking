<template>
  <div class="icp-footer">
    <a
      v-if="icpNumber"
      :href="MIIT_URL"
      target="_blank"
      rel="noopener noreferrer"
      class="filing-link"
    >
      {{ icpNumber }}
    </a>
    <a
      v-if="psRecordNumber"
      :href="psRecordUrl"
      target="_blank"
      rel="noopener noreferrer"
      class="filing-link"
    >
      {{ psRecordNumber }}
    </a>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

// 工信部 ICP 备案官网
const MIIT_URL = 'https://beian.miit.gov.cn/'
// 公安部备案详情页基础 URL（需拼接 recordcode）
const BEIAN_GOV_BASE = 'http://www.beian.gov.cn/portal/registerSystemInfo?recordcode='

const icpNumber = import.meta.env.VITE_ICP_NUMBER || ''
const psRecordNumber = import.meta.env.VITE_PS_RECORD_NUMBER || ''

// 从公安备案号中提取 14 位数字作为 recordcode
// 公安备案号格式如 "京公网安备 11010102000xxxy号"，其中 14 位数字即 recordcode
const psRecordCode = (psRecordNumber.match(/\d{14}/) || [])[0] || ''

const psRecordUrl = computed(() =>
  psRecordCode ? BEIAN_GOV_BASE + psRecordCode : 'http://www.beian.gov.cn/'
)
</script>

<style scoped>
.icp-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 12px;
  line-height: 1.5;
}

.filing-link {
  color: inherit;
  text-decoration: none;
  transition: opacity 0.2s;
}

.filing-link:hover {
  opacity: 0.75;
}
</style>
