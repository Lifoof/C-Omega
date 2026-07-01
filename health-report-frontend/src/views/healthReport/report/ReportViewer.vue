<template>
  <div class="page-container report-viewer">
    <div class="page-header" v-if="!isShared">
      <h3>{{ $t('report.viewReport') }}</h3>
      <div class="header-actions">
        <el-button type="primary" @click="handleDownload" :loading="downloading">
          <el-icon><Download /></el-icon> {{ $t('report.downloadReport') }}
        </el-button>
        <el-button @click="$router.back()">{{ $t('common.back') }}</el-button>
      </div>
    </div>

    <div v-loading="loading" class="report-wrapper">
      <template v-if="report">
        <div class="form-card report-meta-card">
          <el-descriptions :column="4" border size="small">
            <el-descriptions-item :label="$t('report.reportNo')">{{ report.reportNo }}</el-descriptions-item>
            <el-descriptions-item :label="$t('report.memberName')">{{ report.memberName }}</el-descriptions-item>
            <el-descriptions-item :label="$t('report.modelName')">{{ isEnglish() ? (report.modelNameEn || report.modelName) : report.modelName }}</el-descriptions-item>
            <el-descriptions-item :label="$t('report.completeness')">
              <el-progress
                  :percentage="Math.round(Number(report.completeness) * 10000) / 100"
                  :stroke-width="10"
                  :color="Number(report.completeness) >= 0.7 ? '#67c23a' : '#e6a23c'"
                  style="width: 120px"
              />
            </el-descriptions-item>
            <el-descriptions-item :label="$t('report.generationTime')">{{ report.generationTime }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="form-card report-content-card">
          <!-- PDF 预览 -->
          <div v-if="pdfUrl" class="pdf-preview">
            <iframe
                :src="`${pdfUrl}#toolbar=0&navpanes=0&scrollbar=1`"
                type="application/pdf"
                width="100%"
                height="100%"
                oncontextmenu="return false"
            ></iframe>
          </div>
          <div v-else-if="report.filePath" class="pdf-fallback">
            <el-alert
                :title="$t('report.pdfLoading')"
                type="info"
                :closable="false"
                show-icon
            />
          </div>
          <el-empty v-else :description="$t('report.noPdfFile')" />
        </div>
      </template>
      <el-empty v-else-if="!loading" :description="$t('common.noData')" />
    </div>
  </div>
</template>

<script setup lang="ts" name="ReportViewer">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Share } from '@element-plus/icons-vue'
import { getReportById, downloadReport, getShareLink, getSharedReport, type Report } from "@/api/healthReport/report"
import {useI18n} from "vue-i18n";
const { t: $t,locale } = useI18n()
const route = useRoute()
const loading = ref(false)
const downloading = ref(false)
const report = ref<Report | null>(null)
const pdfUrl = ref<string>('')
const pdfBlobUrl = ref<string>('') // 用于存储动态生成的PDF blob URL
const isShared = computed(() => !!route.params.token)
// 判断是否为英文环境
function isEnglish(): boolean {
  return locale.value === 'en-US' || locale.value === 'en'
}
// 构建 PDF 预览 URL
const baseUrl = import.meta.env.VITE_APP_BASE_API || ''

async function loadData() {
  loading.value = true
  try {
    if (isShared.value) {
      const res = await getSharedReport(route.params.token as string)
      report.value = res.data
    } else {
      const res = await getReportById(route.params.id as string)
      report.value = res.data
    }

    // 根据语言加载对应版本的 PDF
    await loadPdfByLanguage()
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

// 根据当前语言加载对应版本的 PDF
// 构建 PDF 预览 URL，自动带上时间戳
async function loadPdfByLanguage() {
  const lang = isEnglish() ? 'en' : 'zh'
  const filePath = lang === 'zh' ? report.value?.filePath : report.value?.filePathEn

  if (!filePath) {
    pdfUrl.value = ''
    return
  }

  // 🔥 关键：获取时间戳，拼接在 URL 后面
  const timeStop = localStorage.getItem('timeStop') || ''

  // 拼接地址
  let url = ''
  if (filePath.startsWith('http')) {
    url = filePath
  } else {
    url = baseUrl + filePath
  }

  // 自动带上 timeStop
  if (timeStop) {
    url += (url.includes('?') ? '&' : '?') + 'timeStop=' + timeStop
  }

  pdfUrl.value = url
}

// 监听语言变化，重新加载 PDF
watch(() => locale.value, () => {
  if (report.value) {
    loadPdfByLanguage()
  }
})

async function handleDownload() {
  try {
    const lang = isEnglish() ? 'en' : 'zh'
    const filePath = lang === 'zh' ? report.value?.filePath : report.value?.filePathEn

    // 1. 校验PDF路径是否存在
    if (!filePath) {
      ElMessage.warning($t('report.noPdfToDownload'))
      return
    }

    // 2. 直接调用后端文件下载接口（RuoYi标准写法），传递语言参数
    const res = await downloadReport(report.value.id, lang) as Blob
    const blob = new Blob([res], { type: 'application/pdf' }) // 🔥 关键：类型改为PDF
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    // 🔥 关键：文件名改为.pdf后缀
    const filename = `${$t('report.reportFileName')}-${report.value.memberName || report.value.reportNo}.pdf`
    a.download = filename
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success($t('report.downloadSuccess'))
  } catch (err) {
    ElMessage.error($t('report.downloadFailed'))
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.page-container {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
}

.report-viewer {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-shrink: 0;
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

.header-actions {
  display: flex;
  gap: 8px;
}

.report-wrapper {
  flex: 1;
  overflow-y: auto;
}

.form-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
  padding: 24px;
}

.report-meta-card {
  margin-bottom: 16px;
}

.report-content-card {
  padding: 0;
  overflow: hidden;
}

.report-body {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
  line-height: 1.8;

  :deep(h1) { font-size: 22px; margin: 0 0 8px; color: #0f172a; }
  :deep(h2) { font-size: 18px; margin: 20px 0 10px; color: #0f172a; }
  :deep(h3) { font-size: 15px; margin: 14px 0 8px; color: #0f172a; }
  :deep(ul) { padding-left: 20px; }
  :deep(li) { margin-bottom: 4px; }
  :deep(strong) { font-weight: 600; color: #0f172a; }
  :deep(p) { color: #334155; }
}

.pdf-preview {
  width: 100%;
  height: calc(100vh - 280px);
  min-height: 500px;

  iframe {
    border: none;
    width: 100%;
    height: 100%;
  }
}

.pdf-fallback {
  padding: 40px;
  text-align: center;
}
</style>
