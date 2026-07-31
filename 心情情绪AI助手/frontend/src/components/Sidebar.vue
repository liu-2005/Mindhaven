<template>
  <el-aside :width="collapsed ? '64px' : '264px'" class="menu-style">
    <div class="brand">
      <img src="@/assets/images/机器人.png" class="brand-logo" alt="logo" />
      <div class="brand-title" :class="{ 'brand-title--collapsed': collapsed }">
        <span class="brand-title-main">心理健康AI助手</span>
        <span class="brand-title-sub">管理后台</span>
      </div>
    </div>
    <el-menu
      :default-active="activeMenu"
      router
      class="menu-nav"
    >
      <el-tooltip
        v-for="item in menuItems"
        :key="item.path"
        :content="item.meta.title"
        :disabled="!collapsed"
        placement="right"
        :show-after="300"
      >
        <el-menu-item :index="item.path">
          <el-icon>
            <component :is="iconMap[item.meta.icon]" />
          </el-icon>
          <span class="menu-text" :class="{ 'menu-text--hidden': collapsed }">{{ item.meta.title }}</span>
        </el-menu-item>
      </el-tooltip>
    </el-menu>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataAnalysis, Document, ChatDotRound, Sunny } from '@element-plus/icons-vue'

defineProps({
  collapsed: Boolean,
})

const iconMap = {
  DataAnalysis,
  Document,
  ChatDotRound,
  Sunny,
}

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)

const menuItems = computed(() => {
  const backRoute = router.options.routes.find(r => r.path === '/back')
  return backRoute?.children?.filter(c => c.meta) ?? []
})
</script>

<style lang="scss" scoped>
.menu-style {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  transition: width 0.3s ease;

  .brand {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 24px 20px 20px;
    border-bottom: 1px solid #e4e7ed;
    background: #fff;

    .brand-logo {
      width: 40px;
      height: 40px;
      flex-shrink: 0;
    }

    .brand-title {
      display: flex;
      flex-direction: column;
      white-space: nowrap;
      overflow: hidden;
      opacity: 1;
      max-width: 200px;
      transition: opacity 0.25s ease, max-width 0.3s ease;

      &--collapsed {
        opacity: 0;
        max-width: 0;
      }

      .brand-title-main {
        font-size: 20px;
        font-weight: bold;
        color: #303133;
      }

      .brand-title-sub {
        font-size: 12px;
        color: #909399;
        margin-top: 2px;
      }
    }
  }

  .menu-nav {
    flex: 1;
    border-right: none;
  }

  .menu-text {
    white-space: nowrap;
    opacity: 1;
    transition: opacity 0.25s ease;

    &--hidden {
      opacity: 0;
    }
  }
}
</style>