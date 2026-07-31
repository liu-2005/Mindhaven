<template>
  <div class="captcha">
    <el-input
      :model-value="modelValue"
      placeholder="请输入验证码"
      size="large"
      maxlength="4"
      @update:model-value="onChange"
    >
      <template #prefix>
        <el-icon><Key /></el-icon>
      </template>
    </el-input>
    <canvas
      ref="canvasRef"
      class="captcha-canvas"
      width="100"
      height="40"
      title="点击刷新验证码"
      @click="refresh"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Key } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'change'])

const onChange = (val) => {
  emit('update:modelValue', val)
}

const canvasRef = ref(null)
const code = ref('')

const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

const randomColor = (min, max) => {
  const r = Math.floor(Math.random() * (max - min) + min)
  const g = Math.floor(Math.random() * (max - min) + min)
  const b = Math.floor(Math.random() * (max - min) + min)
  return `rgb(${r},${g},${b})`
}

const draw = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = canvas.width
  const h = canvas.height
  ctx.clearRect(0, 0, w, h)

  // 生成 4 位验证码
  let str = ''
  for (let i = 0; i < 4; i++) {
    str += chars[Math.floor(Math.random() * chars.length)]
  }
  code.value = str
  emit('change', str)

  // 背景
  ctx.fillStyle = '#f0fdfa'
  ctx.fillRect(0, 0, w, h)

  // 字符
  for (let i = 0; i < str.length; i++) {
    const x = 14 + i * 22
    const y = 28
    ctx.font = `${24}px Georgia`
    ctx.fillStyle = randomColor(50, 160)
    ctx.save()
    const deg = (Math.random() - 0.5) * 0.5
    ctx.translate(x, y)
    ctx.rotate(deg)
    ctx.fillText(str[i], 0, 0)
    ctx.restore()
  }

  // 干扰线
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = randomColor(120, 200)
    ctx.beginPath()
    ctx.moveTo(Math.random() * w, Math.random() * h)
    ctx.lineTo(Math.random() * w, Math.random() * h)
    ctx.stroke()
  }

  // 干扰点
  for (let i = 0; i < 20; i++) {
    ctx.fillStyle = randomColor(100, 200)
    ctx.beginPath()
    ctx.arc(Math.random() * w, Math.random() * h, 1, 0, Math.PI * 2)
    ctx.fill()
  }
}

const refresh = () => {
  draw()
  emit('update:modelValue', '')
}

onMounted(draw)

defineExpose({
  validate: (val) => (val || '').toUpperCase() === code.value.toUpperCase(),
  refresh,
})
</script>

<style lang="scss" scoped>
.captcha {
  display: flex;
  gap: 12px;
  align-items: center;

  .el-input {
    flex: 1;
  }
  .captcha-canvas {
    width: 100px;
    height: 40px;
    border-radius: 8px;
    cursor: pointer;
    border: 1px solid #e5e7eb;
    flex-shrink: 0;
  }
}
</style>