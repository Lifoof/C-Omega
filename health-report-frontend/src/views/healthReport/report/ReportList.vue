<template>
  <div class="page-container">
    <div class="page-header">
      <h3>{{ $t('report.title') }}</h3>
    </div>

    <div class="search-bar">
      <el-input
          v-model="query.memberName"
          :placeholder="$t('report.memberName')"
          clearable
          class="search-input"
          @keyup.enter="fetchData"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input
          v-model="query.modelName"
          :placeholder="$t('report.modelName')"
          clearable
          class="search-input"
          @keyup.enter="fetchData"
      >
        <template #prefix><el-icon><Cpu /></el-icon></template>
      </el-input>
      <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          :start-placeholder="$t('report.generationTime')"
          :end-placeholder="$t('report.generationTime')"
          class="search-date"
      />
      <el-select v-model="query.status" :placeholder="$t('common.status')" clearable class="search-input">
        <el-option :label="$t('report.statusGenerating')" :value="0" />
        <el-option :label="$t('report.statusCompleted')" :value="1" />
        <el-option :label="$t('report.statusFailed')" :value="2" />
      </el-select>
      <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>{{ $t('common.search') }}</el-button>
      <el-button @click="resetQuery"><el-icon><RefreshRight /></el-icon>{{ $t('common.reset') }}</el-button>
    </div>

    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" stripe width="100%">
        <el-table-column prop="reportNo" :label="$t('report.reportNo')" min-width="180" />
        <el-table-column prop="memberName" :label="$t('report.memberName')" min-width="100" />
        <el-table-column prop="modelName" :label="$t('report.modelName')" min-width="180" >
          <template #default="{ row }">
            {{ isEnglish() ? (row.modelNameEn || row.modelName) : row.modelName }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('report.completeness')" min-width="180">
          <template #default="{ row }">
            <el-progress
                :percentage="Math.round(Number(row.completeness) * 10000) / 100"
                :stroke-width="8"
                :color="Number(row.completeness) >= 0.7 ? '#67c23a' : '#e6a23c'"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.status')" min-width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'" size="small">
              {{ row.status === 1 ? $t('report.statusCompleted') : row.status === 2 ? $t('report.statusFailed') : $t('report.statusGenerating') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="generationTime" :label="$t('report.generationTime')" min-width="180" align="center" />
        <el-table-column :label="$t('common.operation')" min-width="200" fixed="right" align="center">
          <template #default="{ row }">
            <!-- 查看报告：状态不是失败(2) 才显示 -->
            <el-button
                v-if="row.status !== 2"
                link
                type="primary"
                @click="$router.push(`/report/${row.id}/view`)"
            >
              {{ $t('report.viewReport') }}
            </el-button>

            <!-- 下载报告：状态不是失败(2) 才显示 -->
            <el-button
                v-if="row.status !== 2"
                link
                type="success"
                @click="handleDownload(row)"
            >
              <el-icon><Download /></el-icon>
              {{ $t('report.downloadReport') }}
            </el-button>

            <!-- 删除按钮：一直显示 -->
            <el-popconfirm
                :title="$t('common.deleteConfirm')"
                @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button link type="danger">
                  {{ $t('common.delete') }}
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="query.pageNum"
            v-model:page-size="query.pageSize"
            :page-sizes="[10, 20, 30, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @current-change="fetchData"
            @size-change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="Report">
import { ref, reactive, onMounted, watch, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight, Cpu, Download } from '@element-plus/icons-vue'
import { getReportList, downloadReport, type Report,deleteReport } from "@/api/healthReport/report"
import {deleteCollection} from "@/api/healthReport/collection";

const { t: $t, locale } = useI18n()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const tableData = ref<Report[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])

// ====================== 智能自动刷新（安全版）======================
let refreshTimer: number | null = null
const POLL_INTERVAL = 5000 // 5秒刷新一次（非常友好）

function isEnglish(): boolean {
  return locale.value === 'en-US' || locale.value === 'en'
}

const query = reactive({
  memberName: '',
  modelName: '',
  collectionId: undefined as number | undefined,
  status: undefined as number | undefined,
  startDate: '',
  endDate: '',
  pageNum: 1,
  pageSize: 10,
})

watch(dateRange, (val) => {
  query.startDate = val?.[0] || ''
  query.endDate = val?.[1] || ''
})

// 数据刷新
async function fetchData() {
  loading.value = true
  try {
    const res = await getReportList(query)
    tableData.value = res.rows
    total.value = res.total
  } catch { /* handled */ } finally { loading.value = false }
}

// 重置查询
function resetQuery() {
  query.memberName = ''
  query.modelName = ''
  query.startDate = ''
  query.status = undefined
  query.endDate = ''
  query.pageNum = 1
  dateRange.value = []
  fetchData()
}

// 下载报告
async function handleDownload(row: Report) {
  try {
    if (!row.filePath) {
      ElMessage.warning($t('report.noPdfToDownload'))
      return
    }
    const lang = isEnglish() ? 'en' : 'zh'
    const res = await downloadReport(row.id, lang) as Blob
    const blob = new Blob([res], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const filename = `${$t('report.reportFileName')}-${row.memberName || row.reportNo}.pdf`
    a.download = filename
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success($t('report.downloadSuccess'))
  } catch (err) {
    ElMessage.error($t('report.downloadFailed'))
  }
}
async function handleDelete(id: number) {
  try {
    await deleteReport(id)
    ElMessage.success($t('common.success'))
    fetchData()
  } catch { /* handled */ }
}

// ====================== 智能轮询核心 ======================
/**
 * 启动智能刷新
 * 只有列表中有【生成中】任务时才会刷新
 * 全部完成后自动停止
 */
function startSmartPolling() {
  if (refreshTimer) return

  refreshTimer = window.setInterval(() => {
    const hasGenerating = tableData.value.some(row => row.status === 0)
    if (hasGenerating) {
      fetchData()
    } else {
      stopSmartPolling()
    }
  }, POLL_INTERVAL)
}

/**
 * 停止轮询并清理定时器
 * 防止内存泄漏
 */
function stopSmartPolling() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 页面加载时
onMounted(() => {
  if (route.query.refresh === '1') {
    const reportNos = route.query.reportNos as string
    if (reportNos) {
      ElMessage.success($t('report.generateInProgressDetail', { reportNos }))
    }
    router.replace({ path: route.path, query: {} })
  }

  fetchData()
  startSmartPolling() // 启动智能刷新
})

// 页面离开时 【关键：防内存泄漏】
onUnmounted(() => {
  stopSmartPolling()
})
</script>

<style lang="scss" scoped>
.page-container {
  padding: 24px;
  width: 100%;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;

  h3 {
    font-size: 1.25rem;
    font-weight: 600;
    color: #0f172a;
    letter-spacing: -0.02em;
    margin: 0;
  }
}

.search-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);

  .search-input {
    width: 200px;
    flex-shrink: 0;
  }

  .search-date {
    width: 280px;
    flex-shrink: 0;
  }

  .el-button {
    flex-shrink: 0;
  }
}

.table-wrapper {
  background: #ffffff;
  padding: 20px 22px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>