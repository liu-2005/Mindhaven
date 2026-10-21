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
        <el-checkbox v-model="privacyAccepted">我已满18岁，并已阅读</el-checkbox>
        <el-button link type="primary" @click="privacyVisible = true">隐私与AI使用说明</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleRegister">注 册</el-button>
      </el-form-item>
    </el-form>
    <p class="toggle-tip">
      已有账户？<router-link to="/auth/login">立即登录</router-link>
    </p>
  </AuthLayout>
  <el-dialog v-model="privacyVisible" title="隐私与AI使用说明" width="min(560px, 92vw)">
    <p>本产品面向成年用户，用于记录心情、情绪陪伴和心理健康科普，不提供医学诊断、治疗或紧急救援服务。</p>
    <p>日记、聊天与情绪分析保存在本服务数据库。使用AI咨询时，会将当前问题和当前会话最近的必要上下文发送给DeepSeek生成回复；情绪评分会单独分析近期用户发言。请勿输入身份证、密码或详细住址等敏感信息。</p>
    <p>你可以删除自己的日记和会话。会话删除会清理关联消息与评分；为处理风险而产生的最小化事件与操作审计可能保留。退出登录不会删除内容。</p>
    <p>本地体验版本尚未提供自助账号注销、数据导出和自动到期清理；不应将它作为长期保存敏感信息的唯一渠道。</p>
    <p>如果遇到即时危险，请优先联系可信任的人和当地紧急服务。AI情绪分仅供自我觉察，不是临床量表。</p>
  </el-dialog>
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
const privacyAccepted = ref(false)
const privacyVisible = ref(false)

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
  if (!privacyAccepted.value) { ElMessage.warning('请先确认已满18岁，并阅读隐私与AI使用说明'); return }
  registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const { confirmPassword, captcha, ...payload } = registerForm
      await register({ ...payload, privacyConsentVersion: '2026-10-21' })
      ElMessage.success('注册成功')
      router.push('/auth/login')
    } catch (err) {
      console.error('[前台注册] 失败：', err)
    } finally {
      loading.value = false
    }
  })
}
</script>
<style scoped>
:deep(.el-dialog__body p) { margin: 0 0 12px; line-height: 1.75; }
</style>
