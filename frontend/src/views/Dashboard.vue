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
import request from '@/utils/request'

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
  { label: '总用户数', value: '-', sub: '今日新增：-', icon: markRaw(User), bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { label: '情绪日志', value: '-', sub: '今日新增：-', icon: markRaw(Sunny), bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { label: '咨询会话', value: '-', sub: '累计：-', icon: markRaw(ChatDotRound), bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { label: '平均情绪评分', value: '-', sub: '满分 10 分', icon: markRaw(SmileyIcon), bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' },
])

// 图表 DOM 引用
const emotionTrendRef = ref(null)
const sessionStatRef = ref(null)
const userActivityRef = ref(null)
let emotionTrendChart = null
let sessionStatChart = null
let userActivityChart = null

const fetchStats = async () => {
  loading.value = true
  chartLoading.value = true
  try {
    const [ovRes, trRes] = await Promise.all([
      request.get('/admin/analytics/overview').catch(() => ({ data: {} })),
      request.get('/admin/analytics/trends', { params: { from: undefined, to: undefined, granularity: 'day' } }).catch(() => ({ data: [] })),
    ])
    const ov = ovRes.data || {}
    const trends = trRes.data || []

    statCards.value[0].value = ov.totalUsers ?? '-'
    statCards.value[0].sub = `今日新增：${trends.length ? trends[trends.length - 1].newUsers : '-'}`
    statCards.value[1].value = ov.totalDiaries ?? '-'
    statCards.value[1].sub = `今日新增：${ov.todayDiaries ?? '-'}`
    statCards.value[2].value = ov.totalSessions ?? '-'
    statCards.value[2].sub = '累计会话数'
    statCards.value[3].value = ov.averageMoodScore ?? '0.0'

    await nextTick()
    initEmotionTrendChart(trends)
    initSessionStatChart(trends)
    initUserActivityChart(trends)
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
      { name: '平均情绪评分', type: 'line', data: trendData.map(item => item.avgMoodScore), smooth: true, lineStyle: { width: 3, color: '#ffeaa7' }, itemStyle: { color: '#ffeaa7' } },
      { name: '记录数量', type: 'line', data: trendData.map(item => item.diaries), smooth: true, lineStyle: { width: 3, color: '#fab1a0' }, itemStyle: { color: '#fab1a0' } }
    ]
  })
}

// 咨询活动统计（柱状图）
const initSessionStatChart = (trendData) => {
  if (!sessionStatRef.value) return
  const totalSessions = trendData.reduce((s, r) => s + Number(r.sessions || 0), 0)
  sessionStatChart = echarts.init(sessionStatRef.value)
  sessionStatChart.setOption({
    title: [
      { text: '咨询活动统计', textStyle: { fontSize: 16, fontWeight: 600, color: '#2d3436' }, left: 'center', top: 10 },
      { text: `近30天会话：{a|${totalSessions}}`, textStyle: { fontSize: 13, color: '#636e72', rich: { a: { color: '#0984e3', fontWeight: 600, fontSize: 14 } } }, left: 'center', top: 38 }
    ],
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.95)', borderColor: '#fab1a0', borderWidth: 1, textStyle: { color: '#2d3436' } },
    legend: { data: ['会话数量', '新增用户'], top: 68, textStyle: { color: '#636e72' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 110, containLabel: true },
    xAxis: { type: 'category', data: trendData.map(item => item.date), axisLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.3)' } }, axisLabel: { color: '#636e72' } },
    yAxis: { type: 'value', name: '数量', axisLabel: { color: '#636e72' }, axisLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.3)' } }, splitLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.1)' } } },
    series: [
      { name: '会话数量', type: 'bar', data: trendData.map(item => item.sessions), itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#74b9ff' }, { offset: 1, color: '#0984e3' }] } }, barWidth: '40%' },
      { name: '新增用户', type: 'bar', data: trendData.map(item => item.newUsers), itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#fdcb6e' }, { offset: 1, color: '#f39c12' }] } }, barWidth: '40%' }
    ]
  })
}

// 用户活跃度趋势（折线图）
const initUserActivityChart = (trendData) => {
  if (!userActivityRef.value) return
  userActivityChart = echarts.init(userActivityRef.value)
  userActivityChart.setOption({
    title: { text: '用户活跃度趋势', textStyle: { fontSize: 16, fontWeight: 600, color: '#2d3436' }, left: 'center', top: 10 },
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.95)', borderColor: '#fab1a0', borderWidth: 1, textStyle: { color: '#2d3436' } },
    legend: { data: ['新增用户', '日记数量', '会话数量'], top: 40, textStyle: { color: '#636e72' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 80, containLabel: true },
    xAxis: { type: 'category', data: trendData.map(item => item.date), axisLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.3)' } }, axisLabel: { color: '#636e72' } },
    yAxis: { type: 'value', axisLabel: { color: '#636e72' }, axisLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.3)' } }, splitLine: { lineStyle: { color: 'rgba(244, 162, 97, 0.1)' } } },
    series: [
      { name: '新增用户', type: 'line', data: trendData.map(item => item.newUsers), smooth: true, lineStyle: { width: 3, color: '#a29bfe' }, itemStyle: { color: '#a29bfe' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(162, 155, 254, 0.4)' }, { offset: 1, color: 'rgba(162, 155, 254, 0.1)' }] } } },
      { name: '日记数量', type: 'line', data: trendData.map(item => item.diaries), smooth: true, lineStyle: { width: 3, color: '#00b894' }, itemStyle: { color: '#00b894' } },
      { name: '会话数量', type: 'line', data: trendData.map(item => item.sessions), smooth: true, lineStyle: { width: 3, color: '#fab1a0' }, itemStyle: { color: '#fab1a0' } }
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
