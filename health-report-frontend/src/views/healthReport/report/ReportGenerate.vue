<template>
  <div class="page-container">
    <div class="page-header">
      <h3>{{ $t('collection.generateReport') }}</h3>
      <el-button @click="$router.push('/collection')">{{ $t('common.back') }}</el-button>
    </div>

    <div class="form-card steps-card">
      <el-steps :active="step" finish-status="success" align-center>
        <el-step :title="$t('report.selectCollection')" />
        <el-step :title="$t('report.selectModel')" />
        <el-step :title="$t('collection.confirmGenerate')" />
      </el-steps>
    </div>

    <!-- Step 0: Select collection -->
    <div class="form-card" v-if="step === 0">
      <div class="step-content">
        <p class="step-label">{{ $t('report.selectCollection') }}</p>
        <el-select v-model="selectedCollectionId" filterable style="width: 100%;" :placeholder="$t('report.selectCollection')">
          <el-option v-for="c in collections" :key="c.id" :label="`${c.collectionNo} - ${c.memberName} (${c.completeness}%)`" :value="String(c.id)" />
        </el-select>
        <div class="step-actions">
          <el-button type="primary" @click="goToStep1" :disabled="!selectedCollectionId">{{ $t('collection.nextStep') }}</el-button>
        </div>
      </div>
    </div>

    <!-- Step 1: Select model -->
    <div class="form-card model-select-card" v-if="step === 1">
      <!-- Display selected collection info -->
      <div v-if="selectedCollectionInfo" class="selected-collection-info">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item :label="$t('collection.collectionNo')">{{ selectedCollectionInfo.collectionNo }}</el-descriptions-item>
          <el-descriptions-item :label="$t('collection.memberName')">{{ selectedCollectionInfo.memberName }}</el-descriptions-item>
          <el-descriptions-item :label="$t('collection.completeness')">
            <el-progress :percentage="Number(selectedCollectionInfo.completeness) || 0" :stroke-width="10" style="width: 120px" />
          </el-descriptions-item>
          <el-descriptions-item :label="$t('collection.checkupDate')">{{ selectedCollectionInfo.checkupDate || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <p class="step-label" style="margin-top: 16px;">{{ $t('report.selectModel') }}</p>
      <div class="model-grid-wrapper">
        <div class="model-grid">
          <div
              v-for="m in models"
              :key="m.id"
              class="model-card"
              :class="{ 'is-selected': selectedModelIds.includes(String(m.id)) }"
              @click="toggleModel(m)"
          >
            <h4>{{ getModelName(m) }}</h4>
            <p class="model-desc">{{ m.description }}</p>
            <div class="model-meta">
              <el-tag size="small">{{ getCategoryLabel(m.category) }}</el-tag>
              <el-tag size="small" :type="m.applicableGender === 0 ? '' : m.applicableGender === 1 ? 'primary' : 'danger'">
                {{ m.applicableGender === 0 ? $t('model.genderAll') : m.applicableGender === 1 ? $t('model.genderMale') : $t('model.genderFemale') }}
              </el-tag>
              <span class="param-count">{{ $t('model.filledKeyParams') }}: {{ m.filledKeyParams || 0 }}/{{ m.paramCount }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="models.length === 0" :description="$t('report.noModelsAvailable')" :image-size="80" />
      <div class="step-actions model-step-actions">
        <el-button @click="step = 0" v-if="!fromCollection">{{ $t('collection.prevStep') }}</el-button>
        <el-button type="primary" @click="checkAndProceed" :disabled="selectedModelIds.length === 0" :loading="checking">{{ $t('collection.nextStep') }}</el-button>
      </div>
    </div>

    <!-- Step 2: Confirm & check -->
    <div class="form-card" v-if="step === 2">
      <el-alert
          v-if="checkResult.missingRequired.length > 0"
          type="error"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
      >
        <template #title>
          <strong>{{ $t('collection.missingRequired') }}（{{ checkResult.missingRequired.length }} {{ $t('report.item') }}）</strong>
        </template>
        <div class="missing-list">
          <el-tag v-for="p in checkResult.missingRequired" :key="p" type="danger" size="small" style="margin: 2px 4px;">
            {{ getFieldDisplayName(p) }}
          </el-tag>
        </div>
        <p style="margin-top: 8px; font-size: 13px; color: #909399;">
          {{ $t('report.pleaseCompleteParams') }}
        </p>
      </el-alert>

      <el-alert
          v-if="checkResult.missingRequired.length === 0 && checkResult.overallCompleteness < 70"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
      >
        <template #title>{{ $t('collection.completenessWarning') }}</template>
        <p style="margin-top: 4px; font-size: 13px;" v-html="$t('report.completenessWarningDetail', {
          completeness: checkResult.overallCompleteness,
          filled: checkResult.filledKeyParams,
          total: checkResult.totalKeyParams
        })">
        </p>
      </el-alert>

      <el-alert
          v-if="checkResult.missingRequired.length === 0 && checkResult.overallCompleteness >= 70"
          type="success"
          :closable="false"
          show-icon
          style="margin-bottom: 16px;"
      >
        <template #title>{{ $t('report.dataValidationPassed') }}</template>
        <p style="margin-top: 4px; font-size: 13px;" v-html="$t('report.validationPassedDetail', {
          filled: checkResult.filledKeyParams,
          total: checkResult.totalKeyParams,
          completeness: checkResult.overallCompleteness
        })">
        </p>
        <div v-if="selectedModelIds.length > 0" style="margin-top: 8px;">
          <span style="font-size: 13px; font-weight: 500;">{{ $t('report.selectedModels') }}：</span>
          <el-tag v-for="modelId in selectedModelIds" :key="modelId" type="success" size="small" style="margin: 2px 4px;">
            {{ getModelName(models.find(m => String(m.id) === modelId)!) || modelId }}
          </el-tag>
        </div>
      </el-alert>

      <div class="check-stats" v-if="checkResult.totalKeyParams > 0">
        <div class="stat-item">
          <span class="stat-label">{{ $t('report.keyParamsLabel') }}</span>
          <el-progress
              :percentage="checkResult.keyCompleteness"
              :color="checkResult.missingRequired.length > 0 ? '#f56c6c' : '#67c23a'"
              :stroke-width="14"
              style="width: 200px"
          />
          <span class="stat-text">{{ checkResult.filledKeyParams }}/{{ checkResult.totalKeyParams }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">{{ $t('report.overallCompletenessLabel') }}</span>
          <el-progress
              :percentage="Math.min(100, checkResult.overallCompleteness)"
              :color="checkResult.overallCompleteness >= 70 ? '#67c23a' : '#e6a23c'"
              :stroke-width="14"
              style="width: 200px"
          />
          <span class="stat-text">{{ checkResult.overallCompleteness }}%</span>
        </div>
      </div>

      <div class="step-actions">
        <el-button @click="step = 1">{{ $t('collection.prevStep') }}</el-button>
        <el-button
            v-if="checkResult.missingRequired.length === 0"
            type="primary"
            @click="handleGenerate"
            :loading="generating"
        >{{ $t('collection.generateReport') }}</el-button>
        <el-button
            type="warning"
            @click="goToCollection"
        >{{ $t('keyParams.title') }}</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="ReportGenerate">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getCollectionList, getCollectionConfig, type CollectionRecord as Collection, type CategoryConfig } from "@/api/healthReport/collection"
