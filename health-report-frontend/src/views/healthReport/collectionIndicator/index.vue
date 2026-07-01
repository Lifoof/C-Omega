<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="指标编码" prop="indicatorCode">
        <el-input
            v-model="queryParams.indicatorCode"
            placeholder="请输入指标编码"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="指标名称" prop="indicatorName">
        <el-input
            v-model="queryParams.indicatorName"
            placeholder="请输入指标名称"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="所属器官系统" prop="organSystem">
        <el-select v-model="queryParams.organSystem" placeholder="请选择所属器官系统" clearable>
          <el-option
              v-for="dict in collection_organ_system"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
          ></el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="数据类型" prop="dataType">
        <el-select v-model="queryParams.dataType" placeholder="请选择数据类型" clearable>
          <el-option
              v-for="dict in 	indicator_data_type"
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
            v-hasPermi="['business:collectionIndicator:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['business:collectionIndicator:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['business:collectionIndicator:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="Download"
            @click="handleExport"
            v-hasPermi="['business:collectionIndicator:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="collectionIndicatorList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="指标ID" align="center" prop="id" />
      <el-table-column label="指标编码" align="center" prop="indicatorCode" />
      <el-table-column label="指标名称" align="center" prop="indicatorName" />
      <el-table-column label="所属器官系统" align="center" prop="organSystem" >
        <template #default="scope">
          <dict-tag :options="collection_organ_system" :value="scope.row.organSystem"/>
        </template>
      </el-table-column>
      <el-table-column label="数据类型" align="center" prop="dataType" >
        <template #default="scope">
          <dict-tag :options="indicator_data_type" :value="scope.row.dataType"/>
        </template>
      </el-table-column>
      <el-table-column label="单位" align="center" prop="unit" />
      <el-table-column label="排序号" align="center" prop="sortOrder" />
      <el-table-column label="指标说明" align="center" prop="description" />
      <el-table-column label="备注" align="center" prop="remarks" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['business:collectionIndicator:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['business:collectionIndicator:remove']">删除</el-button>
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

    <!-- 添加或修改体检指标定义对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="collectionIndicatorRef" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="指标编码" prop="indicatorCode">
              <el-input v-model="form.indicatorCode" placeholder="请输入指标编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="指标名称" prop="indicatorName">
              <el-input v-model="form.indicatorName" placeholder="请输入指标名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="所属器官系统" prop="organSystem">
              <el-select v-model="form.organSystem" placeholder="请选择所属器官系统" style="width: 100%">
                <el-option
                    v-for="dict in collection_organ_system"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据类型" prop="dataType">
              <el-select v-model="form.dataType" placeholder="请选择数据类型" style="width: 100%">
                <el-option
                    v-for="dict in indicator_data_type"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="请输入单位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序号" prop="sortOrder">
              <el-input v-model="form.sortOrder" placeholder="请输入排序号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="指标说明" prop="description">
              <el-input v-model="form.description" type="textarea" placeholder="请输入指标说明" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注" prop="remarks">
              <el-input v-model="form.remarks" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
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

<script setup name="CollectionIndicator">
import { listCollectionIndicator, getCollectionIndicator, delCollectionIndicator, addCollectionIndicator, updateCollectionIndicator } from "@/api/healthReport/collectionIndicator"

const { proxy } = getCurrentInstance()
const { sys_yes_no, collection_organ_system,indicator_data_type } = proxy.useDict('sys_yes_no', 'collection_organ_system','indicator_data_type')

const collectionIndicatorList = ref([])
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
    indicatorName: null,
    organSystem: null,
  },
  rules: {
    indicatorCode: [
      { required: true, message: "指标编码不能为空", trigger: "blur" }
    ],
    indicatorName: [
      { required: true, message: "指标名称不能为空", trigger: "blur" }
    ],
    organSystem: [
      { required: true, message: "所属器官系统不能为空", trigger: "change" }
    ],
    dataType: [
      { required: true, message: "数据类型不能为空", trigger: "change" }
    ],
    isRequired: [
      { required: true, message: "是否必填不能为空", trigger: "change" }
    ],
    isKeyFeature: [
      { required: true, message: "是否核心特征不能为空", trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询体检指标定义列表 */
function getList() {
  loading.value = true
  listCollectionIndicator(queryParams.value).then(response => {
    collectionIndicatorList.value = response.rows
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
    indicatorName: null,
    organSystem: null,
    dataType: null,
    unit: null,
    referenceMin: null,
    referenceMax: null,
    referenceText: null,
    isRequired: null,
    isKeyFeature: null,
    applicableModels: null,
    sortOrder: null,
    description: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remarks: null,
    delFlag: null
  }
  proxy.resetForm("collectionIndicatorRef")
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
  title.value = "添加体检指标定义"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getCollectionIndicator(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改体检指标定义"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["collectionIndicatorRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateCollectionIndicator(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addCollectionIndicator(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除体检指标定义编号为"' + _ids + '"的数据项？').then(function() {
    return delCollectionIndicator(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('business/collectionIndicator/export', {
    ...queryParams.value
  }, `collectionIndicator_${new Date().getTime()}.xlsx`)
}

getList()
</script>
