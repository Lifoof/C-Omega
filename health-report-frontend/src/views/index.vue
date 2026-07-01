<template>
  <div class="dashboard">
    <!-- 顶部欢迎栏 -->
    <div class="dashboard-header">
      <div class="welcome">
        <h2>{{ $t('dashboard.title') }}</h2>
        <p class="welcome-sub">{{ currentDate }}</p>
      </div>
      <div class="time-switch">
        <el-radio-group v-model="timeRange" size="small" @change="loadData">
          <el-radio-button label="week">{{ $t('dashboard.lastWeek') }}</el-radio-button>
          <el-radio-button label="month">{{ $t('dashboard.lastMonth') }}</el-radio-button>
          <el-radio-button label="quarter">{{ $t('dashboard.lastQuarter') }}</el-radio-button>
          <el-radio-button label="year">{{ $t('dashboard.lastYear') }}</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 数据卡片 -->
    <div class="stat-cards" :class="{ 'stat-cards--user': !isAdmin }">
      <div class="stat-card stat-card--users" v-if="isAdmin">
        <div class="stat-icon"><el-icon :size="28"><User /></el-icon></div>
        <div class="stat-body">
          <div class="stat-num">{{ animatedUsers }}</div>
          <div class="stat-label">{{ $t('dashboard.totalUsers') }}</div>
        </div>
      </div>
      <div class="stat-card stat-card--reports">
        <div class="stat-icon"><el-icon :size="28"><Tickets /></el-icon></div>
        <div class="stat-body">
          <div class="stat-num">{{ animatedReports }}</div>
          <div class="stat-label">{{ $t('dashboard.totalReports') }}</div>
        </div>
      </div>
      <div class="stat-card stat-card--collections">
        <div class="stat-icon"><el-icon :size="28"><Document /></el-icon></div>
        <div class="stat-body">
          <div class="stat-num">{{ animatedCollections }}</div>
          <div class="stat-label">{{ $t('dashboard.totalCollections') }}</div>
        </div>
      </div>
      <div class="stat-card stat-card--model">
        <div class="stat-icon"><el-icon :size="28"><Cpu /></el-icon></div>
        <div class="stat-body">
          <div class="stat-num">{{ isEnglish() ? (stats.mostUsedModelEn || stats.mostUsedModel) : stats.mostUsedModel || '-' }}</div>
          <div class="stat-label">{{ $t('dashboard.mostUsedModel') }}</div>
          <div class="stat-sub">{{ stats.mostUsedModelCount }} {{ $t('model.callCount') }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区域：同一行显示 报告趋势 + 模型分布 -->
    <div class="chart-row">
      <!-- 管理员：左边用户趋势 -->
      <template v-if="isAdmin">
        <div class="chart-panel chart-panel--wide">
          <div class="chart-title">{{ $t('dashboard.userTrend') }}</div>
          <div class="chart-body" ref="userChartRef"></div>
        </div>
      </template>

      <!-- 普通用户：左边报告趋势 -->
      <template v-else>
        <div class="chart-panel chart-panel--wide">
          <div class="chart-title">{{ $t('dashboard.reportTrend') }}</div>
          <div class="chart-body chart-body--short" ref="reportChartRef"></div>
        </div>
      </template>

      <!-- 右边永远是模型分布，不动 -->
      <div class="chart-panel">
        <div class="chart-title">{{ $t('dashboard.modelUsage') }}</div>
        <div class="chart-body" ref="modelChartRef"></div>
      </div>
    </div>

    <!-- 管理员：下面再单独放一行报告趋势 -->
    <div class="chart-row" v-if="isAdmin">
      <div class="chart-panel chart-panel--full">
        <div class="chart-title">{{ $t('dashboard.reportTrend') }}</div>
        <div class="chart-body chart-body--short" ref="reportChartRef"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted, onBeforeUnmount, getCurrentInstance} from 'vue'
import {useI18n} from 'vue-i18n'
import * as echarts from 'echarts'
import {User, Tickets, Document, Cpu} from '@element-plus/icons-vue'
import {getOverviewStats, getUserTrend, getReportTrend, getModelUsageDistribution} from '@/api/healthReport/dashboard'
import useUserStore from '@/store/modules/user'

const {proxy} = getCurrentInstance()
const userStore = useUserStore()

// 判断是否为管理员
const isAdmin = computed(() => {
  return userStore.roles.includes('admin') || userStore.roles.includes('ROLE_ADMIN')
})

const stats = reactive({
  totalUsers: 0,
  totalReports: 0,
  totalCollections: 0,
  mostUsedModel: '',
  mostUsedModelEn: '',
  mostUsedModelCount: 0
})
const timeRange = ref('month')
const animatedUsers = ref(0)
const animatedReports = ref(0)
const animatedCollections = ref(0)

const userChartRef = ref()
const reportChartRef = ref()
const modelChartRef = ref()

let userChart
let reportChart
let modelChart
let resizeHandler = null

const {t: $t, locale} = useI18n()

// 判断是否为英文环境
function isEnglish() {
  return locale.value === 'en-US' || locale.value === 'en'
}

