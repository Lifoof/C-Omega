<template>
  <div class="page-container">
    <div class="page-header">
      <h3 class="page-title">{{ $t('keyParams.title') }}</h3>
      <div class="header-actions">
        <el-button type="primary" :loading="saving" @click="handleSaveAndGenerate">
          {{ $t('keyParams.saveAndGenerate') }}
        </el-button>
        <el-button @click="handleBack">{{ $t('common.back') }}</el-button>
      </div>
    </div>

    <div class="form-container" v-loading="loading">
      <div v-if="record" class="member-info">
        <span class="label">{{ $t('keyParams.user') }}：</span>
        <span class="value">{{ record.memberName }}</span>
        <span class="label" style="margin-left: 24px;">{{ $t('keyParams.collectionNo') }}：</span>
        <span class="value">{{ record.collectionNo }}</span>
      </div>

      <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="140px"
          class="key-params-form"
      >
        <div class="params-grid">
          <div
              v-for="field in keyFields"
              :key="field.fieldCode"
              class="param-item"
          >
            <el-form-item
                :label="getFieldName(field)"
                :prop="field.fieldCode"
                :rules="field.required ? [{ required: true, message: getRequiredMessage(field), trigger: 'blur' }] : []"
            >
              <div class="input-with-unit">
                <!-- 文本输入 -->
                <el-input
                    v-if="field.fieldType === 'TEXT'"
                    v-model="formData[field.fieldCode]"
                    :placeholder="getPlaceholder(field)"
                    class="param-input"
                />
                <span v-if="getUnit(field)&&field.fieldType === 'TEXT'" class="field-unit">{{ getUnit(field) }}</span>
                <el-tooltip
                    v-if="showHelpIcon(field)&&field.fieldType === 'TEXT'"
                    placement="top"
                    :raw-content="true"
                    :content="getTooltipContent(field)"
                >
                  <el-icon class="field-help"><QuestionFilled /></el-icon>
                </el-tooltip>
                <!-- 数字输入 -->
                <el-input
                    v-else-if="field.fieldType === 'NUMBER'"
                    v-model="formData[field.fieldCode]"
                    type="number"
                    :placeholder="getPlaceholder(field)"
                    class="param-input"
                />
                <span v-if="getUnit(field)&&field.fieldType === 'NUMBER'" class="field-unit">{{ getUnit(field) }}</span>
                <el-tooltip
                    v-if="showHelpIcon(field)&&field.fieldType === 'NUMBER'"
                    placement="top"
                    :raw-content="true"
                    :content="getTooltipContent(field)"
                >
                  <el-icon class="field-help"><QuestionFilled /></el-icon>
                </el-tooltip>
                <!-- 日期选择 -->
                <el-date-picker
                    v-else-if="field.fieldType === 'date'"
                    v-model="formData[field.fieldCode]"
                    type="date"
                    :placeholder="getSelectPlaceholder(field)"
                    class="param-input"
                    value-format="YYYY-MM-DD"
                />
                <el-tooltip
                    v-if="showHelpIcon(field)&&field.fieldType === 'date'"
                    placement="top"
                    :raw-content="true"
                    :content="getTooltipContent(field)"
                >
                  <el-icon class="field-help"><QuestionFilled /></el-icon>
                </el-tooltip>
                <!-- 下拉选择 -->
                <el-select
                    v-else-if="field.fieldType === 'select'"
                    v-model="formData[field.fieldCode]"
                    :placeholder="getSelectPlaceholder(field)"
                    class="param-input"
                >
                  <el-option
                      v-for="opt in parseOptions(field.enumOptions)"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                  />
                </el-select>
                <el-tooltip
                    v-if="showHelpIcon(field)&&field.fieldType === 'select'"
                    placement="top"
                    :raw-content="true"
                    :content="getTooltipContent(field)"
                >
                  <el-icon class="field-help"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
            </el-form-item>
          </div>
        </div>
      </el-form>

    </div>
  </div>
</template>

<script setup lang="ts" name="KeyParamsFill">
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { getCollectionDetail, getCollectionConfig, saveBatchCategoryData, type CollectionRecord, type CategoryConfig, type FieldConfig } from '@/api/healthReport/collection'
import { checkModelParams, getModelById } from '@/api/healthReport/model'
import { generateReport } from '@/api/healthReport/report'
import {QuestionFilled} from "@element-plus/icons-vue";

// 扩展FieldConfig接口，增加必填标记
interface ExtendedFieldConfig extends FieldConfig {
  required: boolean
}

