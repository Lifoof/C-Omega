<template>
  <div class="page-container">
    <!-- 列表视图 -->
    <template v-if="!showForm">
      <div class="page-header">
        <h3>{{ $t('model.title') }}</h3>
        <div class="header-actions">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>{{ $t('common.add') }}
          </el-button>
        </div>
      </div>

      <div class="search-bar">
        <el-input
            v-model="query.modelName"
            :placeholder="$t('model.modelName')"
            clearable
            class="search-input"
            @keyup.enter="fetchData"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>

        <!-- 模型分类 下拉搜索 -->
        <el-select v-model="query.category" clearable :placeholder="$t('model.category')" class="search-select">
          <el-option
              v-for="dict in model_category"
              :key="dict.value"
              :label="$t(`model.categoryLabels.${dict.value}`) !== `model.categoryLabels.${dict.value}` ? $t(`model.categoryLabels.${dict.value}`) : dict.label"
              :value="dict.value"
          />
        </el-select>

        <el-select v-model="query.applicableGender" clearable :placeholder="$t('model.applicableGender')" class="search-select">
          <el-option :label="$t('model.genderAll')" :value="0" />
          <el-option :label="$t('model.genderMale')" :value="1" />
          <el-option :label="$t('model.genderFemale')" :value="2" />
        </el-select>

        <el-select v-model="query.status" clearable :placeholder="$t('model.status')" class="search-select">
          <el-option :label="$t('model.enabled')" :value="1" />
          <el-option :label="$t('model.disabled')" :value="0" />
        </el-select>

        <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>{{ $t('common.search') }}</el-button>
        <el-button @click="resetQuery"><el-icon><RefreshRight /></el-icon>{{ $t('common.reset') }}</el-button>
      </div>

      <div class="table-wrapper">
        <el-table :data="tableData" v-loading="loading" stripe style="width: 100%;">
          <el-table-column prop="modelName" :label="$t('model.modelName')" min-width="140" >
            <template #default="{ row }">
              {{ isEnglish() ? (row.modelNameEn || row.modelName) : row.modelName }}
            </template>
          </el-table-column>

          <!-- 表格显示字典标签 -->
          <el-table-column :label="$t('model.category')" min-width="140">
            <template #default="scope">
              {{ $t(`model.categoryLabels.${scope.row.category}`) !== `model.categoryLabels.${scope.row.category}` ? $t(`model.categoryLabels.${scope.row.category}`) : scope.row.category }}
            </template>
          </el-table-column>

          <el-table-column :label="$t('model.applicableGender')" min-width="100">
            <template #default="{ row }">
              {{ row.applicableGender === 0 ? $t('model.genderAll') : row.applicableGender === 1 ? $t('model.genderMale') : $t('model.genderFemale') }}
            </template>
          </el-table-column>
          <el-table-column prop="paramCount" :label="$t('model.paramCount')" min-width="80" align="center" />
          <el-table-column prop="callCount" :label="$t('model.callCount')" min-width="80" align="center" />
          <el-table-column prop="priority" :label="$t('model.priority')" min-width="80" align="center" />
          <el-table-column :label="$t('model.status')" min-width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? $t('model.enabled') : $t('model.disabled') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" :label="$t('model.description')" min-width="160" />
          <el-table-column :label="$t('common.action')" min-width="200" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEdit(row)">
                {{ $t('common.edit') }}
              </el-button>
              <el-button
                  v-if="row.status === 1"
                  link type="warning"
                  @click="handleToggleStatus(row, 0)"
              >{{ $t('common.disable') }}</el-button>
              <el-button
                  v-else
                  link type="success"
                  @click="handleToggleStatus(row, 1)"
              >{{ $t('common.enable') }}</el-button>
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
    </template>

    <!-- 表单视图 -->
    <template v-else>
      <div class="model-form-page" :class="{ 'fullscreen-mode': paramFullscreen }">
        <template v-if="!paramFullscreen">
          <div class="page-header">
            <h3>{{ isEdit ? $t('model.editTitle') : $t('model.createTitle') }}</h3>
            <div class="header-actions">
              <el-button @click="handleBack">{{ $t('common.back') }}</el-button>
            </div>
          </div>
          <div class="form-card basic-info-card">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" class="model-form">
              <!-- 第一行 -->
              <div class="form-row">
                <el-form-item :label="$t('model.modelName')" prop="modelName">
                  <el-input v-model="form.modelName" style="width: 200px" />
                </el-form-item>
                <el-form-item :label="$t('model.modelNameEn')" prop="modelNameEn">
                  <el-input v-model="form.modelNameEn" style="width: 200px" />
                </el-form-item>
                <el-form-item :label="$t('model.category')" prop="category">
                  <el-select v-model="form.category" :placeholder="$t('model.selectCategory')" style="width: 200px">
                    <el-option
                        v-for="dict in model_category"
                        :key="dict.value"
                        :label="$t(`model.categoryLabels.${dict.value}`) !== `model.categoryLabels.${dict.value}` ? $t(`model.categoryLabels.${dict.value}`) : dict.label"
                        :value="dict.value"
                    />
                  </el-select>
                </el-form-item>
              </div>
              <!-- 第二行 -->
              <div class="form-row">
                <el-form-item :label="$t('model.applicableGender')" prop="applicableGender">
                  <el-radio-group v-model="form.applicableGender" :disabled="isEdit">
                    <el-radio :value="0">{{ $t('model.genderAll') }}</el-radio>
                    <el-radio :value="1">{{ $t('model.genderMale') }}</el-radio>
                    <el-radio :value="2">{{ $t('model.genderFemale') }}</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item :label="$t('model.priority')" prop="priority">
                  <el-input-number v-model="form.priority" :min="0" :max="999" style="width: 200px" />
                </el-form-item>
                <el-form-item :label="$t('model.description')">
                  <el-input v-model="form.description" style="width: 200px" />
                </el-form-item>
              </div>
              <!-- 第三行 -->
              <div class="form-row">
                <el-form-item :label="$t('model.modelParams')">
                  <el-button type="primary" icon="Upload" @click="handleImport">{{ $t('model.importModelParams') }}</el-button>
                </el-form-item>
                <el-form-item :label="$t('model.pythonFile')" prop="pyPath">
                  <el-upload
                      ref="pythonUploadRef"
                      class="python-upload"
                      :action="rootUploadUrl"
                      :headers="headers"
                      :on-success="handlePythonUploadSuccess"
                      :on-error="handlePythonUploadError"
                      :before-upload="beforePythonUpload"
                      :limit="1"
                      :file-list="pythonFileList"
                      accept=".py"
                      :on-change="handlePythonChange"
                      :on-remove="handlePythonRemove"
                  >
                    <el-button type="primary" icon="Upload">{{ $t('model.uploadPython') }}</el-button>
                  </el-upload>
                </el-form-item>
                <el-form-item :label="$t('model.pklFile')" prop="pklPath">
                  <el-upload
                      ref="pklUploadRef"
                      class="pkl-upload"
                      :action="modelUploadUrl"
                      :headers="headers"
                      :on-success="handlePklUploadSuccess"
                      :on-error="handlePklUploadError"
                      :before-upload="beforePklUpload"
                      :on-change="handlePklChange"
                      :on-remove="handlePklRemove"
                      :limit="1"
                      accept=".pkl"
                      :file-list="pklFileList"
                  >
                    <el-button type="primary" icon="Upload">{{ $t('model.uploadPkl') }}</el-button>
                  </el-upload>
                </el-form-item>
              </div>
            </el-form>
          </div>
        </template>

        <div
            class="param-section"
            :class="{ collapsed: paramCollapsed, expanded: paramFullscreen }"
            v-loading="configLoading"
        >
          <div class="section-toolbar">
            <div class="toolbar-left">
              <span class="section-title">{{ $t('model.keyParams') }}</span>
              <el-tag type="success" size="small" round>{{ selectedParams.length }}</el-tag>
            </div>
            <div class="toolbar-right">
              <el-button link size="small" @click="paramCollapsed = !paramCollapsed" v-if="!paramFullscreen">
                <el-icon><component :is="paramCollapsed ? 'ArrowDown' : 'ArrowUp'" /></el-icon>
                {{ paramCollapsed ? $t('common.expand') : $t('common.collapse') }}
              </el-button>
              <el-button link size="small" @click="paramFullscreen = !paramFullscreen">
                <el-icon><component :is="paramFullscreen ? 'CloseBold' : 'FullScreen'" /></el-icon>
                {{ paramFullscreen ? $t('model.exitFullscreen') : $t('model.fullscreen') }}
              </el-button>
            </div>
          </div>

          <div class="param-panel" v-show="!paramCollapsed">
            <div class="param-left">
              <div class="panel-header">
                <span class="panel-title">{{ $t('model.allParams') }}</span>
                <div class="panel-actions">
                  <el-button link type="primary" size="small" @click="toggleExpandAll">
                    {{ allExpanded ? $t('common.collapseAll') : $t('common.expandAll') }}
                  </el-button>
                </div>
              </div>
              <div class="category-list">
                <div v-for="cat in displayCategories" :key="cat.categoryCode" class="category-group">
                  <div class="category-header" @click="toggleExpand(cat.categoryCode)">
                    <div class="category-header-left">
                      <el-icon class="expand-icon" :class="{ expanded: expandedMap[cat.categoryCode] }">
                        <ArrowRight />
                      </el-icon>
                      <el-checkbox
                          :model-value="isGroupAllSelected(cat)"
                          :indeterminate="isGroupIndeterminate(cat)"
                          @change="(val: boolean) => toggleGroup(cat, val)"
                          @click.stop
                      />
                      <span class="category-name">{{ isEnglish() ? (cat.categoryNameEn || cat.categoryName) : cat.categoryName }}</span>
                    </div>
                    <el-tag size="small" :type="getGroupSelectedCount(cat) > 0 ? 'success' : 'info'" round>
                      {{ getGroupSelectedCount(cat) }}/{{ cat.fields.length }}
                    </el-tag>
                  </div>
                  <div class="category-fields" v-show="expandedMap[cat.categoryCode]">
                    <div
                        v-for="field in cat.fields"
                        :key="field.fieldCode"
                        class="field-item"
                        :class="{ selected: selectedSet.has(field.fieldCode) }"
                    >
                      <el-checkbox
                          :model-value="selectedSet.has(field.fieldCode)"
                          @change="(val: boolean) => toggleField(field, cat, val)"
                      >
                        <span class="field-name">{{ isEnglish() ? (field.fieldNameEn || field.fieldName) : field.fieldName }}</span>
                        <span class="field-unit" v-if="field.unit">({{ isEnglish() ? (field.unitEn || field.unit) : field.unit }})</span>
                      </el-checkbox>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="param-right">
              <div class="panel-header">
                <span class="panel-title">
                  {{ $t('model.selectedKeyParams') }}
                  <el-tag type="success" size="small" round style="margin-left: 8px">{{ selectedParams.length }}</el-tag>
                </span>
                <el-button v-if="selectedParams.length > 0" link type="danger" size="small" @click="clearAll">
                  {{ $t('common.clearAll') }}
                </el-button>
              </div>
              <div class="selected-list" v-if="selectedParams.length > 0">
                <div v-for="group in selectedGrouped" :key="group.categoryCode" class="selected-group">
                  <div class="selected-group-header">
                    <span>{{ isEnglish() ? (group.categoryNameEn || group.categoryName) : group.categoryName }}</span>
                    <el-tag size="small" round>{{ group.fields.length }}</el-tag>
                  </div>
                  <div class="selected-group-fields">
                    <el-tag
                        v-for="f in group.fields"
                        :key="f.fieldCode"
                        closable
                        size="default"
                        class="selected-tag"
                        @close="toggleField(f, null, false)"
                    >{{ isEnglish() ? (f.fieldNameEn || f.fieldName) : f.fieldName }}</el-tag>
                  </div>
                </div>
              </div>
              <el-empty v-else :description="$t('model.selectParamsHint')" :image-size="80" />
            </div>
          </div>
        </div>

        <div class="form-footer">
          <div class="footer-info">
            {{ $t('model.selectedCount', { count: selectedParams.length }) }}
          </div>
          <div class="footer-actions">
            <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
              {{ $t('common.save') }}
            </el-button>
            <el-button size="large" @click="handleBack">{{ $t('common.cancel') }}</el-button>
          </div>
        </div>
        <!-- 导入对话框 -->
        <el-dialog :title="$t('model.importTitle')" v-model="importDialogVisible" width="400px" append-to-body>
          <el-upload
              ref="uploadRef"
              :limit="1"
              accept=".xlsx, .xls"
              :headers="upload.headers"
              :action="upload.url"
              :disabled="upload.isUploading"
              :on-progress="handleFileUploadProgress"
              :on-success="handleFileSuccess"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :auto-upload="false"
              drag
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">{{ $t('model.dragUpload') }}<em>{{ $t('model.clickUpload') }}</em></div>
            <template #tip>
              <div class="el-upload__tip text-center">
                <span>{{ $t('model.importFileTip') }}</span>
              </div>
            </template>
          </el-upload>
          <template #footer>
            <div class="dialog-footer">
              <el-button type="primary" @click="submitImport" :loading="upload.isUploading">{{ $t('common.confirm') }}</el-button>
              <el-button @click="importDialogVisible = false">{{ $t('common.cancel') }}</el-button>
            </div>
          </template>
        </el-dialog>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts" name="Model">
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import {Plus, Search, RefreshRight, ArrowRight, Upload, UploadFilled} from '@element-plus/icons-vue'
import { getModelList, toggleModelStatus, deleteModel, getModelById, createModel, updateModel, type AiModel } from "@/api/healthReport/model"
import {  getModelConfig, type CategoryConfig, type FieldConfig } from '@/api/healthReport/collection'