const currentDate = computed(() => {
  const lang = locale.value === 'en-US' ? 'en-US' : 'zh-CN'
  return new Date().toLocaleDateString(lang, {year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'})
})

function animateNumber(target, setter, duration = 800) {
  const start = 0
  const startTime = Date.now()
  const step = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    setter(Math.floor(start + (target - start) * progress))
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

async function loadData() {
  try {
    // 获取概览统计数据
    const overviewRes = await getOverviewStats(timeRange.value)
    if (overviewRes.code === 200) {
      Object.assign(stats, overviewRes.data)
      if (isAdmin.value) {
        animateNumber(stats.totalUsers, (v) => animatedUsers.value = v)
      }
      animateNumber(stats.totalReports, (v) => animatedReports.value = v)
      animateNumber(stats.totalCollections, (v) => animatedCollections.value = v)
    }

    // 获取用户趋势数据（仅管理员可见）
    if (isAdmin.value) {
      const userTrendRes = await getUserTrend(timeRange.value)
      if (userTrendRes.code === 200 && userChartRef.value) {
        if (!userChart) userChart = echarts.init(userChartRef.value)
        userChart.setOption({
          tooltip: {trigger: 'axis'},
          grid: {left: '3%', right: '4%', bottom: '3%', containLabel: true},
          xAxis: {
            type: 'category',
            data: userTrendRes.data.map(d => d.date),
            axisLine: {lineStyle: {color: '#e2e8f0'}},
            axisLabel: {color: '#64748b'}
          },
          yAxis: {
            type: 'value',
            splitLine: {lineStyle: {color: '#f1f5f9'}},
            axisLabel: {color: '#94a3b8'}
          },
          series: [{
            data: userTrendRes.data.map(d => d.count),
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {color: '#0d9488', width: 3},
            itemStyle: {color: '#0d9488'},
            areaStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  {offset: 0, color: 'rgba(13, 148, 136, 0.3)'},
                  {offset: 1, color: 'rgba(13, 148, 136, 0.05)'}
                ]
              }
            }
          }]
        })
      }
    }

    // 获取报告趋势数据
    const reportTrendRes = await getReportTrend(timeRange.value)
    if (reportTrendRes.code === 200 && reportChartRef.value) {
      if (!reportChart) reportChart = echarts.init(reportChartRef.value)
      reportChart.setOption({
        tooltip: {trigger: 'axis'},
        grid: {left: '3%', right: '4%', bottom: '3%', containLabel: true},
        xAxis: {
          type: 'category',
          data: reportTrendRes.data.map(d => d.date),
          axisLine: {lineStyle: {color: '#e2e8f0'}},
          axisLabel: {color: '#64748b'}
        },
        yAxis: {
          type: 'value',
          splitLine: {lineStyle: {color: '#f1f5f9'}},
          axisLabel: {color: '#94a3b8'}
        },
        series: [{
          data: reportTrendRes.data.map(d => d.count),
          type: 'bar',
          barWidth: '50%',
          itemStyle: {
            color: '#0d9488',
            borderRadius: [4, 4, 0, 0]
          }
        }]
      })
    }

    // 获取模型使用分布
    const modelUsageRes = await getModelUsageDistribution(timeRange.value)
    if (modelUsageRes.code === 200 && modelChartRef.value) {
      if (!modelChart) modelChart = echarts.init(modelChartRef.value)
      const palette = ['#0d9488', '#0ea5e9', '#f59e0b', '#ef4444', '#8b5cf6', '#10b981', '#ec4899']
      modelChart.setOption({
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(15,23,42,0.85)',
          borderColor: 'transparent',
          textStyle: {color: '#f1f5f9', fontSize: 12}
        },
        legend: {
          show: false
        },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          label: {
            formatter: '{b}\n{d}%',
            color: '#334155',
            fontSize: 12
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 20,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0,0,0,0.15)'
            }
          },
          data: modelUsageRes.data.map((d, i) => ({
            name: isEnglish() ? (d.modelNameEn || d.modelName) : d.modelName,
            value: d.count,
            itemStyle: {color: palette[i % palette.length]}
          }))
        }]
      })
    }
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
  }
}

onMounted(() => {
  loadData()
  resizeHandler = () => {
    userChart?.resize()
    reportChart?.resize()
    modelChart?.resize()
  }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeHandler)
  userChart?.dispose()
  reportChart?.dispose()
  modelChart?.dispose()
})
</script>

<style lang="scss" scoped>
.dashboard {
  padding: 20px;
  background: #f1f5f9;
  min-height: calc(100vh - 84px);
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .welcome {
    h2 {
      font-size: 24px;
      font-weight: 600;
      color: #0f172a;
      margin: 0 0 8px 0;
    }

    .welcome-sub {
      color: #64748b;
      font-size: 14px;
      margin: 0;
    }
  }

  .time-switch {
    :deep(.el-radio-button__inner) {
      border-color: #e2e8f0;
    }

    :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
      background-color: #0d9488;
      border-color: #0d9488;
      box-shadow: -1px 0 0 0 #0d9488;
    }
  }
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;

  // 普通用户：3个卡片铺满整行
  &--user {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
    &--user {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    &--user {
      grid-template-columns: 1fr;
    }
  }
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);

  &--users .stat-icon {
    background: linear-gradient(135deg, #0d9488 0%, #14b8a6 100%);
  }

  &--reports .stat-icon {
    background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
  }

  &--collections .stat-icon {
    background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
  }

  &--model .stat-icon {
    background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-body {
  flex: 1;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  margin-top: 4px;
}

.stat-sub {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 20px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
}

.chart-panel {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);

  &--wide {
    grid-column: span 1;
  }

  &--full {
    grid-column: span 2;

    @media (max-width: 1200px) {
      grid-column: span 1;
    }
  }

  // 普通用户：模型分布保持1/3宽度
  &--user {
    grid-column: span 1;
  }
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 16px;
}

.chart-body {
  height: 300px;

  &--short {
    height: 200px;
  }
}
</style>