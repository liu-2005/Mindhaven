<template>
  <AuthLayout wide>
    <div class="back-link" @click="goHome">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回首页</span>
    </div>
    <h3>创建账户</h3>
    <p class="auth-sub">从今天开始，照顾你的情绪</p>
    <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules">
      <el-form-item prop="username">
        <el-input v-model="registerForm.username" placeholder="用户名" size="large">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="email">
        <el-input v-model="registerForm.email" placeholder="邮箱" size="large">
          <template #prefix>
            <el-icon><Message /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="nickname">
        <el-input v-model="registerForm.nickname" placeholder="昵称（选填）" size="large">
          <template #prefix>
            <el-icon><Avatar /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="phone">
        <el-input v-model="registerForm.phone" placeholder="手机（选填）" size="large">
          <template #prefix>
            <el-icon><Phone /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="registerForm.password" type="password" placeholder="密码" size="large" show-password>
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          placeholder="确认密码"
          size="large"
          show-password
        >
          <template #prefix>
            <el-icon><Lock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleRegister">注 册</el-button>
      </el-form-item>
    </el-form>
    <p class="toggle-tip">
      已有账户？<router-link to="/auth/login">立即登录</router-link>
    </p>
  </AuthLayout>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message, Avatar, Phone, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'
import { register } from '@/api/user'

const router = useRouter()
const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  email: '',
  nickname: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const validateConfirm = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: ['blur', 'change'] },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const goHome = () => router.push('/')

const handleRegister = () => {
  registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const { confirmPassword, captcha, ...payload } = registerForm
      const res = await register({ ...payload, userType: 1 })
      console.log('[前台注册] 接口返回：', res)
      ElMessage.success('注册成功')
      // 前台界面待开发，暂不跳转
    } catch (err) {
      console.error('[前台注册] 失败：', err)
    } finally {
      loading.value = false
    }
  })
}
</script>