const { t: $t, locale } = useI18n()
const { proxy } = getCurrentInstance()

// 判断是否为英文环境
function isEnglish(): boolean {
  return locale.value === 'en-US' || locale.value === 'en'
}
const { model_category } = proxy.useDict("model_category")
const pythonUploadRef = ref(null)
const pklUploadRef = ref(null)
const modelUploadUrl = import.meta.env.VITE_APP_BASE_API + '/common/uploadModel'
const rootUploadUrl = import.meta.env.VITE_APP_BASE_API + '/common/uploadRoot'
const headers = ref({ Authorization: "Bearer " + getToken() })
// 新增：文件列表响应式变量
const pythonFileList = ref<any[]>([])
const pklFileList = ref<any[]>([])
interface SelectedField {
  fieldCode: string
  fieldName: string
  fieldNameEn?: string
  categoryCode: string
  categoryName: string
  categoryNameEn?: string
  unit?: string
  unitEn?: string
}

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<AiModel[]>([])
const total = ref(0)
const query = reactive({
  modelName: undefined as string | undefined,
  category: undefined as string | undefined,
  applicableGender: undefined as number | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})
import { getToken } from "@/utils/auth"
// 导入相关
const importDialogVisible = ref(false)
const uploadRef = ref<any>(null)
const upload = reactive({
  // 是否禁用上传
  isUploading: false,
  // 设置上传的请求头部
  headers: { Authorization: "Bearer " + getToken() },
  // 🔥 自动拼接 timeStop
  url: (() => {
    const baseUrl = import.meta.env.VITE_APP_BASE_API
    const api = "/healthReport/aiModel/importData"
    const timeStop = localStorage.getItem("timeStop") || ""
    const lang = locale.value === 'en-US' ? 'en' : 'zh'

    // 拼接 timeStop
    return baseUrl + api + "?timeStop=" + encodeURIComponent(timeStop)+ "&lang=" + lang
  })(),
  // 选中的文件
  selectedFile: null as any
})

