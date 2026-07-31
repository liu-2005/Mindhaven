<template>
  <div class="backend-layout">
    <el-container class="layout-container">
      <Sidebar :collapsed="isCollapsed" />
      <el-container>
        <el-header>
          <Navbar :collapsed="isCollapsed" @toggle-sidebar="isCollapsed = !isCollapsed" />
        </el-header>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from '@/components/Sidebar.vue'
import Navbar from '@/components/Navbar.vue'

const isCollapsed = ref(false)
</script>

<style lang="scss" scoped>
.backend-layout {
  height: 100vh;

  .layout-container {
    height: 100%;
  }
}

.el-main {
  background: #fff;
  margin: 20px;
  border-radius: 8px;
  padding: 20px;
}
</style>
<style>
.el-header {
  height: 85px !important;
}

.el-main {
  background: #fff;
  margin: 20px;
  border-radius: 8px;
  padding: 20px;
}
</style>