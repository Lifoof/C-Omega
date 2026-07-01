<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="模型编码" prop="modelCode">
        <el-input
          v-model="queryParams.modelCode"
          placeholder="请输入模型编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="模型名称" prop="modelName">
        <el-input
          v-model="queryParams.modelName"
          placeholder="请输入模型名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="适用性别" prop="gender">
        <el-select v-model="queryParams.gender" placeholder="请选择适用性别" clearable>
          <el-option
              v-for="dict in sys_user_sex"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="器官系统" prop="organSystem">
        <el-select v-model="queryParams.organSystem" placeholder="请选择器官系统" clearable>
          <el-option
              v-for="dict in collection_organ_system"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          ></el-option>
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
          v-hasPermi="['business:aiModel:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['business:aiModel:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['business:aiModel:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['business:aiModel:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="aiModelList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="模型ID" align="center" prop="id" />
      <el-table-column label="模型编码" align="center" prop="modelCode" />
      <el-table-column label="模型名称" align="center" prop="modelName" />
      <el-table-column label="适用性别" align="center" prop="gender">
        <template #default="scope">
          <dict-tag :options="sys_user_sex" :value="scope.row.gender"/>
        </template>
      </el-table-column>
      <el-table-column label="器官系统" align="center" prop="organSystem" >
        <template #default="scope">
          <dict-tag :options="collection_organ_system" :value="scope.row.organSystem"/>
        </template>
      </el-table-column>
      <el-table-column label="Python模块路径" align="center" prop="pythonModule" />
      <el-table-column label="模型API端点" align="center" prop="apiEndpoint" />
      <el-table-column label="最小完成率(%)" align="center" prop="minCompletionRate" />
      <el-table-column label="核心指标列表" align="center" prop="keyIndicators" />
      <el-table-column label="模型描述" align="center" prop="description" />
      <el-table-column label="模型版本" align="center" prop="version" />
      <el-table-column label="备注" align="center" prop="remarks" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['business:aiModel:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['business:aiModel:remove']">删除</el-button>
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

    <!-- 添加或修改AI模型定义对话框 -->
    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="aiModelRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="模型编码" prop="modelCode">
          <el-input v-model="form.modelCode" placeholder="请输入模型编码" />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="适用性别" prop="gender">
          <el-select v-model="form.gender" placeholder="请选择适用性别" style="width: 100%">
            <el-option
                v-for="dict in sys_user_sex"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="器官系统" prop="organSystem">
          <el-select v-model="form.organSystem" placeholder="请选择器官系统" style="width: 100%">
            <el-option
                v-for="dict in collection_organ_system"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="Python模块路径" prop="pythonModule">
          <el-input v-model="form.pythonModule" placeholder="请输入Python模块路径" />
        </el-form-item>
        <el-form-item label="模型API端点" prop="apiEndpoint">
          <el-input v-model="form.apiEndpoint" placeholder="请输入模型API端点" />
        </el-form-item>
        <el-form-item label="最小完成率(%)" prop="minCompletionRate">
          <el-input-number v-model="form.minCompletionRate" :precision="2" :step="0.1" :max="100" :min="0" placeholder="请输入最小完成率(%)" style="width: 100%" />
        </el-form-item>
        <el-form-item label="核心指标列表" prop="keyIndicators">
          <el-input v-model="form.keyIndicators" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="模型描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="模型版本" prop="version">
          <el-input v-model="form.version" placeholder="请输入模型版本" />
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

<script setup name="AiModel">
import { listAiModel, getAiModel, delAiModel, addAiModel, updateAiModel } from "@/api/healthReport/aiModel"

const { proxy } = getCurrentInstance()
const { sys_user_sex } = proxy.useDict('sys_user_sex')
const { collection_organ_system } = proxy.useDict('collection_organ_system')

const aiModelList = ref([])
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
    modelCode: null,
    modelName: null,
    gender: null,
    organSystem: null,
  },
  rules: {
    modelCode: [
      { required: true, message: "模型编码不能为空", trigger: "blur" }
    ],
    modelName: [
      { required: true, message: "模型名称不能为空", trigger: "blur" }
    ],
    gender: [
      { required: true, message: "适用性别不能为空", trigger: "blur" }
    ],
    organSystem: [
      { required: true, message: "器官系统不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询AI模型定义列表 */
function getList() {
  loading.value = true
  listAiModel(queryParams.value).then(response => {
    aiModelList.value = response.rows
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
    modelCode: null,
    modelName: null,
    gender: null,
    organSystem: null,
    pythonModule: null,
    apiEndpoint: null,
    minCompletionRate: null,
    keyIndicators: null,
    description: null,
    status: null,
    version: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remarks: null,
    delFlag: null
  }
  proxy.resetForm("aiModelRef")
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
  title.value = "添加AI模型定义"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getAiModel(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改AI模型定义"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["aiModelRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateAiModel(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAiModel(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除AI模型定义编号为"' + _ids + '"的数据项？').then(function() {
    return delAiModel(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('business/aiModel/export', {
    ...queryParams.value
  }, `aiModel_${new Date().getTime()}.xlsx`)
}

getList()
</script>