// 表单相关
const showForm = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
// 保存编辑时的原始allParamCodes，确保衍生变量不会丢失
const originalAllParamCodes = ref<string[]>([])

const paramCollapsed = ref(false)
const paramFullscreen = ref(false)
const configLoading = ref(false)

const form = reactive({
  modelName: '',
  modelNameEn:'',
  category: '',
  applicableGender: 0,
  priority: 0,
  description: '',
  pyPath: '',
  pklPath: '',
})

const rules = {
  modelName: [{ required: true, message: $t('model.inputModelName'), trigger: 'blur' }],
  modelNameEn: [{ required: true, message: $t('model.inputModelNameEn'), trigger: 'blur' }],
  priority: [{ required: true, message: $t('model.inputPriority'), trigger: 'blur' }],
  category: [{ required: true, message: $t('model.selectCategory'), trigger: 'change' }],
  pyPath: [{ required: true, message: $t('model.uploadPythonRequired'), trigger: 'change' }],
  pklPath: [{ required: true, message: $t('model.uploadPklRequired'), trigger: 'change' }],
}

const categories = ref<CategoryConfig[]>([])
const selectedParams = ref<SelectedField[]>([])
const expandedMap = reactive<Record<string, boolean>>({})

// 过滤掉衍生变量分类用于显示（但保存时仍使用原始categories）
const displayCategories = computed(() => {
  return categories.value.filter(cat =>
      cat.categoryCode !== 'derived_variable' &&
      cat.categoryName !== '衍生变量'
  )
})