import { getAvailableModelsWithParams, checkModelParams, type AiModel, type ModelCheckResult } from "@/api/healthReport/model"
import { generateReport } from "@/api/healthReport/report"

const { t: $t, locale } = useI18n()

// 判断是否为英文环境
function isEnglish(): boolean {
  return locale.value === 'en-US' || locale.value === 'en'
}

// 根据当前语言获取模型名称
function getModelName(model: AiModel): string {
  return isEnglish() ? (model.modelNameEn || model.modelName) : model.modelName
}

// 根据当前语言获取模型描述（暂时不需要英文）
// function getModelDescription(model: AiModel): string {
//   return isEnglish() ? (model.descriptionEn || model.description) : model.description
// }
const route = useRoute()
const router = useRouter()
const step = ref(0)
const generating = ref(false)
const checking = ref(false)
const fromCollection = ref(false)
const selectedCollectionId = ref<string>('')
const selectedModelIds = ref<string[]>([])
const collections = ref<Collection[]>([])
const models = ref<AiModel[]>([])
const fieldConfigs = ref<CategoryConfig[]>([])
const categoryLabels = ref({})
const loadingCategories = ref(false)
// 从路由参数中获取的性别，用于优先获取模型
const routeGender = ref<number | null>(null)

const checkResult = reactive<ModelCheckResult>({
  missingRequired: [],
  keyCompleteness: 0,
  overallCompleteness: 0,
  totalKeyParams: 0,
  filledKeyParams: 0,
})

