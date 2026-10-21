<template>
  <div class="auth-page">
    <div class="auth-bg">
      <div class="bg-circle circle-1"></div>
      <div class="bg-circle circle-2"></div>
    </div>

    <div class="auth-container" :class="{ 'auth-container--wide': wide }">
      <div class="auth-left">
        <div class="brand-area">
          <img src="@/assets/images/smile.svg" class="brand-logo" alt="logo" />
          <h1>心晴 MindHaven</h1>
          <p class="brand-slogan">情绪没有好坏，只有被看见和没被看见</p>
        </div>
        <div class="feature-list">
          <div class="feature">
            <el-icon :size="24"><EditPen /></el-icon>
            <div>
              <strong>情绪日记</strong>
              <span>每天写几句，看见自己的变化</span>
            </div>
          </div>
          <div class="feature">
            <el-icon :size="24"><Sunny /></el-icon>
            <div>
              <strong>AI 情绪分析</strong>
              <span>AI 读懂你的情绪，给出贴心建议</span>
            </div>
          </div>
          <div class="feature">
            <el-icon :size="24"><ChatDotRound /></el-icon>
            <div>
              <strong>咨询陪伴</strong>
              <span>和AI梳理感受，获得日常陪伴</span>
            </div>
          </div>
        </div>
      </div>

      <div class="auth-right">
        <div class="auth-card">
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { EditPen, ChatDotRound, Sunny } from '@element-plus/icons-vue'

defineProps({
  wide: Boolean,
})
</script>

<style lang="scss" scoped>
$primary: #14b8a6;
$primary-dark: #0d9488;
$text-dark: #1f2937;
$text-body: #4b5563;
$text-muted: #9ca3af;

.auth-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f0fdfa 0%, #fef3f2 100%);
  overflow: hidden;
}

.auth-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;

  .bg-circle {
    position: absolute;
    border-radius: 50%;
  }

  .circle-1 {
    width: 500px;
    height: 500px;
    background: radial-gradient(circle, rgba(20, 184, 166, 0.15), transparent 70%);
    top: -150px;
    right: -100px;
  }

  .circle-2 {
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba(249, 115, 22, 0.1), transparent 70%);
    bottom: -120px;
    left: -80px;
  }
}

.auth-container {
  position: relative;
  display: flex;
  width: 920px;
  max-width: 100%;
  min-height: 540px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(20, 184, 166, 0.12);
  overflow: hidden;

  &--wide {
    width: 1080px;
    min-height: 620px;
  }
}

.auth-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px 48px;
  background: linear-gradient(160deg, #f0fdfa 0%, #ccfbf1 100%);

  .brand-area {
    text-align: center;
    margin-bottom: 44px;

    .brand-logo {
      width: 56px;
      height: 56px;
      margin-bottom: 14px;
      padding: 10px;
      background: #fff;
      border-radius: 50%;
      box-shadow: 0 6px 20px rgba(20, 184, 166, 0.2);
      object-fit: contain;
    }

    h1 {
      font-size: 26px;
      font-weight: 700;
      color: $text-dark;
      margin-bottom: 8px;
      background: linear-gradient(135deg, $primary, #f97316);
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }

    .brand-slogan {
      font-size: 13px;
      color: $text-muted;
      line-height: 1.6;
    }
  }

  .feature-list {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .feature {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 14px 18px;
      background: rgba(255, 255, 255, 0.7);
      border-radius: 12px;
      border: 1px solid rgba(20, 184, 166, 0.1);
      backdrop-filter: blur(6px);

      .el-icon {
        color: $primary-dark;
        flex-shrink: 0;
        font-size: 24px;
      }

      div {
        display: flex;
        flex-direction: column;

        strong {
          font-size: 14px;
          color: $text-dark;
          margin-bottom: 2px;
          font-weight: 600;
        }

        span {
          font-size: 12px;
          color: $text-muted;
        }
      }
    }
  }
}

.auth-right {
  width: 420px;
  max-width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 44px;
  background: #fff;

  .auth-container--wide & {
    width: 520px;
    padding: 48px 56px;
  }
}

.auth-card {
  width: 100%;

  :deep(h3) {
    font-size: 26px;
    color: $text-dark;
    margin-bottom: 6px;
    font-weight: 700;
  }

  :deep(.auth-sub) {
    font-size: 14px;
    color: $text-muted;
    margin-bottom: 32px;
  }

  :deep(.submit-btn) {
    width: 100%;
    height: 44px;
    font-size: 15px;
    letter-spacing: 4px;
    background: $primary;
    border-color: $primary;

    &:hover {
      background: $primary-dark;
      border-color: $primary-dark;
    }
  }

  :deep(.toggle-tip) {
    text-align: center;
    font-size: 14px;
    color: $text-muted;
    margin-top: 20px;

    a {
      color: $primary;
      font-weight: 500;

      &:hover {
        color: $primary-dark;
        text-decoration: underline;
      }
    }
  }

  :deep(.back-link) {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 14px;
    color: $text-muted;
    cursor: pointer;
    margin-bottom: 24px;
    transition: color 0.2s;

    &:hover {
      color: $primary;
    }
  }

  :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: 0 0 0 1px #e5e7eb inset;

    &:hover {
      box-shadow: 0 0 0 1px $primary inset;
    }
    &.is-focus {
      box-shadow: 0 0 0 1px $primary inset;
    }
  }

  :deep(.el-input__prefix-inner .el-icon) {
    color: $primary-dark;
    font-size: 18px;
  }

  :deep(.el-form-item__label) {
    color: $text-dark;
  }
}
</style>