const selectedSet = computed(() => new Set(selectedParams.value.map(p => p.fieldCode)))

const allExpanded = computed(() => {
  if (displayCategories.value.length === 0) return false
  return displayCategories.value.every(c => expandedMap[c.categoryCode])
})

const selectedGrouped = computed(() => {
  const map = new Map<string, { categoryCode: string; categoryName: string; categoryNameEn?: string; fields: (SelectedField & { fieldNameEn?: string })[] }>()
  for (const p of selectedParams.value) {
    if (!map.has(p.categoryCode)) {
      map.set(p.categoryCode, { categoryCode: p.categoryCode, categoryName: p.categoryName, categoryNameEn: (p as any).categoryNameEn, fields: [] })
    }
    map.get(p.categoryCode)!.fields.push(p as any)
  }
  return Array.from(map.values())
})

function isGroupAllSelected(cat: CategoryConfig): boolean {
  if (cat.fields.length === 0) return false
  return cat.fields.every(f => selectedSet.value.has(f.fieldCode))
}

function isGroupIndeterminate(cat: CategoryConfig): boolean {
  if (cat.fields.length === 0) return false
  const count = cat.fields.filter(f => selectedSet.value.has(f.fieldCode)).length
  return count > 0 && count < cat.fields.length
}

function getGroupSelectedCount(cat: CategoryConfig): number {
  return cat.fields.filter(f => selectedSet.value.has(f.fieldCode)).length
}

