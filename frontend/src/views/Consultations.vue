<template>
  <div class="page-container">
    <PageHeader title="咨询记录" />

    <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%; margin-top: 16px;">
      <el-table-column label="会话ID" width="90" align="center" fixed="left">
        <template #default="{ row }">
          <img :src="getAvatar(row)" class="user-avatar" alt="会话头像" />
        </template>
      </el-table-column>
      <el-table-column label="情绪日志" min-width="360">
        <template #default="{ row }">
          <div class="emotion-log-cell">
            <div class="emotion-top-line">
              <span class="ai-name">{{ getAiName(row) }}</span>
              <span class="last-msg-time">{{ formatTime(row.lastMessageTime || row.last_message_time) }}</span>
            </div>
            <div class="emotion-content">{{ getEmotionContent(row) }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="消息数" width="120" align="center">
        <template #default="{ row }">
          {{ row.messageCount || row.message_count || row.msgCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatTime(row.startedAt || row.createTime || row.createdAt || row.create_time || row.gmtCreate || row.gmt_create || row.createdTime || row.created_time || row.createDate || row.timestamp) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleViewMessages(row)">详情</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.currentPage"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="messageDialogVisible" title="会话详情" width="960px" :close-on-click-modal="false">
      <!-- 用户信息 -->
      <div class="dialog-user-info">
        <img :src="getAvatar(currentRow)" class="dialog-avatar" alt="头像" />
        <div class="dialog-user-meta">
          <div class="dialog-user-name">{{ getAiName(currentRow) }}</div>
          <div class="dialog-user-detail">
            <span>会话：{{ currentRow.title || currentRow.sessionTitle || currentRow.session_title || '-' }}</span>
          </div>
          <div class="dialog-user-detail">
            <span>创建：{{ formatTime(currentRow.startedAt || currentRow.createTime || currentRow.createdAt || currentRow.create_time) }}</span>
            <span style="margin-left: 16px;">消息数：{{ currentRow.messageCount || currentRow.message_count || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- 对话记录 -->
      <div class="dialog-section-title">对话记录</div>
      <div v-loading="messageLoading" class="message-list">
        <div v-if="!messageList.length && !messageLoading" class="empty-tip">暂无消息记录</div>
        <div
          v-for="(msg, index) in messageList"
          :key="index"
          class="message-item"
          :class="getMessageRole(msg) === 'user' ? 'is-user' : 'is-bot'"
        >
          <div class="message-avatar">
            <img v-if="getMessageRole(msg) === 'user'" src="@/assets/images/user.jpg" alt="user" />
            <img v-else src="@/assets/images/机器人.png" alt="bot" />
          </div>
          <div class="message-body">
            <div class="message-meta">
              <span class="message-role">{{ getMessageRole(msg) === 'user' ? '用户' : 'AI' }}</span>
              <span class="message-time">{{ formatTime(msg.createTime || msg.createdAt || msg.create_time) }}</span>
            </div>
            <div class="message-content">{{ msg.content || msg.text || msg.message || '-' }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="messageDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { getSessionPage, getSessionMessages, deleteSession } from '@/api/consultation'
import defaultAvatar from '@/assets/images/user.jpg'



// 头像：优先取行数据中的头像字段，没有则用默认用户图
const getAvatar = (row) => {
  const url = row.avatar || row.userAvatar || row.user?.avatar || row.headImg || row.profileImage || row.profile_image
  return url || defaultAvatar
}

// 情绪日志内容：优先情绪字段，fallback 到 lastMessageContent
const getEmotionContent = (row) => {
  if (!row) return '-'
  const emotionObj = row.emotion || row.emotionData || row.emotionInfo
  if (emotionObj && typeof emotionObj === 'object') {
    const log = emotionObj.log || emotionObj.content || emotionObj.text || emotionObj.description || emotionObj.summary || emotionObj.note || emotionObj.emotionLog || ''
    if (log) return log
  }
  return row.emotionLog || row.emotion_log || row.emotionContent || row.emotion_content ||
         row.lastMessageContent || row.last_message_content ||
         row.content || row.description || row.summary || row.note || row.title || row.sessionTitle || row.log || '-'
}

// AI名称：从 sessionTitle 或 userNickname 提取
const getAiName = (row) => {
  if (!row) return '-'
  const title = row.title || row.sessionTitle || row.session_title || ''
  const dashIdx = title.indexOf('-')
  if (dashIdx > 0) return title.substring(0, dashIdx)
  return row.userNickname || row.user_nickname || row.aiName || row.ai_name || 'AI助手'
}

const pagination = reactive({
  currentPage: 1,
  size: 10,
  total: 0,
})

const loading = ref(false)
const tableData = ref([])

// 消息弹窗
const messageDialogVisible = ref(false)
const messageLoading = ref(false)
const currentRow = ref({})
const messageList = ref([])

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.currentPage,
      size: pagination.size,
    }
    const res = await getSessionPage(params)
    const data = res.data || res
    tableData.value = data.items || []
    pagination.total = data.total ?? tableData.value.length
  } catch (e) {
    console.error('获取咨询会话列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleViewMessages = async (row) => {
  currentRow.value = row
  messageDialogVisible.value = true
  messageLoading.value = true
  messageList.value = []
  try {
    const res = await getSessionMessages(row.id, { page: 1, size: 200 })
    const data = res.data || res
    messageList.value = data.items || []
  } catch (e) {
    console.error('获取消息列表失败', e)
    ElMessage.error('获取消息列表失败')
  } finally {
    messageLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该会话吗？此操作不可恢复。', '警告', {
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    })
    await deleteSession(row.id)
    ElMessage.success('删除成功')
    // 删除当前详情弹窗对应的行时关闭弹窗
    if (currentRow.value.id === row.id) messageDialogVisible.value = false
    // 删除后若当前页已空，回退一页
    if (tableData.value.length === 1 && pagination.currentPage > 1) {
      pagination.currentPage -= 1
    }
    fetchList()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') console.error('删除失败', e)
  }
}

const getMessageRole = (msg) => {
  if (!msg) return 'bot'
  const st = msg.senderType || msg.sender_type
  if (st === 'USER' || st === 1 || st === '1') return 'user'
  if (st === 'ASSISTANT' || st === 'SYSTEM' || st === 2 || st === '2') return 'bot'
  const role = msg.role || msg.sender || msg.from
  if (role) {
    const r = String(role).toLowerCase()
    if (r === 'user' || r === '用户' || r === 'human' || r === '0') return 'user'
  }
  return 'bot'
}

const formatTime = (val) => {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}











.ai-name { font-size: 14px; color: #303133; font-weight: 500; } .last-msg-time { font-size: 12px; color: #909399; margin-left: 12px; } .emotion-top-line { display: flex; align-items: center; margin-bottom: 6px; }

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.dialog-user-info {
  display: flex; align-items: flex-start; gap: 16px;
  padding: 16px 20px; background: #f5f7fa; border-radius: 8px; margin-bottom: 16px;
}
.dialog-avatar { width: 52px; height: 52px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.dialog-user-meta { display: flex; flex-direction: column; gap: 4px; }
.dialog-user-name { font-size: 18px; font-weight: 600; color: #303133; }
.dialog-user-detail { font-size: 14px; color: #606266; }
.dialog-section-title { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 12px; padding-left: 4px; border-left: 3px solid #409eff; }
.message-list {
  max-height: 620px;
  overflow-y: auto;
  padding: 4px 8px;

  .empty-tip {
    text-align: center;
    color: #909399;
    padding: 40px 0;
  }
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;

  &.is-user {
    flex-direction: row-reverse;

    .message-body {
      align-items: flex-end;
    }

    .message-content { font-size: 18px;
      background: #ecf5ff;
      color: #303133;
    }
  }

  .message-avatar img {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    object-fit: cover;
  }

  .message-body {
    display: flex;
    flex-direction: column;
    max-width: 70%;
  }

  .message-meta { font-size: 13px; display: flex; gap: 8px;
    color: #909399;
    margin-bottom: 4px;
  }

  .message-content { font-size: 18px;
    padding: 10px 14px;
    background: #f5f7fa;
    border-radius: 8px;
    color: #303133;
    
    line-height: 1.6;
    word-break: break-word;
    white-space: pre-wrap;
  }
}
:deep(.el-table__body tr td) { padding-top: 12px; padding-bottom: 12px; }
</style>
