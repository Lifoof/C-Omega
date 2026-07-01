<template>
  <div class="page-container">
    <div class="page-header">
      <h3>{{ $t('common.helpCenter') }}</h3>
    </div>

    <!-- 1. 下载按钮区：用户一眼就能看到 -->
    <div class="download-card">
      <el-button type="primary" size="large" @click="downloadUserManual">
        <el-icon><Document /></el-icon>
        {{ $t('helpCenter.downloadUserManual') }}
      </el-button>
      <el-button type="success" size="large" @click="downloadDemoSample">
        <el-icon><Download /></el-icon>
        {{ $t('helpCenter.downloadDemoSample') }}
      </el-button>
    </div>

    <!-- 2. 使用流程说明：按步骤写清楚 -->
    <div class="guide-card">
      <h4>{{ $t('helpCenter.usageGuide') }}</h4>
      <el-steps direction="vertical" :active="4" finish-status="success">
        <el-step
            :title="$t('helpCenter.step1Title')"
            :description="$t('helpCenter.step1Desc')"
        />
        <el-step :title="$t('helpCenter.step2Title')">
          <template #description>
            <div style="white-space: pre-line; line-height: 1.5; margin-top: 4px;">
              {{ $t('helpCenter.step2Desc') }}
            </div>
          </template>
        </el-step>
        <el-step
            :title="$t('helpCenter.step3Title')"
            :description="$t('helpCenter.step3Desc')"
        />
        <el-step
            :title="$t('helpCenter.step4Title')"
            :description="$t('helpCenter.step4Desc')"
        />
      </el-steps>
    </div>

    <!-- 3. 注意事项：避免用户踩坑 -->
    <div class="warning-card">
      <el-alert :closable="false" type="warning">
        <template #title>{{ $t('helpCenter.importantNotes') }}</template>
        <ul>
          <li>{{ $t('helpCenter.note1') }}</li>
          <li>{{ $t('helpCenter.note2') }}</li>
          <li>{{ $t('helpCenter.note3') }}</li>
          <li>{{ $t('helpCenter.note4') }}</li>
        </ul>
      </el-alert>
    </div>

  </div>
</template>

<script setup lang="ts" name="HelpCenter">
import { ElMessage } from 'element-plus'
import { Document, Download } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

const { t: $t, locale } = useI18n()

// ====================== 下载用户手册 ======================
async function downloadUserManual() {
  try {
    const fileName = locale.value === 'en-US'
        ? 'UserManual.docx'
        : '用户手册.docx'

    // 用 fetch 获取文件，强制二进制流下载
    const response = await fetch(`/files/${fileName}?v=${Date.now()}`)
    if (!response.ok) throw new Error('文件不存在或服务器异常')

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)

    ElMessage.success($t('common.downloadSuccess'))
  } catch (e) {
    ElMessage.error('文件下载失败')
    console.error(e)
  }
}

// ====================== 下载示例样本 ======================
async function downloadDemoSample() {
  try {
    const fileName = locale.value === 'en-US'
        ? 'DemoSample.xlsx'
        : '示例样本.xlsx'

    const response = await fetch(`/files/${fileName}?v=${Date.now()}`)
    if (!response.ok) throw new Error('文件不存在或服务器异常')

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)

    ElMessage.success($t('common.downloadSuccess'))
  } catch (e) {
    ElMessage.error('文件下载失败')
    console.error(e)
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  padding: 24px;
  width: 100%;
  box-sizing: border-box;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;

  h3 {
    font-size: 1.25rem;
    font-weight: 600;
    color: #0f172a;
    margin: 0;
  }
}

/* 下载按钮区 */
.download-card {
  background: #fff;
  padding: 24px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
  margin-bottom: 20px;
  display: flex;
  gap: 16px;
}

/* 流程说明卡片 */
.guide-card {
  background: #fff;
  padding: 24px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
  margin-bottom: 20px;

  h4 {
    margin: 0 0 16px 0;
    font-size: 18px;
    font-weight: 600;
    color: #0f172a;
  }
}

/* 注意事项卡片 */
.warning-card {
  margin-bottom: 20px;
}

/* 常见问题卡片 */
.faq-card {
  background: #fff;
  padding: 24px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);

  h4 {
    margin: 0 0 16px 0;
    font-size: 18px;
    font-weight: 600;
    color: #0f172a;
  }
}
</style>