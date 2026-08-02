<template>
  <div class="page-header">
    <div class="page-header__left">
      <h2 class="page-header__title">{{ title }}</h2>
      <p v-if="description" class="page-header__desc">{{ description }}</p>
    </div>
    <div class="page-header__right">
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<style lang="scss" scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 20px;

  &__title {
    font-size: 22px;
    font-weight: normal;
    
    color: #303133;
    margin: 0;
  }

  &__desc {
    font-size: 14px;
    color: #909399;
    margin: 8px 0 0;
  }
}
</style>