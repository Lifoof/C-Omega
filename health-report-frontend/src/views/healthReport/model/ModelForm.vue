<template>
  <div class="page-container model-form-page" :class="{ 'fullscreen-mode': paramFullscreen }">
    <!-- Basic info (hidden in fullscreen) -->
    <template v-if="!paramFullscreen">
      <div class="page-header">
        <h3>{{ isEdit ? $t('model.editTitle') : $t('model.createTitle') }}</h3>
        <div class="header-actions">
          <el-button type="primary" @click="handleImport">
            <el-icon><Upload /></el-icon>{{ $t('common.import') }}
          </el-button>
          <el-button @click="$router.back()">{{ $t('common.back') }}</el-button>
        </div>
      </div>
      <div class="form-card basic-info-card">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" inline>
          <el-form-item :label="$t('model.modelName')" prop="modelName">
            <el-input v-model="form.modelName" style="width: 200px" />
          </el-form-item>
          <el-form-item :label="$t('model.modelCode')" prop="modelCode">
            <el-input v-model="form.modelCode" :disabled="isEdit" style="width: 200px" />
          </el-form-item>

          <!-- 模型分类 - 下拉框 字典：model_category -->
          <el-form-item :label="$t('model.category')" prop="category">
            <el-select v-model="form.category" :placeholder="$t('model.selectCategory')" style="width: 200px">
              <el-option
                  v-for="dict in model_category"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="$t('model.applicableGender')" prop="applicableGender">
            <el-radio-group v-model="form.applicableGender" @change="onGenderChange">
              <el-radio :value="0">{{ $t('model.genderAll') }}</el-radio>
              <el-radio :value="1">{{ $t('model.genderMale') }}</el-radio>
              <el-radio :value="2">{{ $t('model.genderFemale') }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="$t('model.description')">
            <el-input v-model="form.description" type="textarea" :rows="2" style="width: 440px" />
          </el-form-item>
        </el-form>
      </div>
    </template>

    <!-- Param panel with expand/collapse/fullscreen -->
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
        <!-- Left: All params by category -->
        <div class="param-left">
          <div class="panel-header">
            <span class="panel-title">{{ $t('model.allParams') }}</span>
            <div class="panel-actions">
              <el-button link type="primary" size="small" @click="toggleExpandAll">
                {{ allExpanded ? $t('model.collapseAll') : $t('model.expandAll') }}
              </el-button>
            </div>
          </div>
          <div class="category-list">
            <div v-for="cat in categories" :key="cat.categoryCode" class="category-group">
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
                  <span class="category-name">{{ cat.categoryName }}</span>
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
                    <span class="field-name">{{ field.fieldName }}</span>
                    <span class="field-unit" v-if="field.unit">({{ field.unit }})</span>
                  </el-checkbox>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Selected key params -->
        <div class="param-right">
          <div class="panel-header">
            <span class="panel-title">
              {{ $t('model.selectedKeyParams') }}
              <el-tag type="success" size="small" round style="margin-left: 8px">{{ selectedParams.length }}</el-tag>
            </span>
            <el-button v-if="selectedParams.length > 0" link type="danger" size="small" @click="clearAll">
              {{ $t('model.clearAll') }}
            </el-button>
          </div>
          <div class="selected-list" v-if="selectedParams.length > 0">
            <div v-for="group in selectedGrouped" :key="group.categoryCode" class="selected-group">
              <div class="selected-group-header">
                <span>{{ group.categoryName }}</span>
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
                >{{ f.fieldName }}</el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else :description="$t('model.noParamsSelected')" :image-size="80" />
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="form-footer">
      <div class="footer-info">
        {{ $t('model.selectedCount', { count: selectedParams.length }) }}
      </div>
      <div class="footer-actions">
        <el-button type="primary" size="large" @click="handleSubmit" :loading="loading">
          {{ $t('common.save') }}
        </el-button>
        <el-button size="large" @click="$router.back()">{{ $t('common.cancel') }}</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { ArrowRight, Upload } from '@element-plus/icons-vue'
import { getModelById, createModel, updateModel } from "@/api/healthReport/model"
import { getCollectionConfig, type CategoryConfig, type FieldConfig } from '@/api/healthReport/collection'
const { proxy } = getCurrentInstance()
const { model_category } = proxy.useDict("model_category")

interface SelectedField {
  fieldCode: string
  fieldName: string
  categoryCode: string
  categoryName: string
  unit?: string
}

const { t: $t } = useI18n()
const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const configLoading = ref(false)
const isEdit = computed(() => !!route.params.id)

const paramCollapsed = ref(false)
const paramFullscreen = ref(false)

const form = reactive({
  modelName: '',
  modelCode: '',
  category: '',
  applicableGender: 0,
  description: '',
})


const rules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelCode: [{ required: true, message: '请输入模型编码', trigger: 'blur' }],
  category: [{ required: true, message: '请选择模型分类', trigger: 'change' }],
}