function toggleGroup(cat: CategoryConfig, val: boolean) {
  if (val) {
    for (const f of cat.fields) {
      if (!selectedSet.value.has(f.fieldCode)) {
        selectedParams.value.push({
          fieldCode: f.fieldCode,
          fieldName: f.fieldName,
          categoryCode: cat.categoryCode,
          categoryName: cat.categoryName,
          unit: f.unit,
        })
      }
    }
  } else {
    const codes = new Set(cat.fields.map(f => f.fieldCode))
    selectedParams.value = selectedParams.value.filter(p => !codes.has(p.fieldCode))
  }
}

function toggleField(field: FieldConfig | SelectedField, cat: CategoryConfig | null, val: boolean) {
  if (val) {
    if (!selectedSet.value.has(field.fieldCode)) {
      const catName = cat?.categoryName || (field as SelectedField).categoryName || ''
      const catCode = cat?.categoryCode || (field as SelectedField).categoryCode || ''
      const catNameEn = (cat as any)?.categoryNameEn || (field as any).categoryNameEn || ''
      selectedParams.value.push({
        fieldCode: field.fieldCode,
        fieldName: field.fieldName,
        fieldNameEn: (field as any).fieldNameEn,
        categoryCode: catCode,
        categoryName: catName,
        categoryNameEn: catNameEn,
        unit: 'unit' in field ? field.unit : undefined,
        unitEn: (field as any).unitEn,
      } as any)
    }
  } else {
    selectedParams.value = selectedParams.value.filter(p => p.fieldCode !== field.fieldCode)
  }
}

function toggleExpand(code: string) {
  expandedMap[code] = !expandedMap[code]
}

function toggleExpandAll() {
  const target = !allExpanded.value
  for (const cat of displayCategories.value) {
    expandedMap[cat.categoryCode] = target
  }
}

function clearAll() {
  selectedParams.value = []
}

async function loadConfig() {
  configLoading.value = true
  try {
    const gender = form.applicableGender === 0 ? undefined : form.applicableGender
    const res = await getModelConfig(gender,'')
    categories.value = res.data

    // 调试：检查返回的数据是否包含英文字段
    if (res.data && res.data.length > 0) {
      const firstCat = res.data[0] as any
      console.log('Category sample:', {
        categoryName: firstCat.categoryName,
        categoryNameEn: firstCat.categoryNameEn,
        hasFields: firstCat.fields?.length > 0
      })
      if (firstCat.fields && firstCat.fields.length > 0) {
        const firstField = firstCat.fields[0] as any
        console.log('Field sample:', {
          fieldName: firstField.fieldName,
          fieldNameEn: firstField.fieldNameEn,
          unit: firstField.unit,
          unitEn: firstField.unitEn
        })
      }
    }

    for (const cat of res.data) {
      if (expandedMap[cat.categoryCode] === undefined) {
        expandedMap[cat.categoryCode] = false
      }
    }

    if (selectedParams.value.length > 0) {
      const allFieldCodes = new Set<string>()
      for (const cat of res.data) {
        for (const f of cat.fields) allFieldCodes.add(f.fieldCode)
      }
      selectedParams.value = selectedParams.value.filter(p => allFieldCodes.has(p.fieldCode))
    }
  } catch {
    ElMessage.error($t('model.loadConfigFailed'))
  } finally {
    configLoading.value = false
  }
}



