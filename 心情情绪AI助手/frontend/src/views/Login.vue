<template>
  <AuthLayout wide>
    <div class="back-link" @click="goHome">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回首页</span>
    </div>
    <h3>欢迎回来</h3>
    <p class="auth-sub">登录后，继续记录你的心情</p>
    <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules">
      <el-form-item prop="username">
        <el-input v-model="loginForm.username" placeholder="用户名 / 邮箱" size="large">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password>
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="captcha">
        <Captcha ref="captchaRef" v-model="loginForm.captcha" />
      </el-form-item>
      <el-form-item>
        <el-radio-group v-model="loginForm.userType" class="user-type-group">
          <el-radio :value="1">前台用户</el-radio>
          <el-radio :value="2">后台管理员</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleLogin">登 录</el-button>
      </el-form-item>
    </el-form>
    <p class="toggle-tip">
      还没有账户？<router-link to="/auth/register">立即注册</router-link>
    </p>
  </AuthLayout>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'
import Captcha from '@/components/Captcha.vue'
import { login } from '@/api/user'

const route = useRoute()
const router = useRouter()
const loginFormRef = ref(null)
const captchaRef = ref(null)
const loading = ref(false)
const loginForm = reactive({ username: '', password: '', captcha: '', userType: 1 })
const loginRules = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const goHome = () => router.push('/')

const handleLogin = async () => {
  console.log('[登录] 按钮点击', loginForm)
  if (!loginFormRef.value) {
    console.error('[登录] 表单实例未挂载')
    return
  }
  try {
    await loginFormRef.value.validate()
  } catch (e) {
    console.warn('[登录] 表单校验未通过', e)
    return
  }
  if (!captchaRef.value || !captchaRef.value.validate(loginForm.captcha)) {
    ElMessage.error('验证码错误')
    captchaRef.value?.refresh()
    return
  }
  loading.value = true
  try {
    const { captcha, ...payload } = loginForm
    const res = await login(payload)
    console.log(`[登录 userType=${loginForm.userType}] 接口返回：`, res)
const data = res.data || res
      const token = data.token || res.token
      const userInfo = data.userInfo || data.user || res.userInfo || {}
      // 始终以前端选择的身份为准，确保守卫与跳转一致
      userInfo.userType = loginForm.userType
      if (token) localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    if (loginForm.userType === 2) {
      router.push(typeof redirect === 'string' && redirect.startsWith('/back') ? redirect : '/back/dashboard')
    } else {
      router.push('/')
    }
  } catch (err) {
    console.error('[登录] 失败：', err)
    captchaRef.value?.refresh()
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.user-type-group {
  width: 100%;
  display: flex;
  justify-content: center;
  gap: 24px;
}
</style>