const selectedCollectionInfo = computed(() => {
  if (!selectedCollectionId.value) return null
  return collections.value.find(c => String(c.id) === String(selectedCollectionId.value)) || null
})

function getFieldDisplayName(fieldCode: string): string {
  for (const cat of fieldConfigs.value) {
    for (const f of cat.fields) {
      if (f.fieldCode === fieldCode) {
        return isEnglish() ? (f.fieldNameEn || f.fieldName) : f.fieldName
      }
    }
  }
  return fieldCode
}

async function loadCollections() {
  try {
    const res = await getCollectionList({ current: 1, size: 200 })
    // 检查接口返回格式，支持 records 或 rows
    collections.value = res.records || res.rows || []
    await loadModels()
  } catch { /* handled */ }
}

async function loadModels() {
  try {

    // 优先使用路由参数中的性别，其次从选中的体检记录中获取
    let gender = routeGender.value;
    if (gender === null && selectedCollectionInfo.value) {
      gender = selectedCollectionInfo.value.memberGender;
    }

    const collectionId = selectedCollectionId.value;
    const res = await getAvailableModelsWithParams(gender, collectionId)
    models.value = res.data
    // 加载分类字典
    await loadCategories()
  } catch { /* handled */ }
}

async function initPage() {
  if (route.query.collectionId) {
    selectedCollectionId.value = String(route.query.collectionId)
    fromCollection.value = true
    step.value = 1
    // 从路由参数中获取性别
    if (route.query.gender) {
      routeGender.value = Number(route.query.gender)
    }
    await loadCollections()
    // 处理从体检录入页面传递的模型选择
    if (route.query.modelIds) {
      const modelIds = String(route.query.modelIds).split(',')
      selectedModelIds.value = modelIds
    }
  } else {
    await loadCollections()
  }
}

// 监听路由变化，重新初始化页面（只在有collectionId时）
watch(() => route.query.collectionId, (newVal) => {
  if (newVal) {
    step.value = 0
    selectedCollectionId.value = ''
    selectedModelIds.value = []
    routeGender.value = null
    if (route.query.gender) {
      routeGender.value = Number(route.query.gender)
    }
    Object.assign(checkResult, {
      missingRequired: [],
      keyCompleteness: 0,
      overallCompleteness: 0,
      totalKeyParams: 0,
      filledKeyParams: 0,
    })
    initPage()
  }
})

async function ensureFieldConfigs() {
  if (fieldConfigs.value.length > 0) return
  try {
    const res = await getCollectionConfig()
    fieldConfigs.value = res.data
  } catch { /* handled */ }
}

function getCategoryLabel(category: string) {
  // 优先使用 i18n 翻译
  const i18nLabel = $t(`model.categoryLabels.${category}`)
  if (i18nLabel && i18nLabel !== `model.categoryLabels.${category}`) {
    return i18nLabel
  }
  // 回退到字典数据
  return categoryLabels.value[category] || category
}

async function loadCategories() {
  if (loadingCategories.value) return

  loadingCategories.value = true
  try {
    const { getDicts } = await import('@/api/system/dict/data')
    const resp = await getDicts('model_category')

    const labels: Record<string, string> = {}
    resp.data.forEach((item: any) => {
      labels[item.dictValue] = item.dictLabel
    })

    categoryLabels.value = labels
  } catch {
    // 忽略错误
  } finally {
    loadingCategories.value = false
  }
}

