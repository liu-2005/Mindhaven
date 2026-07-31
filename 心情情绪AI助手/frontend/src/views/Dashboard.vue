<template>
  <div class="dashboard">
    <PageHeader title="数据分析" />

    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="(card, index) in statCards" :key="index">
        <el-card shadow="hover" class="stat-card" v-loading="loading">
          <div class="stat-card-body">
            <div class="stat-icon" :style="{ background: card.bgColor }">
              <el-icon :size="28" color="#fff"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
              <div v-if="card.sub" class="stat-sub">{{ card.sub }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover" v-loading="chartLoading">
          <div ref="emotionTrendRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" v-loading="chartLoading">
          <div ref="sessionStatRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="24">
        <el-card shadow="hover" v-loading="chartLoading">
          <div ref="userActivityRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, markRaw, h, nextTick } from 'vue'
import * as echarts from 'echarts'
import { User, Sunny, ChatDotRound } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getUserPage } from '@/api/user'
import { getEmotionDiaryPage } from '@/api/emotionDiary'
import { getSessionPage } from '@/api/consultation'

const loading = ref(false)
const chartLoading = ref(false)

// 笑脸图标
const SmileyIcon = markRaw({
  render() {
    return h('svg', { viewBox: '0 0 24 24', width: '1em', height: '1em', fill: 'currentColor' }, [
      h('path', { d: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z' })
    ])
  }
})

const statCards = ref([
  { label: '总用户数', value: '-', sub: '活跃用户：-', icon: markRaw(User), bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { label: '情绪日志', value: '-', sub: '今日新增：-', icon: markRaw(Sunny), bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { label: '咨询会话', value: '-', sub: '今日新增：-', icon: markRaw(ChatDotRound), bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { label: '平均情绪评分', value: '-', sub: '满分 10 分', icon: markRaw(SmileyIcon), bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' },
])

// 图表 DOM 引用
const emotionTrendRef = ref(null)
const sessionStatRef = ref(null)
const userActivityRef = ref(null)
let emotionTrendChart = null
let sessionStatChart = null
let userActivityChart = null

// 日期工具
const isToday = (val) => {
  if (!val) return false
  const d = new Date(val)
  if (isNaN(d.getTime())) return false
  const now = new Date()
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate()
}

// 获取最近 N 天的日期数组
const getRecentDays = (n) => {
  const days = []
  const now = new Date()
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    const pad = (v) => String(v).padStart(2, '0')
    days.push(`${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`)
  }
  return days
}

// 取日期部分（YYYY-MM-DD）
const toDateStr = (val) => {
  if (!val) return ''
  const d = new Date(val)
  if (isNaN(d.getTime())) return String(val).substring(0, 10)
  const pad = (v) => String(v).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const fetchStats = async () => {
  loading.value = true
  chartLoading.value = true
  try {
    const [userRes, emotionRes, sessionRes] = await Promise.all([
      getUserPage({ current: 1, size: 500 }).catch(() => ({ data: { records: [], total: 0 } })),
      getEmotionDiaryPage({ current: 1, size: 500 }),
      getSessionPage({ current: 1, size: 500 }),
    ])

    const userData = userRes.data || userRes
    const userList = userData.records || userData.list || userData.rows || []
    const emotionData = emotionRes.data || emotionRes
    const emotionList = emotionData.records || emotionData.list || emotionData.rows || []
    const sessionData = sessionRes.data || sessionRes
    const sessionList = sessionData.records || sessionData.list || sessionData.rows || []

    // 统计卡片
    statCards.value[0].value = userData.total ?? userList.length
    statCards.value[0].sub = `活跃用户：${userList.filter(u => isToday(u.lastLoginTime || u.last_login_time || u.updateTime || u.update_time || u.createTime || u.createdAt || u.create_time)).length}`
    statCards.value[1].value = emotionData.total ?? emotionList.length
    statCards.value[1].sub = `今日新增：${emotionList.filter(r => isToday(r.diaryDate || r.diary_date || r.createdAt || r.createTime || r.create_time)).length}`
    statCards.value[2].value = sessionData.total ?? sessionList.length
    statCards.value[2].sub = `今日新增：${sessionList.filter(r => isToday(r.startedAt || r.createTime || r.createdAt || r.create_time)).length}`

    const scores = emotionList.map(r => Number(r.moodScore || r.mood_score || r.score || 0)).filter(s => !isNaN(s) && s > 0)
    statCards.value[3].value = scores.length ? (scores.reduce((s, v) => s + v, 0) / scores.length).toFixed(1) : '0.0'

    // 准备图表数据：按最近 30 天聚合
    const days = getRecentDays(30)

    // 情绪趋势：每日平均评分 + 记录数
    const emotionByDay = days.map(date => {
      const items = emotionList.filter(r => toDateStr(r.diaryDate || r.diary_date || r.createdAt || r.createTime || r.create_time) === date)
      const dayScores = items.map(r => Number(r.moodScore || r.mood_score || r.score || 0)).filter(s => !isNaN(s) && s > 0)
      return { date, avgScore: dayScores.length ? Number((dayScores.reduce((s, v) => s + v, 0) / dayScores.length).toFixed(1)) : 0, recordCount: items.length }
    })

    // 咨询会话统计：每日会话数 + 参与用户数（去重 userId）+ 平均时长
    const sessionByDay = days.map(date => {
      const items = sessionList.filter(r => toDateStr(r.startedAt || r.createTime || r.createdAt || r.create_time) === date)
      const userIds = new Set()
      items.forEach(r => { if (r.userId) userIds.add(r.userId) })
      // 计算当日平均时长（分钟）
      const durations = items.map(r => Number(r.duration || r.durationMinutes || r.totalTime || r.totalMinutes || 0)).filter(d => !isNaN(d) && d > 0)
      const avgDuration = durations.length ? Number((durations.reduce((s, v) => s + v, 0) / durations.length).toFixed(1)) : 0
      return { date, sessionCount: items.length, userCount: userIds.size, avgDuration }
    })
    // 总会话数和总平均时长（全部数据）
    const allDurations = sessionList.map(r => Number(r.duration || r.durationMinutes || r.totalTime || r.totalMinutes || 0)).filter(d => !isNaN(d) && d > 0)
    const totalSessions = sessionData.total ?? sessionList.length
    const overallAvgDuration = allDurations.length ? Number((allDurations.reduce((s, v) => s + v, 0) / allDurations.length).toFixed(1)) : 0

    // 用户活跃度：活跃用户、新增用户、日记用户、咨询用户
    const activityByDay = days.map(date => {
      const emotionUsers = new Set(emotionList.filter(r => toDateStr(r.diaryDate || r.diary_date || r.createdAt || r.createTime || r.create_time) === date).map(r => r.userId))
      const sessionUsers = new Set(sessionList.filter(r => toDateStr(r.startedAt || r.createTime || r.createdAt || r.create_time) === date).map(r => r.userId))
      const newUsers = new Set(userList.filter(r => toDateStr(r.createTime || r.createdAt || r.create_time) === date).map(r => r.userId || r.id))
      const activeUsers = new Set([...emotionUsers, ...sessionUsers, ...newUsers])
      return { date, activeUsers: activeUsers.size, newUsers: newUsers.size, diaryUsers: emotionUsers.size, consultationUsers: sessionUsers.size }
    })

    await nextTick()
    initEmotionTrendChart(emotionByDay)
    initSessionStatChart(sessionByDay, totalSessions, overallAvgDuration)
    initUserActivityChart(activityByDay)
  } catch (e) {
    console.error('获取统计数据失败', e)
  } finally {
    loading.value = false
    chartLoading.value = false
  }
}

// 情绪趋势分析（折线图）
const initEmotionTrendChart = (trendData) => {
  if (!emotionTrendRef.value) return
  emotionTrendChart = echarts.init(emotionTrendRef.value)
  emotionTrendChart.setOption({
    title: { text: '情绪趋势分析', textStyle: { fontSize: 16, fontWeight: 600, color: '#2d3436' }, left: 'center', top: 10 },
    tooltip: { trigger: 'axis', borderColor: '#fab1a0', borderWidth: 1, textStyle: { color: '#2d3436' } },
    legend: { data: ['平均情绪评分', '记录数量'], top: 40 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 80, containLabel: true },
    xAxis: { type: 'category', data: trendData.map(item => item.date), axisLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.3)' } } },
    yAxis: [
      { type: 'value', name: '情绪评分', position: 'left', axisLabel: { color: '#636e72' }, axisLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.3)' } }, splitLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.1)' } } },
      { type: 'value', name: '记录数量', position: 'right', axisLabel: { color: '#636e72' }, axisLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.3)' } }, splitLine: { show: false } }
    ],
    series: [
      { name: '平均情绪评分', type: 'line', data: trendData.map(item => item.avgScore), smooth: true, lineStyle: { width: 3, color: '#ffeaa7' }, itemStyle: { color: '#ffeaa7' } },
      { name: '记录数量', type: 'line', data: trendData.map(item => item.recordCount), smooth: true, lineStyle: { width: 3, color: '#fab1a0' }, itemStyle: { color: '#fab1a0' } }
    ]
  })
}

// 咨询活动统计（柱状图 + 顶部汇总）
const initSessionStatChart = (dailyTrend, totalSessions, overallAvgDuration) => {
  if (!sessionStatRef.value) return
  sessionStatChart = echarts.init(sessionStatRef.value)
  sessionStatChart.setOption({
    title: [
      { text: '咨询活动统计', textStyle: { fontSize: 16, fontWeight: 600, color: '#2d3436' }, left: 'center', top: 10 },
      { text: `总会话数：{a|${totalSessions}}    平均时长：{b|${overallAvgDuration} 分钟}`, textStyle: { fontSize: 13, color: '#636e72', rich: { a: { color: '#0984e3', fontWeight: 600, fontSize: 14 }, b: { color: '#f39c12', fontWeight: 600, fontSize: 14 } } }, left: 'center', top: 38 }
    ],
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.95)', borderColor: '#fab1a0', borderWidth: 1, textStyle: { color: '#2d3436' } },
    legend: { data: ['会话数量', '参与用户数'], top: 68, textStyle: { color: '#636e72' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 110, containLabel: true },
    xAxis: { type: 'category', data: dailyTrend.map(item => item.date), axisLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.3)' } }, axisLabel: { color: '#636e72' } },
    yAxis: { type: 'value', name: '数量', axisLabel: { color: '#636e72' }, axisLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.3)' } }, splitLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.1)' } } },
    series: [
      { name: '会话数量', type: 'bar', data: dailyTrend.map(item => item.sessionCount), itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#74b9ff' }, { offset: 1, color: '#0984e3' }] } }, barWidth: '40%' },
      { name: '参与用户数', type: 'bar', data: dailyTrend.map(item => item.userCount), itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#fdcb6e' }, { offset: 1, color: '#f39c12' }] } }, barWidth: '40%' }
    ]
  })
}

// 用户活跃度趋势（折线图）
const initUserActivityChart = (activityData) => {
  if (!userActivityRef.value) return
  userActivityChart = echarts.init(userActivityRef.value)
  userActivityChart.setOption({
    title: { text: '用户活跃度趋势', textStyle: { fontSize: 16, fontWeight: 600, color: '#2d3436' }, left: 'center', top: 10 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.95)', borderColor: '#fab1a0', borderWidth: 1, textStyle: { color: '#2d3436' } },
    legend: { data: ['活跃用户', '新增用户', '日记用户', '咨询用户'], top: 40, textStyle: { color: '#636e72' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 80, containLabel: true },
    xAxis: { type: 'category', data: activityData.map(item => item.date), axisLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.3)' } }, axisLabel: { color: '#636e72' } },
    yAxis: { type: 'value', axisLabel: { color: '#636e72' }, axisLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.3)' } }, splitLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.1)' } } },
    series: [
      { name: '活跃用户', type: 'line', data: activityData.map(item => item.activeUsers), smooth: true, lineStyle: { width: 3, color: '#a29bfe' }, itemStyle: { color: '#a29bfe' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(162, 155, 254, 0.4)' }, { offset: 1, color: 'rgba(162, 155, 254, 0.1)' }] } } },
      { name: '新增用户', type: 'line', data: activityData.map(item => item.newUsers), smooth: true, lineStyle: { width: 3, color: '#fdcb6e' }, itemStyle: { color: '#fdcb6e' } },
      { name: '日记用户', type: 'line', data: activityData.map(item => item.diaryUsers), smooth: true, lineStyle: { width: 3, color: '#00b894' }, itemStyle: { color: '#00b894' } },
      { name: '咨询用户', type: 'line', data: activityData.map(item => item.consultationUsers), smooth: true, lineStyle: { width: 3, color: '#fab1a0' }, itemStyle: { color: '#fab1a0' } }
    ]
  })
}

const handleResize = () => {
  emotionTrendChart?.resize()
  sessionStatChart?.resize()
  userActivityChart?.resize()
}

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  emotionTrendChart?.dispose()
  sessionStatChart?.dispose()
  userActivityChart?.dispose()
})
</script>

<style lang="scss" scoped>
.stat-row { margin-top: 16px; }
.stat-card {
  .stat-card-body { display: flex; align-items: center; gap: 16px; }
  .stat-icon { width: 56px; height: 56px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
  .stat-info { flex: 1; }
  .stat-value { font-size: 28px; font-weight: 700; color: #303133; line-height: 1.2; }
  .stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
  .stat-sub { font-size: 12px; color: #c0c4cc; margin-top: 4px; }
}
.chart-row { margin-top: 20px; }
.chart-container { height: 360px; width: 100%; }
</style>