const { t: $t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()

const loading = ref(false)
const saving = ref(false)
const record = ref<CollectionRecord | null>(null)
const collectionId = ref<number>(0)
const selectedModelIds = ref<string[]>([])
const keyFields = ref<ExtendedFieldConfig[]>([])
const formData = reactive<Record<string, any>>({})
const formRules = reactive<Record<string, any>>({})

// 解析下拉选项
function parseOptions(enumOptions?: string) {
  if (!enumOptions) return []
  try {
    return JSON.parse(enumOptions)
  } catch {
    return enumOptions.split(',').map(item => {
      const [value, label] = item.split(':')
      return { value: value || item, label: label || item }
    })
  }
}

// 判断是否为英文环境
function isEnglish(): boolean {
  return locale.value === 'en-US' || locale.value === 'en'
}

// 根据当前语言获取字段名称
function getFieldName(field: any): string {
  if (!field) return ''
  return isEnglish() ? (field.fieldNameEn || field.fieldName) : field.fieldName
}

// 根据当前语言获取单位
function getUnit(field: any): string {
  if (!field) return ''
  return isEnglish() ? (field.unitEn || field.unit) : field.unit
}

// 根据当前语言获取参考值范围
function getRefRangeText(field: any): string {
  if (!field) return ''
  return isEnglish() ? (field.refRangeTextEn || field.refRangeText) : field.refRangeText
}

// 获取必填验证消息
function getRequiredMessage(field: any): string {
  const fieldName = getFieldName(field)
  return isEnglish() ? `Please enter ${fieldName}` : `请输入${fieldName}`
}

// 获取输入框占位符
function getPlaceholder(field: any): string {
  const refRange = getRefRangeText(field)
  if (refRange&&refRange.length <= PLACEHOLDER_MAX_LENGTH) {
    return isEnglish() ? `Please Enter (${refRange})` : `请输入 (${refRange})`
  }
  return isEnglish() ? `Please Enter` : `请输入`
}
// 输入框提示语最大长度，超过就放进问号
const PLACEHOLDER_MAX_LENGTH = 10
// 判断是否需要显示问号图标
function showHelpIcon(field: any) {
  const range = getRefRangeText(field)
  // 参考范围太长 或 有别名/备注 → 显示问号
  return (range && range.length > PLACEHOLDER_MAX_LENGTH)
}

// 获取问号里面显示的完整内容（自动换行 + 中英文）
function getTooltipContent(field: any) {
  const parts = []

  // 参考范围
  const range = getRefRangeText(field)
  if (range&& range.length > PLACEHOLDER_MAX_LENGTH) {
    parts.push($t('collection.referenceRange') + range)
  }

  // 关键：用 \n 换行，Element Plus tooltip 自动识别
  return parts.join('<br/>')
}

// 获取选择框占位符
function getSelectPlaceholder(field: any): string {
  const fieldName = getFieldName(field)
  return isEnglish() ? `Select ${fieldName}` : `请选择${fieldName}`
}

// 获取模型关联的关键参数
async function loadKeyParams() {
  loading.value = true
  try {
    // 获取采集记录详情
    const detailRes = await getCollectionDetail(collectionId.value)
    record.value = detailRes.data.record

    // 获取所有配置
    const configRes = await getCollectionConfig(record.value.memberGender,'衍生变量')
    const allCategories: CategoryConfig[] = configRes.data

    // 获取所有选中模型的详情，收集所有参数和必填参数
    const allParamCodes = new Set<string>()
    const requiredParamCodes = new Set<string>()

    for (const modelId of selectedModelIds.value) {
      try {
        const modelRes = await getModelById(Number(modelId))
        const model = modelRes.data

        // 收集所有参数
        if (model.allParams) {
          let params: string[] = []
          if (typeof model.allParams === 'string') {
            try { params = JSON.parse(model.allParams) } catch { params = [] }
          } else if (Array.isArray(model.allParams)) {
            params = model.allParams
          }
          params.forEach(code => allParamCodes.add(code))
        }

        // 收集必填参数
        if (model.requiredParams) {
          let required: string[] = []
          if (typeof model.requiredParams === 'string') {
            try { required = JSON.parse(model.requiredParams) } catch { required = [] }
          } else if (Array.isArray(model.requiredParams)) {
            required = model.requiredParams
          }
          required.forEach(code => requiredParamCodes.add(code))
        }
      } catch (e) {
        console.error(`获取模型 ${modelId} 详情失败`, e)
      }
    }

    // 从所有配置中筛选出模型的所有参数
    const fields: ExtendedFieldConfig[] = []
    allCategories.forEach(category => {
      category.fields.forEach(field => {
        // 匹配字段代码或字段名称
        if (allParamCodes.has(field.fieldCode) || allParamCodes.has(field.fieldName)) {
          fields.push({
            ...field,
            required: requiredParamCodes.has(field.fieldCode) || requiredParamCodes.has(field.fieldName)
          })
        }
      })
    })

    // 去重（按fieldCode）
    const uniqueFields: ExtendedFieldConfig[] = []
    const seenCodes = new Set<string>()
    fields.forEach(field => {
      if (!seenCodes.has(field.fieldCode)) {
        seenCodes.add(field.fieldCode)
        uniqueFields.push(field)
      }
    })

    // 排序：必填参数在前，然后按分类和排序号
    uniqueFields.sort((a, b) => {
      // 必填参数排在最前面
      if (a.required !== b.required) {
        return a.required ? -1 : 1
      }
      // 然后按分类排序
      if (a.categoryCode !== b.categoryCode) {
        return a.categoryCode.localeCompare(b.categoryCode)
      }
      // 最后按排序号
      return a.sortOrder - b.sortOrder
    })

    keyFields.value = uniqueFields

    // 初始化表单数据
    uniqueFields.forEach(field => {
      formData[field.fieldCode] = null
    })

    // 加载已有数据
    if (detailRes.data.categoryDataList) {
      detailRes.data.categoryDataList.forEach((cd: any) => {
        if (cd.fieldData) {
          Object.entries(cd.fieldData).forEach(([key, value]) => {
            if (key in formData) {
              formData[key] = value
            }
          })
        }
      })
    }
  } catch (e: any) {
    ElMessage.error(e?.message || $t('report.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleSaveAndGenerate() {
  if (!formRef.value) return

  // 手动验证必填参数
  const requiredFields = keyFields.value.filter(f => f.required)
  const missingFields = requiredFields.filter(f => {
    const value = formData[f.fieldCode]
    return value === null || value === undefined || value === ''
  })

  if (missingFields.length > 0) {
    const missingNames = missingFields.map(f => getFieldName(f)).join(isEnglish() ? ', ' : '、')
    const message = isEnglish() ? `Please fill in required fields: ${missingNames}` : `请填写必填参数：${missingNames}`
    ElMessage.error(message)
    return
  }

  saving.value = true
  try {
    // ==============================================
    // 🔥 一次性构建所有分类数据（不再循环调用）
    // ==============================================
    const allCategoryData = []
    const categoryDataMap: Record<string, Record<string, any>> = {}

    keyFields.value.forEach(field => {
      if (!categoryDataMap[field.categoryCode]) {
        categoryDataMap[field.categoryCode] = {}
      }
      categoryDataMap[field.categoryCode][field.fieldCode] = formData[field.fieldCode]
    })

    // 转成数组传给后端
    for (const [categoryCode, fieldData] of Object.entries(categoryDataMap)) {
      allCategoryData.push({
        categoryCode,
        fieldData
      })
    }

    // ==============================================
    // 🔥 调用【批量保存接口】一次搞定
    // ==============================================
    await saveBatchCategoryData({
      collectionId: collectionId.value,
      categoryList: allCategoryData  // 所有分类+所有字段
    })

    // 生成报告
    const params = {
      collectionId: collectionId.value,
      modelIds: selectedModelIds.value,
    }
    await generateReport(params)

    ElMessage.success($t('keyParams.generateSuccess'))
    router.push('/report')
  } catch (e: any) {
    ElMessage.error(e?.message || $t('common.failed'))
  } finally {
    saving.value = false
  }
}

// 返回
function handleBack() {
  // 返回到生成报告页面
  router.push({
    path: '/collection/generate',
    query: {
      collectionId: collectionId.value,
      modelIds: selectedModelIds.value.join(',')
    }
  })
}

// 加载页面数据
function initPage() {
  const id = route.query.collectionId
  const models = route.query.modelIds

  if (!id || !models) {
    ElMessage.error($t('report.paramError'))
    router.push('/report')
    return
  }

  collectionId.value = Number(id)
  selectedModelIds.value = String(models).split(',')
  loadKeyParams()
}

onMounted(() => {
  initPage()
})

// 每次激活页面时都重新加载（处理缓存情况）
onActivated(() => {
  initPage()
})
</script>

<style lang="scss" scoped>
.page-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.form-container {
  background: #fff;
  border-radius: 4px;
  padding: 24px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.member-info {
  margin-bottom: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;

  .label {
    color: #606266;
    font-size: 14px;
  }

  .value {
    color: #303133;
    font-size: 14px;
    font-weight: 500;
  }
}

.params-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 40px;
}

.param-item {
  :deep(.el-form-item) {
    margin-bottom: 16px;
    align-items: flex-start;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
    font-weight: 500;
    color: #606266;
    min-width: 200px;
    max-width: 420px;
    width: 45%;
    white-space: normal;
    word-break: break-all;
    overflow-wrap: break-word;
    text-align: left;
    line-height: 1.5;
    padding-top: 8px;
    flex-shrink: 1;
  }

  :deep(.el-form-item__content) {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  :deep(.el-input) {
    flex-shrink: 1;
    min-width: 150px;
    width: 180px;
  }

  :deep(.el-select) {
    flex-shrink: 1;
    min-width: 150px;
    width: 180px;
  }

  :deep(.el-date-picker) {
    flex-shrink: 1;
    min-width: 150px;
    width: 180px;
  }
}

.input-with-unit {
  display: flex;
  align-items: center;
  width: 100%;
}

.param-input {
  flex-shrink: 1;
  min-width: 150px;
  width: 180px;
}

.param-input :deep(.el-input__inner),
.param-input :deep(.el-input-number__decrease),
.param-input :deep(.el-input-number__increase) {
  height: 32px;
  line-height: 32px;
}

.field-unit {
  margin-left: 8px;
  color: #64748b;
  font-size: 13px;
  flex-shrink: 0;
}

.form-actions {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: center;
  gap: 16px;
}

@media (max-width: 768px) {
  .params-grid {
    grid-template-columns: 1fr;
  }
}
</style>
