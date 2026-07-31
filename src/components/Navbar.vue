<template>
  <div class="navbar">
    <div class="navbar-left">
      <el-icon class="toggle-btn" @click="$emit('toggle-sidebar')" :size="20">
        <Fold v-if="!collapsed" />
        <Expand v-else />
      </el-icon>
      <span class="page-title">{{ route.meta?.title ?? '' }}</span>
    </div>
    <div class="navbar-right">
      <el-avatar :size="32" :src="avatarUrl" />
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="username">
          {{ username }}
          <el-icon class="arrow-icon"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, ArrowDown } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage, ElLoading } from 'element-plus'
import { logout } from '@/api/user'

defineProps({
  collapsed: Boolean,
})

defineEmits(['toggle-sidebar'])

const route = useRoute(); const router = useRouter()

const username = '管理员'

// 清除本地登录信息
const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(async () => {
      const loading = ElLoading.service({ fullscreen: true, text: '正在退出...' })
      try {
        // 调用后端登出接口，让服务端销毁 token
        await logout()
      } catch (e) {
        // 接口失败不阻塞登出流程，前端依然清除本地状态
        console.error('登出接口调用失败', e)
      } finally {
        clearAuth()
        loading.close()
        ElMessage.success('已退出登录')
        router.push('/auth/login')
      }
    }).catch(() => {})
  }
}

const avatarUrl = new URL('@/assets/images/user.jpg', import.meta.url).href
</script>

<style lang="scss" scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;

  .navbar-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .toggle-btn {
      cursor: pointer;
      color: #606266;
      padding: 4px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      transition: color 0.2s, border-color 0.2s;

      &:hover {
        color: #409eff;
        border-color: #409eff;
      }
    }

    .page-title {
      font-size: 26px;
      font-weight: bold;
      color: #303133;
    }
  }

  .navbar-right {
    display: flex;
    align-items: center;
    gap: 8px;

    .username {
      font-size: 14px;
      color: #303133;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 4px;

      .arrow-icon {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}
</style>