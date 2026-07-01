<template>
  <div class="page-container">
    <!-- 列表视图 -->
    <template v-if="!showEntry">
      <div class="page-header">
        <h3>{{ $t('collection.title') }}</h3>
        <div class="header-actions">
          <el-button type="primary" @click="handleBatchImport">
            <el-icon><Upload /></el-icon>{{ $t('collection.batchImport') }}
          </el-button>
          <el-button type="primary" @click="handleDownloadTemplate">
            <el-icon><Download /></el-icon>{{ $t('collection.downloadTemplate') }}
          </el-button>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>{{ $t('common.create') }}
          </el-button>
        </div>
      </div>

      <div class="search-bar">
        <el-input
            v-model="query.memberName"
            :placeholder="$t('collection.memberName')"
            clearable
            class="search-input"
            @keyup.enter="fetchData"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            :start-placeholder="$t('collection.checkupDate')"
            :end-placeholder="$t('collection.checkupDate')"
            class="search-date"
        />
        <el-select v-model="query.reportStatus" clearable :placeholder="$t('collection.reportGenerateStatus')" class="search-select">
          <el-option :label="$t('collection.reportGenerated')" :value="2" />
          <el-option :label="$t('collection.reportNotGenerated')" :value="0" />
        </el-select>
        <el-select v-model="query.status" clearable :placeholder="$t('collection.status')" class="search-select">
          <el-option :label="$t('collection.statusDraft')" :value="0" />
          <el-option :label="$t('collection.statusComplete')" :value="1" />
        </el-select>
        <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>{{ $t('common.search') }}</el-button>
        <el-button @click="resetQuery"><el-icon><RefreshRight /></el-icon>{{ $t('common.reset') }}</el-button>
      </div>

      <div class="table-wrapper">
        <el-table :data="tableData" v-loading="loading" stripe @sort-change="handleSortChange">
          <el-table-column prop="collectionNo" :label="$t('collection.collectionNo')" width="160" />
          <el-table-column prop="memberName" :label="$t('collection.memberName')" width="100" />
          <el-table-column :label="$t('personalInfo.gender')" width="70" align="center">
            <template #default="{ row }">
              {{ row.memberGender === 1 ? $t('personalInfo.male') : row.memberGender === 2 ? $t('personalInfo.female') : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="checkupDate" :label="$t('collection.checkupDate')" width="100" sortable />
          <el-table-column :label="$t('collection.source')" width="100">
            <template #default="{ row }">
              <el-tag :type="row.sourceType === 1 ? '' : 'success'" size="small">
                {{ row.sourceType === 1 ? $t('collection.sourceManual') : $t('collection.sourceExcel') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('collection.completeness')" width="140" sortable prop="completeness">
            <template #default="{ row }">
              <el-progress
                  :percentage="Number(row.completeness) || 0"
                  :color="Number(row.completeness) >= 70 ? '#67c23a' : '#e6a23c'"
                  :stroke-width="8"
              />
            </template>
          </el-table-column>
          <el-table-column :label="$t('collection.status')" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
                {{ row.status === 1 ? $t('collection.statusComplete') : $t('collection.statusDraft') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('collection.reportGenerateStatus')" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.reportStatus === 2 ? 'success' : 'info'" size="small">
                {{ row.reportStatus === 2 ? $t('collection.reportGenerated') : $t('collection.reportNotGenerated') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="$t('personalInfo.uploadTime')" width="160" align="center" sortable />
          <el-table-column :label="$t('common.operation')" width="210" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEdit(row)">
                {{ $t('collection.continueEntry') }}
              </el-button>
              <el-button link type="success" @click="handleGenerateReport(row)">
                {{ $t('collection.generateReport') }}
              </el-button>
              <el-popconfirm :title="$t('common.deleteConfirm')" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button link type="danger">{{ $t('common.delete') }}</el-button>
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

      <!-- 新建采集对话框 -->
      <el-dialog
          v-model="showCreateDialog"
          :title="$t('menu.collectionCreate')"
          width="620px"
          :close-on-click-modal="false"
          class="create-dialog"
          top="12vh"
      >
        <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="140px" size="large">
          <el-form-item :label="$t('collection.selectMember')" prop="memberId">
            <el-select
                v-model="createForm.memberId"
                filterable
                :placeholder="$t('collection.selectMember')"
                style="width: 100%"
            >
              <el-option
                  v-for="m in memberOptions"
                  :key="m.id"
                  :label="`${m.name} (${m.gender === 1 ? $t('personalInfo.male') : m.gender === 2 ? $t('personalInfo.female') : '-'})`"
                  :value="m.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('collection.checkupDate')" prop="checkupDate">
            <el-date-picker v-model="createForm.checkupDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :placeholder="$t('collection.checkupDate')" />
          </el-form-item>
          <el-form-item :label="$t('collection.hospital')">
            <el-autocomplete
                v-model="createForm.hospital"
                :fetch-suggestions="queryHospital"
                :placeholder="$t('collection.hospital')"
                style="width: 100%"
                clearable
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button size="large" @click="showCreateDialog = false">{{ $t('common.cancel') }}</el-button>
            <el-button size="large" type="primary" :loading="creating" @click="handleCreate">{{ $t('common.confirm') }}</el-button>
          </div>
        </template>
      </el-dialog>
    </template>

    <!-- 录入视图 -->
    <template v-else>
      <div class="entry-page">
        <div class="page-header">
          <div class="header-left">
            <el-button @click="handleBackToList">{{ $t('common.back') }}</el-button>
            <h3 v-if="record">{{ record.memberName }} - {{ isEditEntry ? $t('collection.continueEntry') : $t('menu.collectionCreate') }}</h3>
            <div class="model-select-section">
              <div class="model-section-title">{{ $t('report.selectModel') }}</div>
              <div class="model-grid">
                <div
                    v-for="model in modelOptions"
                    :key="model.id"
                    class="model-card"
                    :class="{ 'is-selected': selectedModels.some(m => m.id === model.id) }"
                    @click="toggleModel(model)"
                >
                  <h4>{{ model.modelName }}</h4>
                  <div class="model-meta">
                    <el-tag size="small">{{ getCategoryLabel(model.category) }}</el-tag>
                    <el-tag size="small" :type="model.applicableGender === 0 ? 'success' : model.applicableGender === 1 ? 'primary' : 'danger'">
                      {{ model.applicableGender === 0 ? $t('model.genderAll') : model.applicableGender === 1 ? $t('common.male') : $t('common.female') }}
                    </el-tag>
                    <span class="param-count">{{ $t('model.keyParamsLabel') }} {{ model.paramCount || 0 }}</span>
                  </div>
                </div>
                <div v-if="modelOptions.length === 0" class="no-models">
                  {{ $t('model.noModelsAvailable') }}
                </div>
              </div>
            </div>
          </div>
          <div class="header-right">
            <!-- OCR上传按钮 -->
            <div class="ocr-upload-section">
              <el-upload
                  ref="ocrImageUploadRef"
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleOcrImageChange"
                  accept="image/*"
                  multiple
                  class="ocr-upload-btn"
              >
                <el-button type="warning" :loading="ocrLoading">
                  <el-icon><Picture /></el-icon>
                  {{ $t('collection.uploadImage') }}
                </el-button>
              </el-upload>
              <el-upload
                  ref="ocrPdfUploadRef"
                  action="#"
                  :auto-upload="false"
                  :show-file-list="false"
                  :on-change="handleOcrPdfChange"
                  accept=".pdf"
                  multiple
                  class="ocr-upload-btn"
              >
                <el-button type="info" :loading="ocrLoading">
                  <el-icon><Document /></el-icon>
                  {{ $t('collection.uploadPdf') }}
                </el-button>
              </el-upload>
            </div>
            <div class="completion-info">
              <el-progress type="circle" :percentage="overallPct" :width="52" :stroke-width="5" />
              <span class="pct-label">{{ $t('collection.overallCompletion', { percent: overallPct }) }}</span>
            </div>
            <el-button type="success" :disabled="selectedModels.length === 0" @click="handleComplete">
              {{ $t('collection.generateReport') }}
            </el-button>
          </div>
        </div>

        <div class="entry-body" v-loading="entryLoading">
          <div class="category-nav">
            <div
                v-for="(cat, idx) in categories"
                :key="cat.categoryCode"
                class="nav-item"
                :class="{ active: activeIndex === idx, 'has-data': getCategoryFilled(cat.categoryCode) > 0 }"
                @click="switchCategory(idx)"
            >
              <div class="nav-icon">
                <el-icon><component :is="cat.icon || 'Document'" /></el-icon>
              </div>
              <div class="nav-text">
                <span class="nav-name">{{ getCategoryName(cat) }}</span>
                <span class="nav-count">{{ getCategoryFilled(cat.categoryCode) }}/{{ cat.fields.length }}</span>
              </div>
              <el-icon v-if="getCategoryFilled(cat.categoryCode) === cat.fields.length && cat.fields.length > 0" class="nav-check" color="#67c23a"><CircleCheckFilled /></el-icon>
            </div>
          </div>

          <div class="form-area" v-if="currentCategory">
            <div class="form-card">
              <div class="form-header">
                <div>
                  <span class="form-title">{{ getCategoryName(currentCategory) }}</span>
                  <el-tag size="small" type="info" style="margin-left: 8px">
                    {{ $t('collection.filledCount', { filled: getCategoryFilled(currentCategory.categoryCode), total: currentCategory.fields.length }) }}
                  </el-tag>
                </div>
                <el-button type="primary" size="small" @click="saveCurrentCategory(false)" :loading="saving">
                  {{ $t('collection.saveCurrentStep') }}
                </el-button>
              </div>

              <!-- 两列布局表单 -->
              <el-form label-width="140px" label-position="right" class="field-form two-column-form">
                <div class="form-row">
                  <template v-for="(field, index) in currentCategory.fields" :key="field.fieldCode">
                    <el-form-item
                        :label="getFieldName(field)"
                        :required="field.isRequired === 1"
                        :class="{ 'has-error': fieldErrors[field.fieldCode] }"
                        :style="{ width: '50%', boxSizing: 'border-box', paddingRight: index % 2 === 0 ? '20px' : '0' }"
                    >
                      <template v-if="field.fieldType === 'NUMBER'">
                        <el-input
                            v-model="formData[field.fieldCode]"
                            type="number"
                            :placeholder="getRefRangeText(field) ? $t('collection.pleaseEnterWithRange', { range: getRefRangeText(field) }) : $t('collection.pleaseEnter')"
                            style="width: 200px"
                            @blur="validateField(field)"
                            @input="onFieldInput()"
                        />
                        <span class="field-unit" v-if="getUnit(field)">{{ getUnit(field) }}</span>
                        <el-tooltip v-if="getAliasName(field) || getRemark(field)" placement="top">
                          <template #content>
                            <div v-if="getAliasName(field)">{{ $t('collection.aliasLabel') }} {{ getAliasName(field) }}</div>
                            <div v-if="getRemark(field)">{{ $t('collection.remarkLabel') }} {{ getRemark(field) }}</div>
                          </template>
                          <el-icon class="field-help"><QuestionFilled /></el-icon>
                        </el-tooltip>
                      </template>

                      <template v-else-if="field.fieldType === 'TEXT'">
                        <el-input
                            v-model="formData[field.fieldCode]"
                            :placeholder="$t('collection.pleaseEnter')"
                            style="width: 200px"
                            @input="onFieldInput()"
                        />
                      </template>

                      <template v-else-if="field.fieldType === 'ENUM'">
                        <el-select v-model="formData[field.fieldCode]" clearable style="width: 200px" @change="onFieldInput()">
                          <el-option
                              v-for="opt in parseEnumOptions(field.enumOptions)"
                              :key="opt.value"
                              :label="opt.label"
                              :value="opt.value"
                          />
                        </el-select>
                      </template>

                      <template v-else-if="field.fieldType === 'BOOLEAN'">
                        <el-radio-group v-model="formData[field.fieldCode]" @change="onFieldInput()">
                          <el-radio :value="true">{{ $t('common.yes') }}</el-radio>
                          <el-radio :value="false">{{ $t('common.no') }}</el-radio>
                        </el-radio-group>
                      </template>

                      <el-tooltip v-if="field.description" :content="field.description" placement="top">
                        <el-icon class="field-help"><QuestionFilled /></el-icon>
                      </el-tooltip>

                      <div class="field-error" v-if="fieldErrors[field.fieldCode]">
                        {{ fieldErrors[field.fieldCode] }}
                      </div>
                    </el-form-item>
                  </template>
                </div>
              </el-form>
            </div>

            <div class="form-actions">
              <el-button @click="prevCategory" :disabled="activeIndex === 0">
                <el-icon><ArrowLeft /></el-icon> {{ $t('collection.prevStep') }}
              </el-button>
              <el-button type="primary" @click="nextCategory" :disabled="activeIndex === categories.length - 1">
                {{ $t('collection.nextStep') }} <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 批量导入对话框 -->
    <el-dialog
        v-model="batchImportDialogVisible"
        :title="$t('collection.batchImport')"
        width="500px"
        :close-on-click-modal="false"
    >
      <el-form label-width="100px">
        <el-form-item :label="$t('collection.selectFile')" required>
          <el-upload
              ref="batchImportUploadRef"
              v-model:file-list="batchImportFileList"
              action="#"
              multiple
              :auto-upload="false"
              accept=".xlsx,.xls"
          >
            <el-button type="primary">{{ $t('collection.selectFile') }}</el-button>
            <template #tip>
              <div class="el-upload__tip">{{ $t('collection.fileTypeTip') }}</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchImportDialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="batchImportLoading" @click="confirmBatchImport">
          {{ $t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import * as XLSX from 'xlsx'
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, Search, RefreshRight, ArrowLeft, ArrowRight, CircleCheckFilled, QuestionFilled, Download, Upload, Picture, Document } from '@element-plus/icons-vue'
import {
  getCollectionList,
  createCollection,
  deleteCollection,
  getCollectionConfig,
  getCollectionDetail,
  saveCategoryDataItem,
  completeCollection,
  batchImportCollection,
  type CollectionRecord,
  type CollectionQuery,
  type CategoryConfig,
  type BatchImportResponse,
  getCollectionConfigName,
} from '@/api/healthReport/collection'
import { download } from "@/utils/request"
import { listMemberInfo as getPersonalInfoList, type PersonalInfo } from "@/api/healthReport/personalInfo"
import { getAvailableModels, type AiModel } from '@/api/healthReport/model'
import { extractIndicators } from '@/api/healthReport/ocr'

const { t: $t, locale } = useI18n()
const router = useRouter()

// ========== 列表相关 ==========
const loading = ref(false)
const tableData = ref<CollectionRecord[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])
const query = reactive<CollectionQuery & { status?: number; orderByColumn?: string; isAsc?: string }>({
  memberName: '',
  startDate: '',
  endDate: '',
  reportStatus: undefined as any,
  status: undefined as any,
  pageNum: 1,
  size: 10,
  orderByColumn: undefined,
  isAsc: undefined,
})

watch(dateRange, (val) => {
  query.startDate = val?.[0] || ''
  query.endDate = val?.[1] || ''
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getCollectionList(query)
    tableData.value = res.rows
    total.value = res.total
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.memberName = ''
  query.startDate = ''
  query.endDate = ''
  query.reportStatus = undefined as any
  query.status = undefined as any
  query.pageNum = 1
  query.orderByColumn = undefined
  query.isAsc = undefined
  dateRange.value = []
  fetchData()
}

function handleSortChange({ prop, order }: { prop: string; order: string | null }) {
  if (prop && order) {
    query.orderByColumn = prop
    query.isAsc = order === 'ascending' ? 'asc' : 'desc'
  } else {
    query.orderByColumn = undefined
    query.isAsc = undefined
  }
  fetchData()
}

async function handleDelete(id: number) {
  try {
    await deleteCollection(id)
    ElMessage.success($t('common.success'))
    fetchData()
  } catch { /* handled */ }
}

// ========== 新建采集对话框 ==========
const showCreateDialog = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const memberOptions = ref<PersonalInfo[]>([])
const createForm = reactive({
  memberId: undefined as string | number | undefined,
  checkupDate: '',
  hospital: '',
})
const createRules = {
  memberId: [{ required: true, message: '请选择成员', trigger: 'change' }],
}

const hospitalHistory = ref<string[]>(JSON.parse(localStorage.getItem('hospital_history') || '[]'))

function queryHospital(queryString: string, cb: (results: { value: string }[]) => void) {
  const results = queryString
      ? hospitalHistory.value.filter(h => h.toLowerCase().includes(queryString.toLowerCase())).map(v => ({ value: v }))
      : hospitalHistory.value.map(v => ({ value: v }))
  cb(results)
}

function saveHospitalHistory(name: string) {
  if (!name || !name.trim()) return
  const trimmed = name.trim()
  const list = hospitalHistory.value.filter(h => h !== trimmed)
  list.unshift(trimmed)
  if (list.length > 20) list.length = 20
  hospitalHistory.value = list
  localStorage.setItem('hospital_history', JSON.stringify(list))
}

async function loadAllMembers() {
  try {
    const res = await getPersonalInfoList({ pageNum: 1, pageSize: 200 })
    memberOptions.value = res.rows
  } catch { /* handled */ }
}

function openCreateDialog() {
  createForm.memberId = undefined
  createForm.checkupDate = ''
  createForm.hospital = ''
  showCreateDialog.value = true
  if (memberOptions.value.length === 0) loadAllMembers()
}

async function handleDownloadTemplate() {
  try {
    // 1. 先弹出友好提示
    await ElMessageBox.alert(
        `
      <div style="text-align: left; line-height: 1.8;">
        <p><strong>📋 模板使用说明：</strong></p>
        <p>1. 请严格按照模板表头填写，不要修改列名、增减列数</p>
        <p>2. <span style="color:red">红色列为必填项，必须填写</span></p>
        <p>3. 性别请使用下拉框选择：男 / 女</p>
        <p>4. 体检日期格式请保持：YYYY-MM-DD（如 2025-03-19）</p>
      </div>
      `,
        '下载体检信息模板',
        {
          confirmButtonText: '立即下载',
          cancelButtonText: '取消',
          showCancelButton: true,
          dangerouslyUseHTMLString: true,
          customClass: 'template-tip-dialog'
        }
    )
    download("/healthReport/collectionRecord/templateDownload", {}, `体检信息模板_${new Date().getTime()}.xlsx`)

    ElMessage.success('模板下载成功')
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error('模板下载失败')
    console.error(error)
  }
}

const batchImportLoading = ref(false)
const batchImportDialogVisible = ref(false)
const batchImportFileList = ref<any[]>([])
const batchImportOverride = ref(false)
const batchImportUploadRef = ref<any>(null)

function handleBatchImport() {
  batchImportFileList.value = []
  batchImportOverride.value = false
  batchImportDialogVisible.value = true
}

async function confirmBatchImport() {
  // 从 upload 组件获取文件列表
  const uploadFiles = batchImportUploadRef.value?.uploadFiles || batchImportFileList.value
  const files: File[] = uploadFiles.map((f: any) => f.raw).filter(Boolean)

  if (files.length === 0) {
    ElMessage.warning($t('collection.selectFileFirst'))
    return
  }

  batchImportLoading.value = true
  try {
    const languageType = locale.value === 'en-US' ? 'en' : 'zh'
    const res = await batchImportCollection(files, batchImportOverride.value, languageType)
    const result: BatchImportResponse = res.data

    batchImportDialogVisible.value = false

    // 🔥 统一处理成功/失败提示，支持HTML换行
    if (result.failCount === 0) {
      // 成功提示：和你系统其他导入风格完全一致
      const successMsg = `<br/>${$t('collection.importAllSuccess', { count: result.successCount })}<br/>`
      ElMessage.success({
        message: successMsg,
        dangerouslyUseHTMLString: true,
        duration: 3000
      })
    } else {
      // 失败提示：完美解决<br/>显示、null行问题
      const failMsg = new StringBuilder()
      failMsg.append(`<br/>${$t('collection.importPartialFailed', { count: result.failCount })}`)

      result.failReasons.forEach((detail, index) => {
        // 🔥 兼容null行（空文件/无数据场景）
        failMsg.append(`<br/><br/>${index + 1}、${detail.reason}`)
      })
      failMsg.append('<br/>')

      ElMessage.error({
        message: failMsg.toString(),
        dangerouslyUseHTMLString: true,
        duration: 5000
      })
    }

    // 清空文件列表
    batchImportFileList.value = []
    if (batchImportUploadRef.value) {
      batchImportUploadRef.value.clearFiles()
    }

    // 刷新列表
    fetchData()
  } catch (error: any) {
    // 🔥 捕获后端抛出的异常，统一处理错误提示
    let errMsg = error?.message || $t('collection.importFailed')
    // 兼容后端返回的带<br/>的错误信息
    ElMessage.error({
      message: errMsg,
      dangerouslyUseHTMLString: true,
      duration: 5000
    })
  } finally {
    batchImportLoading.value = false
  }
}

// 🔥 新增StringBuilder工具类（解决字符串拼接问题，和后端风格统一）
class StringBuilder {
  private buffer: string[] = []
  append(str: string) {
    this.buffer.push(str)
    return this
  }
  toString() {
    return this.buffer.join('')
  }
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    if (createForm.hospital) saveHospitalHistory(createForm.hospital)

    const res = await createCollection({
      memberId: createForm.memberId as any,
      checkupDate: createForm.checkupDate || undefined,
      hospital: createForm.hospital || undefined,
    })
    showCreateDialog.value = false
    ElMessage.success($t('common.success'))
    // 切换到录入视图
    await startEntry(res.data.id, false)
  } catch { /* handled */ } finally {
    creating.value = false
  }
}

// ========== 录入相关 ==========
const showEntry = ref(false)
const isEditEntry = ref(false)
const entryLoading = ref(false)
const saving = ref(false)
const activeIndex = ref(0)
const categories = ref<CategoryConfig[]>([])
const record = ref<CollectionRecord | null>(null)
const collectionId = ref<number>(0)
const formData = reactive<Record<string, any>>({})
const fieldErrors = reactive<Record<string, string>>({})

// ========== OCR相关 ==========
const ocrLoading = ref(false)
const ocrImageUploadRef = ref<any>(null)
const ocrPdfUploadRef = ref<any>(null)
const ocrImageFiles = ref<File[]>([])
const ocrPdfFiles = ref<File[]>([])

// 处理图片上传变化
function handleOcrImageChange(uploadFile: any) {
  if (!uploadFile.raw) return
  ocrImageFiles.value.push(uploadFile.raw)
  // 自动触发识别
  triggerOcrRecognition()
}

// 处理PDF上传变化
function handleOcrPdfChange(uploadFile: any) {
  if (!uploadFile.raw) return
  ocrPdfFiles.value.push(uploadFile.raw)
  // 自动触发识别
  triggerOcrRecognition()
}

// 触发OCR识别
async function triggerOcrRecognition() {
  const allFiles = [...ocrImageFiles.value, ...ocrPdfFiles.value]
  if (allFiles.length === 0) return

  ocrLoading.value = true
  try {
    const res = await extractIndicators(allFiles, collectionId.value)
    const result = res.data

    // 填充识别到的数据
    if (result.extractedData) {
      let filledCount = 0
      Object.keys(result.extractedData).forEach(fieldCode => {
        if (formData.hasOwnProperty(fieldCode)) {
          formData[fieldCode] = result.extractedData[fieldCode]
          filledCount++
        }
      })

      if (filledCount > 0) {
        ElMessage.success($t('collection.ocrExtractSuccess', { count: filledCount }))
        // 触发自动保存
        onFieldInput()
      } else {
        ElMessage.warning($t('collection.ocrNoMatch'))
      }
    }

    // 显示未匹配的文本
    if (result.unmatchedTexts && result.unmatchedTexts.length > 0) {
      console.log('未匹配的文本:', result.unmatchedTexts)
    }

    // 清空文件列表
    ocrImageFiles.value = []
    ocrPdfFiles.value = []
    if (ocrImageUploadRef.value) {
      ocrImageUploadRef.value.clearFiles()
    }
    if (ocrPdfUploadRef.value) {
      ocrPdfUploadRef.value.clearFiles()
    }
  } catch (error: any) {
    ElMessage.error(error?.message || $t('collection.ocrFailed'))
  } finally {
    ocrLoading.value = false
  }
}

// ========== 模型选择相关 ==========
const modelOptions = ref<AiModel[]>([])
const selectedModels = ref<AiModel[]>([])
const categoryLabels = ref<Record<string, string>>({})
const loadingCategories = ref(false)

async function loadModels() {
  try {
    // 先加载分类字典
    await loadCategories()

    const gender = record.value?.memberGender
    const res = await getAvailableModels(gender)
    modelOptions.value = res.data
  } catch { /* handled */ }
}

function getCategoryLabel(category: string) {
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
  const index = selectedModels.value.findIndex(m => m.id === model.id)
  if (index > -1) {
    selectedModels.value.splice(index, 1)
  } else {
    selectedModels.value.push(model)
  }
}

// 移除模型选择功能，现在通过点击卡片来切换选择状态
// function removeModel(model: AiModel) {
//   const index = selectedModels.value.findIndex(m => m.id === model.id)
//   if (index > -1) {
//     selectedModels.value.splice(index, 1)
//   }
// }

const currentCategory = computed(() => categories.value[activeIndex.value] || null)

const overallPct = computed(() => {
  let total = 0
  let filled = 0
  for (const cat of categories.value) {
    for (const f of cat.fields) {
      total++
      const val = formData[f.fieldCode]
      if (val != null && val !== '' && val !== undefined) filled++
    }
  }
  return total > 0 ? Math.min(100, Math.round((filled / total) * 100)) : 0
})

const canSubmit = computed(() => {
  if (!collectionId.value) return false
  for (const cat of categories.value) {
    for (const f of cat.fields) {
      if (f.isRequired === 1) {
        const val = formData[f.fieldCode]
        if (val == null || val === '' || val === undefined) return false
      }
    }
  }
  return true
})

function getCategoryFilled(code: string): number {
  const cat = categories.value.find((c) => c.categoryCode === code)
  if (!cat) return 0
  return cat.fields.filter((f) => {
    const val = formData[f.fieldCode]
    return val != null && val !== '' && val !== undefined
  }).length
}

function parseEnumOptions(jsonStr: string | null): { label: string; value: string }[] {
  if (!jsonStr) return []
  try { return JSON.parse(jsonStr) } catch { return [] }
}

// 根据当前语言获取分类名称
function getCategoryName(category: any): string {
  if (!category) return ''
  return locale.value === 'en-US' ? (category.categoryNameEn || category.categoryName) : category.categoryName
}

// 根据当前语言获取字段名称
function getFieldName(field: any): string {
  if (!field) return ''
  return locale.value === 'en-US' ? (field.fieldNameEn || field.fieldName) : field.fieldName
}

// 根据当前语言获取单位
function getUnit(field: any): string {
  if (!field) return ''
  return locale.value === 'en-US' ? (field.unitEn || field.unit) : field.unit
}

// 根据当前语言获取别名
function getAliasName(field: any): string {
  if (!field) return ''
  return locale.value === 'en-US' ? (field.aliasNameEn || field.aliasName) : field.aliasName
}

// 根据当前语言获取备注
function getRemark(field: any): string {
  if (!field) return ''
  return locale.value === 'en-US' ? (field.remarkEn || field.remark) : field.remark
}

// 根据当前语言获取参考值范围
function getRefRangeText(field: any): string {
  if (!field) return ''
  return locale.value === 'en-US' ? (field.refRangeTextEn || field.refRangeText) : field.refRangeText
}

function validateField(field: any) {
  const val = formData[field.fieldCode]
  const fieldName = getFieldName(field)
  if (field.isRequired === 1 && (val == null || val === '')) {
    fieldErrors[field.fieldCode] = locale.value === 'en-US' ? `${fieldName} is required` : `${fieldName} 为必填项`
    return false
  }
  if (field.fieldType === 'NUMBER' && val != null && val !== '' && isNaN(Number(val))) {
    fieldErrors[field.fieldCode] = locale.value === 'en-US' ? `${fieldName} must be a number` : `${fieldName} 需为数字`
    return false
  }
  delete fieldErrors[field.fieldCode]
  return true
}

let autoSaveTimer: ReturnType<typeof setTimeout> | null = null

function onFieldInput() {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(() => { saveCurrentCategory(true) }, 3000)
}

async function saveCurrentCategory(silent = false) {
  if (!collectionId.value || !currentCategory.value) return
  const cat = currentCategory.value
  const data: Record<string, any> = {}
  for (const f of cat.fields) {
    const val = formData[f.fieldCode]
    if (val != null && val !== '' && val !== undefined) {
      data[f.fieldCode] = val
    }
  }

  saving.value = true
  try {
    await saveCategoryDataItem(collectionId.value, { categoryCode: cat.categoryCode, fieldData: data })
    if (!silent) ElMessage.success({ message: $t('collection.autoSaved'), duration: 1500 })
  } catch {
    if (!silent) ElMessage.error($t('common.failed'))
  } finally {
    saving.value = false
  }
}

async function switchCategory(idx: number) {
  if (idx === activeIndex.value) return
  await saveCurrentCategory(true)
  activeIndex.value = idx
}

async function prevCategory() {
  if (activeIndex.value > 0) {
    await saveCurrentCategory(true)
    activeIndex.value--
  }
}

async function nextCategory() {
  if (activeIndex.value < categories.value.length - 1) {
    await saveCurrentCategory(true)
    activeIndex.value++
  }
}

async function handleComplete() {
  if (selectedModels.value.length === 0) {
    ElMessage.warning('请至少选择一个模型')
    return
  }

  try {
    await saveCurrentCategory(true)

    // 跳转到报告生成页面并携带选中的模型
    const selectedModelIds = selectedModels.value.map(m => m.id).join(',')
    router.push({
      path: '/collection/generate',
      query: {
        collectionId: collectionId.value,
        modelIds: selectedModelIds
      }
    })
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function startEntry(id: number, isEdit: boolean) {
  showEntry.value = true
  isEditEntry.value = isEdit
  entryLoading.value = true

  try {
    collectionId.value = id
    const detailRes = await getCollectionDetail(id)
    const detail = detailRes.data
    record.value = detail.record as any

    const configRes = await getCollectionConfig(detail.record.memberGender,'衍生变量')
    categories.value = configRes.data

    // 清空之前的数据
    Object.keys(formData).forEach(key => delete formData[key])
    Object.keys(fieldErrors).forEach(key => delete fieldErrors[key])
    activeIndex.value = 0

    if (detail.categoryDataList) {
      for (const cd of detail.categoryDataList) {
        if (cd.fieldData) {
          for (const [key, val] of Object.entries(cd.fieldData)) {
            formData[key] = val
          }
        }
      }
    }

    // 加载模型列表
    await loadModels()
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
    handleBackToList()
  } finally {
    entryLoading.value = false
  }
}

async function handleEdit(row: CollectionRecord) {
  await startEntry(row.id, true)
}

function handleBackToList() {
  showEntry.value = false
  isEditEntry.value = false
  record.value = null
  collectionId.value = 0
  categories.value = []
  Object.keys(formData).forEach(key => delete formData[key])
  Object.keys(fieldErrors).forEach(key => delete fieldErrors[key])
  activeIndex.value = 0
}

// ========== 生成报告相关 ==========
async function handleGenerateReport(row: CollectionRecord) {
  // 跳转到生成报告页面（体检录入菜单下）
  router.push({
    path: '/collection/generate',
    query: { collectionId: row.id }
  })
}

// 在onMounted中调用
onMounted(() => {
  fetchData()
})
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

  .search-select {
    width: 150px;
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

.create-dialog {
  :deep(.el-dialog__body) {
    padding: 28px 32px 8px;
  }
  :deep(.el-form-item) {
    margin-bottom: 24px;
  }
  :deep(.el-form-item__label) {
    font-weight: 500;
    font-size: 14px;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

// 录入页面样式
.entry-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);

  .page-header {
    display: flex;
    align-items: center;
    margin-bottom: 20px;
    flex-shrink: 0;
    gap: 16px;
    flex-wrap: wrap;

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;
      flex: 1;
      min-width: 0;

      h3 {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        margin: 0;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .model-select-section {
        min-width: 300px;
        flex: 1;

        .model-section-title {
          font-size: 12px;
          font-weight: 500;
          color: #606266;
          margin-bottom: 8px;
          padding-left: 6px;
          border-left: 2px solid #409eff;
        }

        .model-grid {
          display: flex;
          gap: 8px;
          max-width: 100%;
          overflow-x: auto;
          padding: 2px 0;
        }

        .model-card {
          padding: 8px;
          border: 1px solid #e4e7ed;
          border-radius: 4px;
          cursor: pointer;
          transition: all 0.3s;
          min-width: 140px;
          max-width: 160px;

          &:hover {
            border-color: #c0c4cc;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
          }

          &.is-selected {
            border-color: #409eff;
            background-color: #ecf5ff;
            box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
          }

          h4 {
            font-size: 11px;
            font-weight: 500;
            color: #303133;
            margin: 0 0 4px 0;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          .model-meta {
            display: flex;
            align-items: center;
            gap: 4px;
            flex-wrap: wrap;

            .param-count {
              font-size: 10px;
              color: #909399;
            }
          }
        }

        .no-models {
          color: #94a3b8;
          font-size: 12px;
          text-align: center;
          padding: 12px;
          min-width: 140px;
        }
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-shrink: 0;

      .ocr-upload-section {
        display: flex;
        align-items: center;
        gap: 8px;

        .ocr-upload-btn {
          display: inline-block;

          :deep(.el-upload) {
            display: inline-block;
          }
        }
      }
    }
  }
}

.completion-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pct-label {
  font-size: 13px;
  color: #0d9488;
  font-weight: 600;
}

.entry-body {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.category-nav {
  width: 240px;
  flex-shrink: 0;
  background: #ffffff;
  border-radius: 10px;
  padding: 12px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover { background: #f1f5f9; }
  &.active {
    background: #f0fdfa;
    color: #0d9488;
    .nav-icon { color: #0d9488; }
  }
  &.has-data .nav-count { color: #67c23a; }
}

.nav-icon {
  font-size: 18px;
  color: #94a3b8;
  flex-shrink: 0;
}

.nav-text {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.nav-name {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-count {
  font-size: 11px;
  color: #94a3b8;
}

.nav-check {
  flex-shrink: 0;
  font-size: 16px;
}

.form-area {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.form-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
  padding: 24px;
  flex: 1;
  overflow-y: auto;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.form-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

/* 两列布局核心样式 */
.two-column-form {
  .form-row {
    display: flex;
    flex-wrap: wrap;
    width: 100%;
  }
}

.field-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
    align-items: center;
  }
  :deep(.el-form-item__label) {
    font-size: 13px;
    font-weight: 500;
    color: #606266;
  }
  :deep(.el-form-item__content) {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }
}

.field-unit {
  margin-left: 8px;
  color: #64748b;
  font-size: 13px;
  flex-shrink: 0;
}

.field-help {
  margin-left: 6px;
  color: #94a3b8;
  cursor: pointer;
  &:hover { color: #0d9488; }
}

.field-error {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 2px;
  width: 100%;
}

.has-error {
  :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 1px #f56c6c inset;
  }
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 16px 0;
  flex-shrink: 0;
}
</style>