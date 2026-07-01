<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="指标编码" prop="indicatorCode">
        <el-input
          v-model="queryParams.indicatorCode"
          placeholder="请输入指标编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="性别" prop="gender">
        <el-select v-model="queryParams.gender" placeholder="请选择性别" clearable>
          <el-option
            v-for="dict in sys_user_sex"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['healthReport:indicatorGenderConfig:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['healthReport:indicatorGenderConfig:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['healthReport:indicatorGenderConfig:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['healthReport:indicatorGenderConfig:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="indicatorGenderConfigList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" />
      <el-table-column label="指标编码" align="center" prop="indicatorCode" />
      <el-table-column label="指标名称" align="center" prop="indicatorName" />
      <el-table-column label="性别" align="center" prop="gender">
        <template #default="scope">
          <dict-tag :options="sys_user_sex" :value="scope.row.gender"/>
        </template>
      </el-table-column>
      <el-table-column label="参考范围最小值" align="center" prop="referenceMin" />
      <el-table-column label="参考范围最大值" align="center" prop="referenceMax" />
      <el-table-column label="参考范围文本" align="center" prop="referenceText" />
      <el-table-column label="备注" align="center" prop="remarks" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['healthReport:indicatorGenderConfig:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['healthReport:indicatorGenderConfig:remove']">删除</el-button>
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

    <!-- 添加或修改指标性别配置对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="indicatorGenderConfigRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="指标编码" prop="indicatorCode">
          <el-input v-model="form.indicatorCode" placeholder="请输入指标编码" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" placeholder="请选择性别">
            <el-option
              v-for="dict in sys_user_sex"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="参考范围最小值" prop="referenceMin">
          <el-input v-model="form.referenceMin" placeholder="请输入参考范围最小值" />
        </el-form-item>
        <el-form-item label="参考范围最大值" prop="referenceMax">
          <el-input v-model="form.referenceMax" placeholder="请输入参考范围最大值" />
        </el-form-item>
        <el-form-item label="参考范围文本" prop="referenceText">
          <el-input v-model="form.referenceText" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="备注" prop="remarks">
          <el-input v-model="form.remarks" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="IndicatorGenderConfig">
import { listIndicatorGenderConfig, getIndicatorGenderConfig, delIndicatorGenderConfig, addIndicatorGenderConfig, updateIndicatorGenderConfig } from "@/api/healthReport/indicatorGenderConfig"

const { proxy } = getCurrentInstance()
const { sys_user_sex } = proxy.useDict('sys_user_sex')

const indicatorGenderConfigList = ref([])
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
    indicatorCode: null,
    gender: null,
    isKeyFeature: null,
    isRequired: null,
  },
  rules: {
    indicatorCode: [
      { required: true, message: "指标编码不能为空", trigger: "blur" }
    ],
    gender: [
      { required: true, message: "性别不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询指标性别配置列表 */
function getList() {
  loading.value = true
  listIndicatorGenderConfig(queryParams.value).then(response => {
    indicatorGenderConfigList.value = response.rows
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
    indicatorCode: null,
    gender: null,
    isKeyFeature: null,
    isRequired: null,
    referenceMin: null,
    referenceMax: null,
    referenceText: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remarks: null
  }
  proxy.resetForm("indicatorGenderConfigRef")
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
  title.value = "添加指标性别配置"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getIndicatorGenderConfig(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改指标性别配置"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["indicatorGenderConfigRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateIndicatorGenderConfig(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addIndicatorGenderConfig(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
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
  proxy.$modal.confirm('是否确认删除指标性别配置编号为"' + _ids + '"的数据项？').then(function() {
    return delIndicatorGenderConfig(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('healthReport/indicatorGenderConfig/export', {
    ...queryParams.value
  }, `indicatorGenderConfig_${new Date().getTime()}.xlsx`)
}

getList()
</script>