async function fetchData() {
  loading.value = true
  try {
    const res = await getModelList(query)
    tableData.value = res.rows
    total.value = res.total
  } catch { /* handled */ } finally { loading.value = false }
}

function resetQuery() {
  query.modelName = ''
  query.category = undefined
  query.applicableGender = undefined
  form.priority = 0
  query.status = undefined
  query.pageNum = 1
  fetchData()
}

async function handleToggleStatus(row: AiModel, newStatus: number) {
  try {
    await toggleModelStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? $t('model.enabled') : $t('model.disabled'))
  } catch { /* handled */ }
}

async function handleDelete(id: number) {
  try {
    await deleteModel(id)
    ElMessage.success($t('common.deleteSuccess'))
    fetchData()
  } catch { /* handled */ }
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  form.modelName = ''
  form.modelNameEn = ''
  form.category = ''
  form.applicableGender = 0
  form.priority=0
  form.description = ''
  // 2. 核心：清空文件路径（关键！）
  form.pyPath = ''
  form.pklPath = ''
  // 3. 清空文件列表（组件回显状态）
  pythonFileList.value = []
  pklFileList.value = []
  selectedParams.value = []
  categories.value = []
  showForm.value = true
}

async function handleEdit(row: AiModel) {
  isEdit.value = true
  editId.value = row.id
  showForm.value = true

  try {
    const res = await getModelById(row.id)
    const data = res.data
    form.modelName = data.modelName
    form.modelNameEn = data.modelNameEn
    form.category = data.category
    form.applicableGender = data.applicableGender ?? 0
    form.priority = data.priority ?? 0
    form.description = data.description || ''
    form.pyPath = data.pyPath || ''
    form.pklPath = data.pklPath || ''
    // 👇 核心：编辑时回显文件列表
    if (data.pyPath) {
      const fileName = data.pyPath.split('/').pop() || 'python文件'
      pythonFileList.value = [{
        name: fileName,
        url: data.pyPath,
        status: 'success'
      }]
    } else {
      pythonFileList.value = []
    }

    if (data.pklPath) {
      const fileName = data.pklPath.split('/').pop() || 'pkl文件'
      pklFileList.value = [{
        name: fileName,
        url: data.pklPath,
        status: 'success'
      }]
    } else {
      pklFileList.value = []
    }
    // 获取该模型的所有参数和关键参数
    let allParamCodes: string[] = []
    let requiredCodes: string[] = []

    if (data.allParams) {
      if (typeof data.allParams === 'string') {
        try { allParamCodes = JSON.parse(data.allParams) } catch { allParamCodes = [] }
      } else if (Array.isArray(data.allParams)) {
        allParamCodes = data.allParams
      }
    }

    if (data.requiredParams) {
      if (typeof data.requiredParams === 'string') {
        try { requiredCodes = JSON.parse(data.requiredParams) } catch { requiredCodes = [] }
      } else if (Array.isArray(data.requiredParams)) {
        requiredCodes = data.requiredParams
      }
    }

    // 保存原始的allParamCodes，编辑时需要保持原样（包括衍生变量）
    originalAllParamCodes.value = [...allParamCodes]

    // 加载配置
    await loadConfig()

    // 过滤出该模型的参数（而不是全部参数）
    const allParamCodeSet = new Set(allParamCodes)
    const requiredCodeSet = new Set(requiredCodes)

    // 构建该模型的分类和字段
    const modelCategories: CategoryConfig[] = []
    for (const cat of categories.value) {
      const modelFields = cat.fields.filter(f => allParamCodeSet.has(f.fieldCode))
      if (modelFields.length > 0) {
        modelCategories.push({
          ...cat,
          fields: modelFields
        })
      }
    }
    categories.value = modelCategories

    // 设置已选关键参数
    const params: SelectedField[] = []
    for (const cat of categories.value) {
      const catAny = cat as any
      for (const f of cat.fields) {
        if (requiredCodeSet.has(f.fieldCode)) {
          const fAny = f as any
          params.push({
            fieldCode: f.fieldCode,
            fieldName: f.fieldName,
            fieldNameEn: fAny.fieldNameEn,
            categoryCode: cat.categoryCode,
            categoryName: cat.categoryName,
            categoryNameEn: catAny.categoryNameEn,
            unit: f.unit,
            unitEn: fAny.unitEn,
          })
        }
      }
    }
    selectedParams.value = params

    // 调试：检查设置的参数
    console.log('Selected params:', params.map(p => ({
      fieldName: p.fieldName,
      fieldNameEn: p.fieldNameEn,
      categoryName: p.categoryName,
      categoryNameEn: p.categoryNameEn
    })))
  } catch { /* handled */ }
}