const categories = ref<CategoryConfig[]>([])
const selectedParams = ref<SelectedField[]>([])
const expandedMap = reactive<Record<string, boolean>>({})

const selectedSet = computed(() => new Set(selectedParams.value.map(p => p.fieldCode)))

const allExpanded = computed(() => {
  if (categories.value.length === 0) return false
  return categories.value.every(c => expandedMap[c.categoryCode])
})

const selectedGrouped = computed(() => {
  const map = new Map<string, { categoryCode: string; categoryName: string; fields: SelectedField[] }>()
  for (const p of selectedParams.value) {
    if (!map.has(p.categoryCode)) {
      map.set(p.categoryCode, { categoryCode: p.categoryCode, categoryName: p.categoryName, fields: [] })
    }
    map.get(p.categoryCode)!.fields.push(p)
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
      selectedParams.value.push({
        fieldCode: field.fieldCode,
        fieldName: field.fieldName,
        categoryCode: catCode,
        categoryName: catName,
        unit: 'unit' in field ? field.unit : undefined,
      })
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
  for (const cat of categories.value) {
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
    const res = await getCollectionConfig(gender,'')
    categories.value = res.data

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
    ElMessage.error('加载参数配置失败')
  } finally {
    configLoading.value = false
  }
}

function onGenderChange() {
  loadConfig()
}

async function loadData() {
  if (!isEdit.value) return
  try {
    const res = await getModelById(route.params.id as any)
    const data = res.data
    form.modelName = data.modelName
    form.modelCode = data.modelCode
    form.category = data.category
    form.applicableGender = data.applicableGender ?? 0
    form.description = data.description || ''

    if (data.requiredParams) {
      let codes: string[] = []
      try {
        codes = typeof data.requiredParams === 'string' ? JSON.parse(data.requiredParams) : data.requiredParams
      } catch {
        codes = []
      }

      await loadConfig()

      const codeSet = new Set(codes)
      const params: SelectedField[] = []
      for (const cat of categories.value) {
        for (const f of cat.fields) {
          if (codeSet.has(f.fieldCode)) {
            params.push({
              fieldCode: f.fieldCode,
              fieldName: f.fieldName,
              categoryCode: cat.categoryCode,
              categoryName: cat.categoryName,
              unit: f.unit,
            })
          }
        }
      }
      selectedParams.value = params
    }
  } catch { /* handled */ }
}

async function handleSubmit() {
  if (!paramFullscreen) {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return
  }

  if (selectedParams.value.length === 0) {
    ElMessage.warning($t('model.noParamsSelected'))
    return
  }

  loading.value = true
  try {
    const paramCodes = selectedParams.value.map(p => p.fieldCode)
    const data: any = {
      ...form,
      requiredParams: JSON.stringify(paramCodes),
      paramCount: paramCodes.length,
    }
    if (isEdit.value) {
      await updateModel(route.params.id as any, data)
    } else {
      await createModel(data)
    }
    ElMessage.success($t('common.success'))
    router.push('/model')
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

function handleImport() {
  // 这里可以实现导入功能
  ElMessage.info('导入功能待实现')
}

onMounted(async () => {
  if (isEdit.value) {
    await loadData()
  } else {
    await loadConfig()
  }
})
</script>

<style lang="scss" scoped>
.page-container {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
  height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
}

.model-form-page {
  &.fullscreen-mode {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 2000;
    background: #f8fafc;
    padding: 24px;
    .param-section { flex: 1; }
  }
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

  .header-actions {
    display: flex;
    gap: 12px;
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
  :deep(.el-form--inline .el-form-item) {
    margin-right: 24px;
    margin-bottom: 12px;
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
  margin-top: 16px;
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