function toggleModel(model: AiModel) {
  const modelId = String(model.id)
  const index = selectedModelIds.value.indexOf(modelId)
  if (index > -1) {
    selectedModelIds.value.splice(index, 1)
  } else {
    selectedModelIds.value.push(modelId)
  }
}

async function goToStep1() {
  step.value = 1
  await loadModels()
}

async function checkAndProceed() {
  checking.value = true
  try {
    if (selectedModelIds.value.length === 0) return

    const [res] = await Promise.all([
      checkModelParams(selectedModelIds.value, selectedCollectionId.value),
      ensureFieldConfigs(),
    ])
    Object.assign(checkResult, res.data)
    step.value = 2
  } catch { /* handled */ } finally {
    checking.value = false
  }
}

function goToCollection() {
  if (selectedCollectionId.value) {
    router.push({
      path: '/collection/key-params',
      query: {
        collectionId: selectedCollectionId.value,
        modelIds: selectedModelIds.value.join(',')
      }
    })
  } else {
    ElMessage.error($t('report.pleaseSelectCollectionFirst'))
  }
}

async function handleGenerate() {
  // 校验
  if (!selectedCollectionId.value) {
    ElMessage.warning($t('report.pleaseSelectCollection'));
    return;
  }
  if (!selectedModelIds.value || selectedModelIds.value.length === 0) {
    ElMessage.warning($t('report.pleaseSelectModel'));
    return;
  }

  generating.value = true;
  try {
    // ✅ 构造批量参数（一次提交所有模型）
    const params = {
      collectionId: selectedCollectionId.value,
      modelIds: selectedModelIds.value, // 直接传数组
    };

    // ✅ 调用一次批量接口，后端生成N个报告
    const res = await generateReport(params);
    const reportList = res.data || [];
    const reportNos = reportList.map((r: any) => r.reportNo).join(', ');

    // ✅ 显示生成中提示，然后跳转到报告列表
    ElMessage.success($t('report.generateInProgress', { reportNos }));
    router.push({
      path: '/report',
      query: { refresh: '1', reportNos: reportNos }
    });
  } catch (err) {
    ElMessage.error($t('report.generateFailed'));
    console.error(err);
  } finally {
    generating.value = false;
  }
}

onMounted(initPage)
</script>

<style lang="scss" scoped>
.page-container {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
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

.form-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
  padding: 24px;
  margin-bottom: 16px;
}

.steps-card {
  padding: 24px 48px;
}

.step-content {
  max-width: 600px;
}

.step-label {
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
  margin-bottom: 12px;
}

.step-actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.selected-collection-info {
  margin-bottom: 8px;
  :deep(.el-descriptions__label) { font-weight: 500; }
}

// 选择模型卡片：固定高度，内部滚动
.model-select-card {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 240px);
  overflow: hidden;

  .model-grid-wrapper {
    flex: 1;
    overflow-y: auto;
    max-height: 260px; // 调整为只显示两行卡片的高度
    margin-bottom: 0;
  }

  .model-step-actions {
    margin-top: 0;
    flex-shrink: 0;
    background: #fff;
    padding: 16px 0 0 0;
    position: relative;
    z-index: 10;
    box-shadow: 0 -4px 8px rgba(255, 255, 255, 0.9);
  }
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.model-card {
  cursor: pointer;
  transition: all 0.25s;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
  background: #fff;

  &:hover {
    border-color: #cbd5e1;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  }

  &.is-selected {
    border-color: #0d9488 !important;
    box-shadow: 0 0 12px rgba(13, 148, 136, 0.25);
    background: #f0fdfa;
  }

  h4 { margin: 0 0 8px; font-size: 15px; color: #0f172a; }
  .model-desc { color: #64748b; font-size: 13px; margin-bottom: 12px; }
  .model-meta { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
  .param-count { font-size: 12px; color: #64748b; }
}

.missing-list {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.check-stats {
  display: flex;
  gap: 32px;
  padding: 16px 0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-label {
  font-size: 13px;
  color: #0f172a;
  font-weight: 500;
  white-space: nowrap;
}

.stat-text {
  font-size: 13px;
  color: #0f172a;
  font-weight: 600;
  white-space: nowrap;
}
</style>
