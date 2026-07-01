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
        <el-select v-model="query.gender" clearable :placeholder="$t('personalInfo.gender')" class="search-select">
          <el-option :label="$t('personalInfo.male')" :value="1" />
          <el-option :label="$t('personalInfo.female')" :value="2" />
        </el-select>
        <el-select v-model="query.reportStatus" clearable :placeholder="$t('collection.reportGenerateStatus')" class="search-select">
          <el-option :label="$t('collection.reportGenerated')" :value="2" />
          <el-option :label="$t('collection.reportNotGenerated')" :value="0" />
        </el-select>
        <el-select v-model="query.finishStatus" clearable :placeholder="$t('collection.status')" class="search-select">
          <el-option :label="$t('collection.statusDraft')" :value="0" />
          <el-option :label="$t('collection.statusComplete')" :value="1" />
        </el-select>
        <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>{{ $t('common.search') }}</el-button>
        <el-button @click="resetQuery"><el-icon><RefreshRight /></el-icon>{{ $t('common.reset') }}</el-button>
      </div>

      <div class="table-wrapper">
        <el-table :data="tableData" v-loading="loading" stripe @sort-change="handleSortChange" style="width: 100%;">
          <el-table-column prop="collectionNo" :label="$t('collection.collectionNo')" min-width="160" />
          <el-table-column prop="memberName" :label="$t('collection.memberName')" min-width="80" />
          <el-table-column :label="$t('personalInfo.gender')" min-width="60" align="center">
            <template #default="{ row }">
              {{ row.memberGender === 1 ? $t('personalInfo.male') : row.memberGender === 2 ? $t('personalInfo.female') : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="checkupDate" :label="$t('collection.checkupDate')" min-width="100" sortable />
          <el-table-column :label="$t('collection.source')" min-width="100">
            <template #default="{ row }">
              <el-tag :type="row.sourceType === 1 ? '' : 'success'" size="small">
                {{ row.sourceType === 1 ? $t('collection.sourceManual') : $t('collection.sourceExcel') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('collection.completeness')" min-width="140" sortable prop="completeness">
            <template #default="{ row }">
              <el-progress
                  :percentage="Number(row.completeness) || 0"
                  :color="Number(row.completeness) >= 70 ? '#67c23a' : '#e6a23c'"
                  :stroke-width="8"
              />
            </template>
          </el-table-column>
          <el-table-column :label="$t('collection.status')" min-width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.finishStatus === 1 ? 'success' : 'warning'" size="small">
                {{ row.finishStatus === 1 ? $t('collection.statusComplete') : $t('collection.statusDraft') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('collection.reportGenerateStatus')" min-width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.reportStatus === 2 ? 'success' : 'info'" size="small">
                {{ row.reportStatus === 2 ? $t('collection.reportGenerated') : $t('collection.reportNotGenerated') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="$t('personalInfo.uploadTime')" min-width="160" align="center" sortable />
          <el-table-column :label="$t('common.operation')" min-width="200" fixed="right" align="center">
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
          <el-form-item :label="$t('collection.checkupDate')" prop="checkupDate" :rules="createRules">
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
            <!--            <div class="model-select-section">
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
                                  {{ model.applicableGender === 0 ? $t('model.genderAll') : model.applicableGender === 1 ? $t('personalInfo.male') : $t('personalInfo.female') }}
                                </el-tag>
                                <span class="param-count">{{ $t('collection.keyParamsWithCount', { count: model.paramCount || 0 }) }}</span>
                              </div>
                            </div>
                            <div v-if="modelOptions.length === 0" class="no-models">
                              {{ $t('model.noModelsAvailable') }}
                            </div>
                          </div>
                        </div>-->
          </div>
          <div class="header-right">
            <!-- OCR上传按钮 -->
            <div class="ocr-upload-section">
              <el-button type="warning" @click="openOcrDialog('image')">
                <el-icon><Picture /></el-icon>
                {{ $t('collection.uploadImage') }}
              </el-button>
              <el-button type="info" @click="openOcrDialog('pdf')">
                <el-icon><Document /></el-icon>
                {{ $t('collection.uploadPdf') }}
              </el-button>
            </div>
            <div class="completion-info">
              <el-progress type="circle" :percentage="overallPct" :width="52" :stroke-width="5" />
              <span class="pct-label">{{ $t('collection.overallCompletion', { percent: overallPct }) }}</span>
            </div>
            <el-button type="success" @click="handleComplete">
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
                :class="{
                  active: activeIndex === idx,
                  'has-data': getCategoryFilled(cat.categoryCode) > 0,
                  'ocr-new': ocrFilledCategories.includes(cat.categoryCode)
                }"
                @click="switchCategory(idx)"
            >
              <div class="nav-icon">
                <el-icon><component :is="cat.icon || 'Document'" /></el-icon>
              </div>
              <div class="nav-text">
                <span class="nav-name">{{ getCategoryName(cat) }}</span>
                <span class="nav-count">{{ getCategoryFilled(cat.categoryCode) }}/{{ cat.fields.length }}</span>
              </div>
              <!-- OCR新填充标记 -->
              <span v-if="ocrFilledCategories.includes(cat.categoryCode)" class="ocr-new-badge">{{ $t('collection.ocrNewBadge') }}</span>
              <el-icon v-else-if="getCategoryFilled(cat.categoryCode) === cat.fields.length && cat.fields.length > 0" class="nav-check" color="#67c23a"><CircleCheckFilled /></el-icon>
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
                <el-button type="primary" size="default" @click="saveCurrentCategory(false)" :loading="saving">
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
                        :class="{
                          'has-error': fieldErrors[field.fieldCode],
                          'ocr-filled': ocrFilledFields.includes(field.fieldCode)
                        }"
                        :style="{ width: '50%', boxSizing: 'border-box', paddingRight: index % 2 === 0 ? '20px' : '0' }"
                    >
                      <!-- OCR填充标记 -->
                      <template v-if="ocrFilledFields.includes(field.fieldCode)" #label>
                        <span class="ocr-field-label">{{ getFieldName(field) }}</span>
                        <el-tag size="small" type="warning" class="ocr-tag">OCR</el-tag>
                      </template>
                      <template v-if="field.fieldType === 'NUMBER'">
                        <el-input
                            v-model="formData[field.fieldCode]"
                            type="number"
                            :placeholder="getDynamicPlaceholder(field)"
                            style="width: 200px"
                            @blur="validateField(field)"
                            @input="onFieldInput()"
                        />
                        <span class="field-unit" v-if="getUnit(field)">{{ getUnit(field) }}</span>

                        <el-tooltip
                            v-if="showHelpIcon(field)"
                            placement="top"
                            :raw-content="true"
                            :content="getTooltipContent(field)"
                        >
                          <el-icon class="field-help"><QuestionFilled /></el-icon>
                        </el-tooltip>
                      </template>

                      <template v-else-if="field.fieldType === 'TEXT'">
                        <el-input
                            v-model="formData[field.fieldCode]"
                            :placeholder="getDynamicPlaceholder(field)"
                            style="width: 200px"
                            @input="onFieldInput()"
                        />
                        <span class="field-unit" v-if="getUnit(field)">{{ getUnit(field) }}</span>

                        <el-tooltip
                            v-if="showHelpIcon(field)"
                            placement="top"
                            :raw-content="true"
                            :content="getTooltipContent(field)"
                        >
                          <el-icon class="field-help"><QuestionFilled /></el-icon>
                        </el-tooltip>
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
                        <span class="field-unit" v-if="getUnit(field)">{{ getUnit(field) }}</span>
                        <el-tooltip v-if="getAliasName(field) || getRemark(field)" placement="top">
                          <template #content>
                            <div v-if="getAliasName(field)">{{ $t('collection.aliasLabel') }} {{ getAliasName(field) }}</div>
                            <div v-if="getRemark(field)">{{ $t('collection.remarkLabel') }} {{ getRemark(field) }}</div>
                          </template>
                          <el-icon class="field-help"><QuestionFilled /></el-icon>
                        </el-tooltip>
                      </template>

                      <template v-else-if="field.fieldType === 'BOOLEAN'">
                        <el-radio-group v-model="formData[field.fieldCode]" @change="onFieldInput()">
                          <el-radio :value="true">{{ $t('collection.yes') }}</el-radio>
                          <el-radio :value="false">{{ $t('collection.no') }}</el-radio>
                        </el-radio-group>
                        <span class="field-unit" v-if="getUnit(field)">{{ getUnit(field) }}</span>
                        <el-tooltip v-if="getAliasName(field) || getRemark(field)" placement="top">
                          <template #content>
                            <div v-if="getAliasName(field)">{{ $t('collection.aliasLabel') }} {{ getAliasName(field) }}</div>
                            <div v-if="getRemark(field)">{{ $t('collection.remarkLabel') }} {{ getRemark(field) }}</div>
                          </template>
                          <el-icon class="field-help"><QuestionFilled /></el-icon>
                        </el-tooltip>
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

    <!-- OCR上传弹窗 -->
    <el-dialog
        v-model="ocrDialogVisible"
        :title="ocrDialogTitle"
        width="800px"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
    >
      <div class="ocr-dialog-content">
        <el-upload
            ref="ocrDialogUploadRef"
            action="#"
            :auto-upload="false"
            :show-file-list="true"
            :file-list="ocrDialogFiles"
            :on-change="handleOcrDialogFileChange"
            :on-remove="handleOcrDialogFileRemove"
            :accept="ocrDialogAccept"
            multiple
            list-type="picture-card"
            class="ocr-dialog-upload"
        >
          <el-icon><Plus /></el-icon>
          <template #file="{ file }">
            <div class="ocr-file-item">
              <img v-if="isImageFile(file)" :src="file.url" class="ocr-preview-image" />
              <div v-else class="ocr-file-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="ocr-file-name">{{ file.name }}</div>
            </div>
          </template>
        </el-upload>

        <div v-if="ocrDialogFiles.length > 0" class="ocr-dialog-preview">
          <h4>{{ $t('collection.selectedFiles') }}</h4>
          <ul>
            <li v-for="(file, index) in ocrDialogFiles" :key="index">
              {{ index + 1 }}. {{ file.name }} ({{ formatFileSize(file.size) }})
            </li>
          </ul>
        </div>
      </div>

      <template #footer>
        <el-button @click="closeOcrDialog">{{ $t('common.cancel') }}</el-button>
        <el-button
            type="primary"
            :loading="ocrDialogLoading"
            :disabled="ocrDialogFiles.length === 0"
            @click="confirmOcrUpload"
        >
          <el-icon><Upload /></el-icon>
          {{ $t('collection.saveAndRecognize') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="Collection">
import { ref, reactive, computed, onMounted, watch, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus, Search, RefreshRight, ArrowLeft, ArrowRight, CircleCheckFilled, QuestionFilled, Download, Upload, Picture, Document } from '@element-plus/icons-vue'
import { extractIndicators } from '@/api/healthReport/ocr'
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

const { t: $t, locale } = useI18n()
const router = useRouter()

// ========== 列表相关 ==========
const loading = ref(false)
const tableData = ref<CollectionRecord[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])
const query = reactive<CollectionQuery & { status?: number; orderByColumn?: string; isAsc?: string; gender?: number }>({
  memberName: '',
  startDate: '',
  endDate: '',
  gender: undefined as any,
  reportStatus: undefined as any,
  finishStatus: undefined as any,
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
  query.gender = undefined as any
  query.reportStatus = undefined as any
  query.finishStatus = undefined as any
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
  memberId: [{ required: true, message: $t('collection.selectUserRequired'), trigger: 'change' }],
  checkupDate: [{ required: true, message: $t('collection.checkupDateRequired'), trigger: 'change' }],
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
    // 根据当前语言判断下载中文或英文模板
    const languageType = isEnglish() ? 'en' : 'zh'
    const fileName = isEnglish()
        ? `${$t('collection.healthTemplate')}_${new Date().getTime()}.xlsx`
        : `${$t('collection.healthTemplate')}_${new Date().getTime()}.xlsx`

    await download("/healthReport/collectionRecord/templateDownload", { languageType }, fileName)

    ElMessage.success($t('collection.templateDownloadSuccess'))
  } catch (error) {
    ElMessage.error($t('collection.templateDownloadFailed'))
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
    const languageType = isEnglish() ? 'en' : 'zh'
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
let isSaving = false // 防止重复提交的标志
const activeIndex = ref(0)
const categories = ref<CategoryConfig[]>([])
const record = ref<CollectionRecord | null>(null)
const collectionId = ref<number>(0)
const formData = reactive<Record<string, any>>({})
const fieldErrors = reactive<Record<string, string>>({})

// ========== OCR相关 ==========
let isOcrProcessing = false // 请求锁，防止重复请求

// OCR弹窗相关
const ocrDialogVisible = ref(false)
const ocrDialogType = ref<'image' | 'pdf'>('image')
const ocrDialogFiles = ref<any[]>([])
const ocrDialogLoading = ref(false)
const ocrDialogUploadRef = ref<any>(null)

const ocrDialogTitle = computed(() => {
  return ocrDialogType.value === 'image' ? $t('collection.uploadImageOcr') : $t('collection.uploadPdfOcr')
})

const ocrDialogAccept = computed(() => {
  return ocrDialogType.value === 'image' ? 'image/*' : '.pdf'
})

// OCR视觉反馈相关
const ocrFilledFields = ref<string[]>([]) // 记录OCR填充的字段
const ocrFilledCategories = ref<string[]>([]) // 记录OCR填充的分类

// 打开OCR弹窗
function openOcrDialog(type: 'image' | 'pdf') {
  ocrDialogType.value = type
  ocrDialogFiles.value = []
  ocrDialogVisible.value = true
}

// 关闭OCR弹窗
function closeOcrDialog() {
  ocrDialogVisible.value = false
  ocrDialogFiles.value = []
}

// 判断是否为图片文件
function isImageFile(file: any): boolean {
  return file.name && file.name.match(/\.(jpg|jpeg|png|gif|bmp)$/i)
}

// 格式化文件大小
function formatFileSize(size: number): string {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

// 处理弹窗内文件选择变化
function handleOcrDialogFileChange(uploadFile: any, fileList: any[]) {
  ocrDialogFiles.value = fileList
}

// 处理弹窗内文件移除
function handleOcrDialogFileRemove(uploadFile: any, fileList: any[]) {
  ocrDialogFiles.value = fileList
}

// 确认OCR上传并识别
async function confirmOcrUpload() {
  // 检查是否正在处理中
  if (isOcrProcessing) {
    ElMessage.warning($t('collection.processingPleaseWait'))
    return
  }

  const files = ocrDialogFiles.value.map((item: any) => item.raw)
  if (files.length === 0) return
  // ======================
  // 🔥 计算所有文件总大小
  // ======================
  const maxSize = 200 * 1024 * 1024; // 200MB
  const totalSize = files.reduce((sum, file) => sum + file.size, 0);

  if (totalSize > maxSize) {
    ElMessage.error('所有文件总大小不能超过 200MB！');
    return;
  }
  // 设置请求锁
  isOcrProcessing = true
  ocrDialogLoading.value = true

  try {
    const res = await extractIndicators(files, collectionId.value)
    const result = res.data

    // 填充识别到的数据
    if (result.extractedData) {
      let filledCount = 0
      const filledFieldNames: string[] = []
      ocrFilledFields.value = [] // 清空之前的记录
      ocrFilledCategories.value = [] // 清空之前的分类记录

      Object.keys(result.extractedData).forEach(fieldCode => {
        if (formData.hasOwnProperty(fieldCode)) {
          formData[fieldCode] = result.extractedData[fieldCode]
          ocrFilledFields.value.push(fieldCode)
          filledCount++

          // 获取字段名称
          const field = findFieldByCode(fieldCode)
          if (field) {
            filledFieldNames.push(getFieldName(field))
            // 记录分类
            const category = findCategoryByFieldCode(fieldCode)
            if (category && !ocrFilledCategories.value.includes(category)) {
              ocrFilledCategories.value.push(category)
            }
          }
        }
      })

      if (filledCount > 0) {
        // 显示成功提示
        const summaryText = $t('collection.ocrSummary', { count: filledCount, fields: filledFieldNames.slice(0, 5).join('、') })
        ElMessage.success(summaryText)

        // OCR标记会一直显示，直到：生成报告、切换分类保存、或刷新页面
        // （已移除setTimeout，标记不会自动消失）
      } else {
        ElMessage.warning($t('collection.ocrNoData'))
      }
    }

  } catch (error: any) {
    ElMessage.error($t('collection.ocrFailed') + ': ' + (error.message || 'Unknown error'))
  } finally {
    // 重置请求锁
    isOcrProcessing = false
    // 关闭弹窗
    ocrDialogVisible.value = false
    ocrDialogLoading.value = false
    ocrDialogFiles.value = []
  }
}

// 根据字段编码查找字段
function findFieldByCode(fieldCode: string): any {
  for (const cat of categories.value) {
    const field = cat.fields.find((f: any) => f.fieldCode === fieldCode)
    if (field) return field
  }
  return null
}

// 根据字段编码查找分类
function findCategoryByFieldCode(fieldCode: string): string {
  for (const cat of categories.value) {
    const field = cat.fields.find((f: any) => f.fieldCode === fieldCode)
    if (field) return cat.categoryCode
  }
  return ''
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

// 判断是否为英文环境
function isEnglish(): boolean {
  return locale.value === 'en-US' || locale.value === 'en'
}

// 根据当前语言获取分类名称
function getCategoryName(category: any): string {
  if (!category) return ''
  return isEnglish() ? (category.categoryNameEn || category.categoryName) : category.categoryName
}

// 输入框提示语最大长度，超过就放进问号
const PLACEHOLDER_MAX_LENGTH = 10

// 获取输入框占位符（短显示：请输入(范围)，长显示：请输入）
function getDynamicPlaceholder(field: any) {
  const range = getRefRangeText(field)
  // 没有参考范围 → 只显示请输入
  if (!range || range.trim() === '') {
    return $t('collection.pleaseEnter')
  }

  // 范围短 → 请输入（参考范围）
  if (range.length <= PLACEHOLDER_MAX_LENGTH) {
    return $t('collection.pleaseEnter') + '（' + range + '）'
  }

  // 范围长 → 只显示请输入
  return $t('collection.pleaseEnter')
}

// 获取问号里面显示的完整内容
// 获取问号里面显示的完整内容（自动换行 + 中英文）
function getTooltipContent(field: any) {
  const parts = []

  // 参考范围
  const range = getRefRangeText(field)
  if (range && range.length > PLACEHOLDER_MAX_LENGTH) {
    parts.push($t('collection.referenceRange') + range)
  }

  // 别名
  const alias = getAliasName(field)
  if (alias) {
    parts.push($t('collection.aliasLabel')  + alias)
  }

  // 备注
  const remark = getRemark(field)
  if (remark) {
    // 🔥 同时替换 中文分号； + 英文分号; 为换行
    const formattedRemark = remark.replace(/;|；|:|：/g, '<br/>')
    parts.push($t('collection.remarkLabel') + formattedRemark)
  }

  // 关键：用 \n 换行，Element Plus tooltip 自动识别
  return parts.join('<br/>')
}

// 判断是否需要显示问号图标
function showHelpIcon(field: any) {
  const range = getRefRangeText(field)
  // 参考范围太长 或 有别名/备注 → 显示问号
  return (range && range.length > PLACEHOLDER_MAX_LENGTH)
      || !!getAliasName(field)
      || !!getRemark(field)
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

// 根据当前语言获取别名
function getAliasName(field: any): string {
  if (!field) return ''
  return isEnglish() ? (field.aliasNameEn || field.aliasName) : field.aliasName
}

// 根据当前语言获取备注
function getRemark(field: any): string {
  if (!field) return ''
  return isEnglish() ? (field.remarkEn || field.remark) : field.remark
}

// 根据当前语言获取参考值范围
function getRefRangeText(field: any): string {
  if (!field) return ''
  return isEnglish() ? (field.refRangeTextEn || field.refRangeText) : field.refRangeText
}

function validateField(field: any) {
  const val = formData[field.fieldCode]
  const fieldName = getFieldName(field)
  if (field.isRequired === 1 && (val == null || val === '')) {
    fieldErrors[field.fieldCode] = $t('collection.fieldRequired', { fieldName })
    return false
  }
  if (field.fieldType === 'NUMBER' && val != null && val !== '' && isNaN(Number(val))) {
    fieldErrors[field.fieldCode] = $t('collection.fieldMustBeNumber', { fieldName })
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

  // 防止重复提交
  if (isSaving) {
    if (!silent) ElMessage.warning($t('collection.processingPleaseWait'))
    return
  }

  const cat = currentCategory.value
  const data: Record<string, any> = {}
  for (const f of cat.fields) {
    const val = formData[f.fieldCode]
    if (val != null && val !== '' && val !== undefined) {
      data[f.fieldCode] = val
    }
  }

  // 如果没有数据需要保存，直接返回
  if (Object.keys(data).length === 0) return

  isSaving = true
  saving.value = true
  try {
    await saveCategoryDataItem(collectionId.value, { categoryCode: cat.categoryCode, fieldData: data })
    if (!silent) ElMessage.success({ message: $t('collection.autoSaved'), duration: 1500 })

    // 保存成功后，清空当前分类的OCR填充标记
    const currentFields = cat.fields.map(f => f.fieldCode)
    ocrFilledFields.value = ocrFilledFields.value.filter(code => !currentFields.includes(code))
    ocrFilledCategories.value = ocrFilledCategories.value.filter(c => c !== cat.categoryCode)
  } catch {
    if (!silent) ElMessage.error($t('common.failed'))
  } finally {
    isSaving = false
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
  // 检查是否有OCR识别的指标未保存
  if (ocrFilledFields.value.length > 0) {
    ElMessage.warning($t('collection.ocrDataNotSaved'))
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
    ElMessage.error(e?.message || $t('collection.operationFailed'))
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

    const configRes = await getCollectionConfig(detail.record.memberGender, '衍生变量')
    categories.value = configRes.data

    // 清空之前的数据
    Object.keys(formData).forEach(key => delete formData[key])
    Object.keys(fieldErrors).forEach(key => delete fieldErrors[key])
    activeIndex.value = 0
    // 清空OCR标记
    ocrFilledFields.value = []
    ocrFilledCategories.value = []

    // 为所有字段初始化默认值（确保OCR识别后能正确赋值）
    for (const cat of categories.value) {
      for (const field of cat.fields) {
        formData[field.fieldCode] = ''
      }
    }

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
    ElMessage.error(e?.message || $t('collection.loadFailed'))
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
  // 跳转到生成报告页面（体检录入菜单下），携带collectionId和gender参数
  router.push({
    path: '/collection/generate',
    query: { collectionId: row.id, gender: row.memberGender }
  })
}

// 在onMounted中调用
onMounted(() => {
  fetchData()
})

// 组件被激活时（从keep-alive缓存中恢复），清空OCR标记
onActivated(() => {
  ocrFilledFields.value = []
  ocrFilledCategories.value = []
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

// OCR新填充标记
.ocr-new-badge {
  background: #f97316;
  color: white;
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 4px;
  margin-left: 4px;
  flex-shrink: 0;
}

.nav-item.ocr-new {
  background: #fff7ed;
  border: 1px solid #fdba74;

  &:hover {
    background: #ffedd5;
  }
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
    gap: 20px;          /* 添加列间距 */
  }
  :deep(.el-form-item) {
    min-width: 450px;   /* 最小宽度，确保每列有足够空间 */
    max-width: 50%;      /* 最大宽度为50%，确保两列 */
    flex: 1;            /* 自适应剩余空间 */
  }
}

.field-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
    align-items: flex-start;
  }
  :deep(.el-form-item__label) {
    font-size: 13px;
    font-weight: 500;
    color: #606266;
    min-width: 200px;   /* 最小宽度 - 减小以适应英文 */
    max-width: 420px;   /* 最大宽度 - 大幅增加以适应英文标签 */
    width: 45%;          /* 百分比宽度，增加标签占比 */
    white-space: normal;
    word-break: break-all;  /* 允许断词 */
    overflow-wrap: break-word;
    text-align: left;
    line-height: 1.5;
    padding-top: 8px;
    flex-shrink: 1;      /* 允许标签收缩 */
  }
  :deep(.el-form-item__content) {
    display: flex;
    align-items: center;
    flex-wrap: wrap;     /* 允许换行，适应长标签 */
    gap: 8px;            /* 添加间距 */
  }
  :deep(.el-input) {
    flex-shrink: 1;
    min-width: 150px;    /* 设置最小宽度 */
    width: 180px;        /* 设置固定宽度 */
  }
  :deep(.el-select) {
    flex-shrink: 1;
    min-width: 150px;
    width: 180px;
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

// OCR填充字段高亮
.ocr-filled {
  :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 2px #f97316 inset;
    background-color: #fff7ed;
  }

  :deep(.el-form-item__label) {
    color: #ea580c;
    font-weight: 600;
  }
}

.ocr-field-label {
  color: #ea580c;
  font-weight: 600;
}

.ocr-tag {
  margin-left: 4px;
  font-size: 10px;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 16px 0;
  flex-shrink: 0;
}

/* OCR弹窗样式 */
.ocr-dialog-content {
  padding: 16px;
}

.ocr-dialog-upload {
  margin-bottom: 16px;
}

.ocr-file-item {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ocr-preview-image {
  max-width: 100%;
  max-height: 80px;
  object-fit: contain;
}

.ocr-file-icon {
  font-size: 32px;
  color: #999;
}

.ocr-file-name {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 100%;
  text-align: center;
}

.ocr-dialog-preview {
  padding: 12px;
  background-color: #f9fafb;
  border-radius: 8px;
}

.ocr-dialog-preview h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 500;
}

.ocr-dialog-preview ul {
  margin: 0;
  padding-left: 20px;
}

.ocr-dialog-preview li {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}
</style>