function handleBack() {
  showForm.value = false
  isEdit.value = false
  editId.value = null
  paramFullscreen.value = false
  paramCollapsed.value = false
  form.pyPath = ''
  form.pklPath = ''
  pythonFileList.value = []
  pklFileList.value = []
}

function handleImport() {
  importDialogVisible.value = true
  upload.selectedFile = null
}

// 文件上传成功处理
function handlePythonUploadSuccess(response, file, fileList) {
  if (response.code === 200) {
    form.pyPath = response.fileName;
    // 同步文件列表
    pythonFileList.value = [{
      name: file.name,
      url: response.url,
      status: 'success'
    }]
    ElMessage.success($t('model.uploadSuccess'))
  } else {
    ElMessage.error($t('model.uploadFailed'))
  }
}

// 文件上传失败处理
function handlePythonUploadError(error) {
  ElMessage.error($t('model.uploadFailedRetry'))
}

// 上传前验证
function beforePythonUpload(file) {
  const maxSize = 10 * 1024 * 1024 // 10MB
  if (file.size > maxSize) {
    ElMessage.error($t('model.fileSizeLimit', { size: 10 }))
    return false
  }
  return true
}

// Python文件选择变化处理
function handlePythonChange(file: any, fileList: any[]) {
  if (file.status === 'ready') {
    console.log('Python file selected:', file.name)
  }
}

// Python文件删除处理
function handlePythonRemove(file: any, fileList: any[]) {
  form.pyPath = ''
  pythonFileList.value = []
}

// 文件上传成功处理
function handlePklUploadSuccess(response, file, fileList) {
  if (response.code === 200) {
    form.pklPath = response.fileName;
    // 同步文件列表
    pklFileList.value = [{
      name: file.name,
      url: response.url,
      status: 'success'
    }]
    ElMessage.success($t('model.uploadSuccess'))
  } else {
    ElMessage.error($t('model.uploadFailed'))
  }
}

// 文件上传失败处理
function handlePklUploadError(error) {
  ElMessage.error($t('model.uploadFailedRetry'))
}

// 上传前验证
function beforePklUpload(file) {
  const maxSize = 10 * 1024 * 1024 // 10MB
  if (file.size > maxSize) {
    ElMessage.error($t('model.fileSizeLimit', { size: 10 }))
    return false
  }
  return true
}

// PKL文件选择变化处理
function handlePklChange(file: any, fileList: any[]) {
  if (file.status === 'ready') {
    console.log('PKL file selected:', file.name)
  }
}

// PKL文件删除处理
function handlePklRemove(file: any, fileList: any[]) {
  form.pklPath = ''
  pklFileList.value = []
}

// 文件上传中处理
const handleFileUploadProgress = () => {
  upload.isUploading = true
}

// 文件选择处理
const handleFileChange = (file: any) => {
  upload.selectedFile = file
}

// 文件删除处理
const handleFileRemove = () => {
  upload.selectedFile = null
}

// 文件上传成功处理
const handleFileSuccess = (response: any, file: any) => {
  importDialogVisible.value = false
  upload.isUploading = false
  uploadRef.value?.handleRemove(file)
  ElMessage.success(response.data.msg || $t('collection.importSuccess'))
  // 如果接口返回了categories数据，则替换当前categories
  if (response.data && response.data.categories) {
    categories.value = response.data.categories
    // 初始化展开状态
    for (const cat of response.data.categories) {
      if (expandedMap[cat.categoryCode] === undefined) {
        expandedMap[cat.categoryCode] = false
      }
    }
    // 过滤掉已选参数中不存在于新categories中的参数
    if (selectedParams.value.length > 0) {
      const allFieldCodes = new Set<string>()
      for (const cat of response.data.categories) {
        for (const f of cat.fields) allFieldCodes.add(f.fieldCode)
      }
      selectedParams.value = selectedParams.value.filter(p => allFieldCodes.has(p.fieldCode))
    }
  }
  fetchData()
}

