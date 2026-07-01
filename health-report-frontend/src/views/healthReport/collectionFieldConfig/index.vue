<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item :label="$t('collection.category')" prop="categoryName">
        <el-input
            v-model="queryParams.categoryName"
            :placeholder="$t('collection.inputCategory')"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('collection.fieldName')" prop="fieldName">
        <el-input
            v-model="queryParams.fieldName"
            :placeholder="$t('collection.inputFieldName')"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('collection.aliasName')" prop="aliasName">
        <el-input
            v-model="queryParams.aliasName"
            :placeholder="$t('collection.inputAlias')"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('collection.unit')" prop="unit">
        <el-input
            v-model="queryParams.unit"
            :placeholder="$t('collection.inputUnit')"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('collection.statusLabel')" prop="status">
        <el-select v-model="queryParams.status" :placeholder="$t('collection.statusLabel')" clearable>
          <el-option :label="$t('collection.enabled')" value='1' />
          <el-option :label="$t('collection.disabled')" value='0' />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">{{ $t('collection.search') }}</el-button>
        <el-button icon="Refresh" @click="resetQuery">{{ $t('collection.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <!--      <el-col :span="1.5">
              <el-button
                type="primary"
                plain
                icon="Plus"
                @click="handleAdd"
                v-hasPermi="['healthReport:collectionFieldConfig:add']"
              >新增</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button
                type="success"
                plain
                icon="Edit"
                :disabled="single"
                @click="handleUpdate"
                v-hasPermi="['healthReport:collectionFieldConfig:edit']"
              >修改</el-button>
            </el-col>-->
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['healthReport:collectionFieldConfig:remove']"
        >{{ $t('collection.delete') }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Upload" @click="handleImport" >{{ $t('collection.import') }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="Download"
            @click="handleExport"
            v-hasPermi="['healthReport:collectionFieldConfig:export']"
        >{{ $t('collection.export') }}</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="collectionFieldConfigList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column :label="$t('collection.fieldCode')" align="center" prop="id" />
      <el-table-column :label="$t('collection.category')" align="center">
        <template #default="{ row }">
          {{ locale === 'en-US' ? (row.categoryNameEn || row.categoryName) : row.categoryName }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('collection.fieldName')" align="center">
        <template #default="{ row }">
          {{ locale === 'en-US' ? (row.fieldNameEn || row.fieldName) : row.fieldName }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('collection.aliasName')" align="center" prop="aliasName" >
        <template #default="{ row }">
          {{ locale === 'en-US' ? (row.aliasNameEn || row.aliasName) : row.aliasName }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('collection.unit')" align="center" prop="unit" >
        <template #default="{ row }">
          {{ locale === 'en-US' ? (row.unitEn || row.unit) : row.unit }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('collection.refRange')" align="center" prop="refRangeText" >
        <template #default="{ row }">
          {{ locale === 'en-US' ? (row.refRangeTextEn || row.refRangeText) : row.refRangeText }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('collection.remark')" align="center" prop="remark" >
        <template #default="{ row }">
          {{ locale === 'en-US' ? (row.remarkEn || row.remark) : row.remark }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('collection.statusLabel')" align="center" prop="status" >
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? $t('collection.enabled') : $t('collection.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('collection.operation')" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['healthReport:collectionFieldConfig:edit']">{{ $t('collection.edit') }}</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['healthReport:collectionFieldConfig:remove']">{{ $t('collection.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />

    <!-- 添加或修改指标管理对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="collectionFieldConfigRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('collection.category')" prop="categoryName">
              <el-input v-model="form.categoryName" :placeholder="$t('collection.inputCategory')" :disabled="true" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('collection.fieldName')" prop="fieldName">
              <el-input v-model="form.fieldName" :placeholder="$t('collection.inputFieldName')" :disabled="true" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('collection.fieldNameEn')" prop="fieldNameEn">
              <el-input v-model="form.fieldNameEn" :placeholder="$t('collection.inputFieldNameEn')" :disabled="true" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('collection.sortOrder')" prop="sortOrder">
              <el-input v-model="form.sortOrder" :placeholder="$t('collection.inputSort')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('collection.unit')" prop="unit">
              <el-input v-model="form.unit" :placeholder="$t('collection.inputUnit')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('collection.unitEn')" prop="unitEn">
              <el-input v-model="form.unitEn" :placeholder="$t('collection.inputUnitEn')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('collection.aliasName')" prop="aliasName">
              <el-input v-model="form.aliasName" type="textarea" :placeholder="$t('collection.inputAlias')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('collection.aliasNameEn')" prop="aliasNameEn">
              <el-input v-model="form.aliasNameEn" type="textarea" :placeholder="$t('collection.inputAliasEn')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('collection.refRange')" prop="refRangeText">
              <el-input v-model="form.refRangeText" type="textarea" :placeholder="$t('collection.refRange')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('collection.refRangeEn')" prop="refRangeTextEn">
              <el-input v-model="form.refRangeTextEn" type="textarea" :placeholder="$t('collection.inputRefRangeEn')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('collection.remark')" prop="remark">
              <el-input v-model="form.remark" type="textarea" :placeholder="$t('collection.inputRemark')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('collection.remarkEn')" prop="remarkEn">
              <el-input v-model="form.remarkEn" type="textarea" :placeholder="$t('collection.inputRemarkEn')" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ $t('collection.confirm') }}</el-button>
          <el-button @click="cancel">{{ $t('collection.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>
    <!-- 指标导入对话框 -->
    <el-dialog :title="$t('collection.importTitle')" v-model="upload.open" width="400px" append-to-body>
      <el-upload ref="uploadRef" :limit="1" accept=".xlsx, .xls" :headers="upload.headers" :action="upload.url " :disabled="upload.isUploading || uploadLoading" :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :on-change="handleFileChange" :on-error="handleFileError" :on-remove="handleFileRemove" :auto-upload="false" drag>
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">{{ $t('collection.importTip') }}<em>{{ $t('collection.clickUpload') }}</em></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <span>{{ $t('collection.fileTypeLimit') }}</span>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFileForm" :loading="uploadLoading">{{ $t('collection.confirm') }}</el-button>
          <el-button @click="upload.open = false">{{ $t('collection.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CollectionFieldConfig">
import {useI18n} from 'vue-i18n'
import {
  listCollectionFieldConfig,
  getCollectionFieldConfig,
  delCollectionFieldConfig,
  addCollectionFieldConfig,
  updateCollectionFieldConfig
} from "@/api/healthReport/collectionFieldConfig"

const {locale, t: $t} = useI18n()
// 新增：上传loading状态
const uploadLoading = ref(false)
const {proxy} = getCurrentInstance()

const collectionFieldConfigList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
import {getToken} from "@/utils/auth"

/*** 指标导入参数 */
const upload = reactive({
  // 是否显示弹出层（用户导入）
  open: false,
  // 弹出层标题（用户导入）
  title: "",
  // 是否禁用上传
  isUploading: false,
  // 设置上传的请求头部
  headers: {Authorization: "Bearer " + getToken()},
  // 🔥 自动拼接 timeStop
  url: (() => {
    const baseUrl = import.meta.env.VITE_APP_BASE_API
    const api = "/healthReport/collectionFieldConfig/importData"
    const timeStop = localStorage.getItem("timeStop") || ""
    const lang = locale.value === 'en-US' ? 'en' : 'zh'
    // 拼接 timeStop
    return baseUrl + api + "?timeStop=" + encodeURIComponent(timeStop)+ "&lang=" + lang
  })()
})
const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    fieldName: null,
    categoryName: null,
    aliasName: null,
    unit: null,
    status: null
  },
  rules: {
    fieldName: [
      {required: true, message: () => $t('collection.fieldName') + ' ' + $t('collection.required'), trigger: "blur"}
    ],
    categoryName: [
      {required: true, message: () => $t('collection.category') + ' ' + $t('collection.required'), trigger: "blur"}
    ],
  }
})

const {queryParams, form, rules} = toRefs(data)

/** 查询指标管理列表 */
function getList() {
  loading.value = true
  listCollectionFieldConfig(queryParams.value).then(response => {
    collectionFieldConfigList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 取消按钮
function cancel() {
  open.value = false
  reset()
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    fieldName: null,
    fieldNameEn: null,
    categoryCode: null,
    aliasName: null,
    aliasNameEn: null,
    unit: null,
    unitEn: null,
    refRangeText: null,
    refRangeTextEn: null,
    sortOrder: null,
    remark: null,
    remarkEn: null
  }
  proxy.resetForm("collectionFieldConfigRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = $t('collection.addTitle')
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getCollectionFieldConfig(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = $t('collection.editTitle')
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["collectionFieldConfigRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateCollectionFieldConfig(form.value).then(response => {
          proxy.$modal.msgSuccess($t('collection.editSuccess'))
          open.value = false
          getList()
        })
      } else {
        addCollectionFieldConfig(form.value).then(response => {
          proxy.$modal.msgSuccess($t('collection.addSuccess'))
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
  proxy.$modal.confirm($t('collection.deleteConfirm', {ids: _ids})).then(function () {
    return delCollectionFieldConfig(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess($t('collection.deleteSuccess'))
  }).catch(() => {
  })
}

/** 导出按钮操作 */
function handleExport() {
  const languageType = locale.value === 'en-US' ? 'en' : 'zh'
  const fileName = locale.value === 'en-US'
      ? `Field_Config_${new Date().getTime()}.xlsx`
      : `指标配置_${new Date().getTime()}.xlsx`
  proxy.download('healthReport/collectionFieldConfig/export', {
    ...queryParams.value,
    languageType
  }, fileName)
}

/** 导入按钮操作 */
function handleImport() {
  upload.open = true
  upload.selectedFile = null
}

/**文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
  upload.isUploading = true
}

/** 文件选择处理 */
const handleFileChange = (file, fileList) => {
  upload.selectedFile = file
}

/** 文件删除处理 */
const handleFileRemove = (file, fileList) => {
  upload.selectedFile = null
}

/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
  uploadLoading.value = false
  upload.isUploading = false
  upload.open = false
  upload.isUploading = false
  proxy.$refs["uploadRef"].handleRemove(file)
  proxy.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", $t('collection.importResult'), {dangerouslyUseHTMLString: true})
  getList()
}

/** 文件上传失败处理（新增这个方法，处理异常场景） */
const handleFileError = (err, file, fileList) => {
  // ✅ 上传失败也关闭loading
  uploadLoading.value = false
  upload.isUploading = false
  proxy.$modal.msgError($t('collection.importFailed'))
}

/** 提交上传文件 */
function submitFileForm() {
  const file = upload.selectedFile
  if (!file || file.length === 0 || !file.name.toLowerCase().endsWith('.xls') && !file.name.toLowerCase().endsWith('.xlsx')) {
    proxy.$modal.msgError($t('collection.selectFileFirst'))
    return
  }
  uploadLoading.value = true
  upload.isUploading = true
  proxy.$refs["uploadRef"].submit()
}

getList()
</script>
