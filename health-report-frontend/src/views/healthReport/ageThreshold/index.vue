<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item :label="$t('system.ageThreshold.age')" prop="age">
        <el-input
            v-model="queryParams.age"
            :placeholder="$t('system.ageThreshold.agePlaceholder')"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('system.ageThreshold.gender')" prop="gender">
        <el-select v-model="queryParams.gender" :placeholder="$t('system.ageThreshold.genderPlaceholder')" clearable>
          <el-option :label="$t('model.genderMale')" :value="1" />
          <el-option :label="$t('model.genderFemale')" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('system.ageThreshold.fieldName')" prop="fieldName">
        <el-input
            v-model="queryParams.fieldName"
            :placeholder="$t('system.ageThreshold.fieldNamePlaceholder')"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('system.ageThreshold.minVal')" prop="minVal">
        <el-input
            v-model="queryParams.minVal"
            :placeholder="$t('system.ageThreshold.minValPlaceholder')"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="$t('system.ageThreshold.maxVal')" prop="maxVal">
        <el-input
            v-model="queryParams.maxVal"
            :placeholder="$t('system.ageThreshold.maxValPlaceholder')"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">{{ $t('system.ageThreshold.search') }}</el-button>
        <el-button icon="Refresh" @click="resetQuery">{{ $t('system.ageThreshold.reset') }}</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <!--      <el-col :span="1.5">
              <el-button
                type="primary"
                plain
                icon="Plus"
                @click="handleAdd"
                v-hasPermi="['healthReport:ageThreshold:add']"
              >新增</el-button>
            </el-col>-->
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
        >{{ $t('system.ageThreshold.delete') }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Upload" @click="handleImport" >{{ $t('system.ageThreshold.import') }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="Download"
            @click="handleExport"
        >{{ $t('system.ageThreshold.export') }}</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="ageThresholdList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column :label="$t('system.ageThreshold.fieldCode')" align="center" prop="id" />
      <el-table-column :label="$t('system.ageThreshold.age')" align="center" prop="age" />
      <el-table-column :label="$t('system.ageThreshold.gender')" align="center" prop="gender">
        <template #default="scope">
          <span>{{ scope.row.gender === 1 ? $t('model.genderMale') : scope.row.gender === 2 ? $t('model.genderFemale') : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('system.ageThreshold.fieldName')" align="center" prop="fieldName" >
        <template #default="{ row }">
          {{ locale === 'en-US' ? (row.fieldNameEn || row.fieldName) : row.fieldName }}
        </template>
      </el-table-column>
      <el-table-column :label="$t('system.ageThreshold.minVal')" align="center" prop="minVal" />
      <el-table-column :label="$t('system.ageThreshold.maxVal')" align="center" prop="maxVal" />
      <el-table-column :label="$t('system.ageThreshold.unit')" align="center" prop="unit" />
      <el-table-column :label="$t('system.ageThreshold.operation')" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">{{ $t('system.ageThreshold.edit') }}</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)">{{ $t('system.ageThreshold.delete') }}</el-button>
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

    <!-- 添加或修改指标年龄阈值对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="ageThresholdRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item :label="$t('system.ageThreshold.age')" prop="age">
          <el-input v-model="form.age" :placeholder="$t('system.ageThreshold.agePlaceholder')" :disabled="true"  />
        </el-form-item>
        <el-form-item :label="$t('system.ageThreshold.gender')" prop="gender">
          <el-select v-model="form.gender" :placeholder="$t('system.ageThreshold.genderPlaceholder')" :disabled="true" >
            <el-option :label="$t('model.genderMale')" :value="1" />
            <el-option :label="$t('model.genderFemale')" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('system.ageThreshold.fieldName')" prop="fieldName">
          <el-input v-model="form.fieldName" :placeholder="$t('system.ageThreshold.fieldNamePlaceholder')" :disabled="true" />
        </el-form-item>
        <el-form-item :label="$t('system.ageThreshold.fieldNameEn')" prop="fieldNameEn">
          <el-input v-model="form.fieldNameEn" :placeholder="$t('system.ageThreshold.fieldNameEnPlaceholder')" :disabled="true" />
        </el-form-item>
        <el-form-item :label="$t('system.ageThreshold.minVal')" prop="minVal">
          <el-input v-model="form.minVal" :placeholder="$t('system.ageThreshold.minValPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('system.ageThreshold.maxVal')" prop="maxVal">
          <el-input v-model="form.maxVal" :placeholder="$t('system.ageThreshold.maxValPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ $t('common.confirm') }}</el-button>
          <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>
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

<script setup name="AgeThreshold">
import { listAgeThreshold, getAgeThreshold, delAgeThreshold, addAgeThreshold, updateAgeThreshold } from "@/api/healthReport/ageThreshold"
import {useI18n} from 'vue-i18n'
const {locale, t: $t} = useI18n()
// 新增：上传loading状态
const uploadLoading = ref(false)
const { proxy } = getCurrentInstance()
const { sys_user_sex } = proxy.useDict('sys_user_sex')

const ageThresholdList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    age: null,
    gender: null,
    fieldCode: null,
    minVal: null,
    maxVal: null,
  },
  rules: {
    age: [
      { required: true, message: () => $t('validation.required'), trigger: "blur" }
    ],
    gender: [
      { required: true, message: () => $t('validation.required'), trigger: "change" }
    ],
  }
})

import {getToken} from "@/utils/auth"

/*** 年龄指标阈值导入参数 */
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
    const api = "/healthReport/ageThreshold/importData"
    const timeStop = localStorage.getItem("timeStop") || ""
    const lang = locale.value === 'en-US' ? 'en' : 'zh'
    // 拼接 timeStop
    return baseUrl + api + "?timeStop=" + encodeURIComponent(timeStop)+ "&lang=" + lang
  })()
})

const { queryParams, form, rules } = toRefs(data)

/** 查询指标年龄阈值列表 */
function getList() {
  loading.value = true
  listAgeThreshold(queryParams.value).then(response => {
    ageThresholdList.value = response.rows
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
    age: null,
    gender: null,
    fieldCode: null,
    minVal: null,
    maxVal: null,
    createTime: null,
    updateTime: null
  }
  proxy.resetForm("ageThresholdRef")
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
  title.value = $t('common.add')
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getAgeThreshold(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = $t('common.edit')
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["ageThresholdRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateAgeThreshold(form.value).then(response => {
          proxy.$modal.msgSuccess($t('common.success'))
          open.value = false
          getList()
        })
      } else {
        addAgeThreshold(form.value).then(response => {
          proxy.$modal.msgSuccess($t('common.success'))
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
  proxy.$modal.confirm($t('system.ageThreshold.confirmDelete', { ids: _ids })).then(function() {
    return delAgeThreshold(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess($t('common.success'))
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  const languageType = locale.value === 'en-US' ? 'en' : 'zh'
  proxy.download('healthReport/ageThreshold/export', {
    ...queryParams.value,
    languageType
  }, `ageThreshold_${new Date().getTime()}.xlsx`, { timeout: 300000 })
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
  // 上传失败也关闭loading
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