// 提交上传文件
function submitImport() {
  const file = upload.selectedFile
  console.log(file.name)
  if (!file || !file.name.toLowerCase().endsWith('.xls') && !file.name.toLowerCase().endsWith('.xlsx')) {
    ElMessage.error($t('model.selectExcelFile'))
    return
  }
  uploadRef.value?.submit()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (selectedParams.value.length === 0) {
    ElMessage.warning($t('model.selectAtLeastOneParam'))
    return
  }

  submitting.value = true
  try {
    // 已选关键参数
    const requiredParamCodes = selectedParams.value.map(p => p.fieldCode)
    // 所有参数：编辑时使用原始保存的allParamCodes，新增时从categories中提取所有字段
    let allParamCodes: string[] = []
    if (isEdit.value && originalAllParamCodes.value.length > 0) {
      // 编辑模式：使用原始的allParamCodes，确保衍生变量等不会丢失
      allParamCodes = [...originalAllParamCodes.value]
    } else {
      // 新增模式：从categories中提取所有字段
      for (const cat of categories.value) {
        for (const field of cat.fields) {
          allParamCodes.push(field.fieldCode)
        }
      }
    }

    const data: any = {
      ...form,
      requiredParams: JSON.stringify(requiredParamCodes),
      requiredParamCount: requiredParamCodes.length,
      allParams: JSON.stringify(allParamCodes),
      allParamCount: allParamCodes.length,
      paramCount: requiredParamCodes.length,
    }
    if (isEdit.value && editId.value) {
      await updateModel(editId.value, data)
    } else {
      await createModel(data)
    }
    ElMessage.success($t('common.saveSuccess'))
    handleBack()
    fetchData()
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  fetchData()
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
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 12px;

  h3 {
    font-size: 1.25rem;
    font-weight: 600;
    color: #0f172a;
    letter-spacing: -0.02em;
    margin: 0;
  }

  .header-actions {
    display: flex;
    gap: 12px;
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
    width: 240px;
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

.model-form-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 132px);

  &.fullscreen-mode {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 2000;
    background: #f8fafc;
    padding: 24px;
    height: 100vh;
    .param-section { flex: 1; }
  }
}

.form-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
  padding: 24px;
}

.basic-info-card {
  margin-bottom: 16px;
  flex-shrink: 0;

  .model-form {
    .form-row {
      display: flex;
      align-items: flex-start;
      min-height: 50px;
      margin-bottom: 5px;

      &:last-child {
        margin-bottom: 0;
      }

      .el-form-item {
        margin-right: 24px;
        margin-bottom: 0;

        &:last-child {
          margin-right: 0;
        }
      }
    }
  }
}

.param-section {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);

  &.collapsed {
    flex: 0 0 auto;
    .param-panel { display: none; }
  }
}

.section-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.param-panel {
  display: flex;
  gap: 0;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.param-left, .param-right {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.param-left {
  flex: 3;
  border-right: 1px solid #e2e8f0;
}

.param-right {
  flex: 2;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
  background: #f8fafc;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  display: flex;
  align-items: center;
}

.panel-actions { display: flex; gap: 4px; }

.category-list {
  overflow-y: auto;
  flex: 1;
  padding: 12px;
}

.category-group {
  margin-bottom: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.2s;
  &:hover { border-color: #cbd5e1; }
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f8fafc;
  cursor: pointer;
  transition: background 0.2s;
  &:hover { background: #f1f5f9; }
}

.category-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.expand-icon {
  transition: transform 0.2s;
  font-size: 12px;
  color: #64748b;
  &.expanded { transform: rotate(90deg); }
}

.category-name {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
}

.category-fields {
  padding: 8px 14px 8px 44px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  background: #ffffff;
}

.field-item {
  padding: 2px 0;
  &.selected {
    :deep(.el-checkbox__label) { color: #0d9488; font-weight: 500; }
  }
  :deep(.el-checkbox__label) { font-size: 13px; }
}

.field-name { color: #0f172a; }
.field-unit { color: #64748b; font-size: 12px; margin-left: 2px; }

.selected-list {
  overflow-y: auto;
  flex: 1;
  padding: 16px;
}

.selected-group { margin-bottom: 16px; }

.selected-group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #0d9488;
  padding-bottom: 6px;
  border-bottom: 1px dashed #e2e8f0;
}

.selected-group-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.selected-tag {
  border-color: #ccfbf1;
  background: #f0fdfa;
  color: #0d9488;
  :deep(.el-tag__close) {
    color: #0d9488;
    &:hover { background: #0d9488; color: #fff; }
  }
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  flex-shrink: 0;
  border-top: 1px solid #e2e8f0;
  margin-top: 10px;
}

.footer-info {
  font-size: 14px;
  color: #0d9488;
  font-weight: 600;
}

.footer-actions {
  display: flex;
  gap: 12px;
}
</style>