<template>
  <div class="filter-bar page-card">
    <el-form :inline="true" :model="model" class="filter-form">
      <slot />
      <el-form-item class="filter-actions">
        <el-button @click="$emit('reset')">重置</el-button>
        <el-button type="primary" @click="$emit('search')">查询</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
defineProps<{ model: Record<string, any> }>()
defineEmits(['search', 'reset'])
</script>
