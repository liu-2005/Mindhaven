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
const loginForm = reactive({ username: '', password: '', captcha: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const goHome = () => router.push('/')

// 根据服务端返回的角色决定落地页
const isAdmin = (roles = []) =>
  roles.some(r => ['CONTENT_ADMIN', 'SAFETY_ADMIN', 'USER_ADMIN'].includes(r))

const handleLogin = async () => {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch (e) {
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
    const data = res.data || res
    const token = data.token
    const user = data.user || {}
    if (token) localStorage.setItem('token', token)
    localStorage.setItem('userInfo', JSON.stringify(user))
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    if (isAdmin(user.roles)) {
      router.push(typeof redirect === 'string' && redirect.startsWith('/back') ? redirect : '/back/dashboard')
    } else {
      router.push('/')
    }
  } catch (err) {
    captchaRef.value?.refresh()
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped></style>
