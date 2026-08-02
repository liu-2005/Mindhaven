<template>
  <div class="search-bar">
    <el-form :model="model" inline class="search-form">
      <slot />
    </el-form>
    <div class="form-actions">
      <el-button type="primary" @click="$emit('search')">搜索</el-button>
      <el-button @click="$emit('reset')">重置</el-button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  // 搜索表单的数据对象（v-model 双向绑定的查询条件）
  model: { type: Object, required: true },
})
defineEmits(['search', 'reset'])
</script>

<style lang="scss" scoped>
.search-bar {
  .form-actions {
    padding-top: 12px;
  }
}
</style>
