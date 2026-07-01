<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="报告编号" prop="reportNo">
        <el-input
          v-model="queryParams.reportNo"
          placeholder="请输入报告编号"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
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
      <el-form-item label="器官系统" prop="organSystem">
        <el-select v-model="queryParams.organSystem" placeholder="请选择器官系统" clearable>
          <el-option
            v-for="dict in collection_organ_system"
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
          v-hasPermi="['healthReport:aiPredictionReport:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['healthReport:aiPredictionReport:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['healthReport:aiPredictionReport:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['healthReport:aiPredictionReport:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="aiPredictionReportList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="报告ID" align="center" prop="id" />
      <el-table-column label="报告编号" align="center" prop="reportNo" />
      <el-table-column label="采集主表ID" align="center" prop="collectionId" />
      <el-table-column label="会员ID" align="center" prop="memberId" />
      <el-table-column label="模型编码" align="center" prop="modelCode" />
      <el-table-column label="模型名称" align="center" prop="modelName" />
      <el-table-column label="性别" align="center" prop="gender">
        <template #default="scope">
          <dict-tag :options="sys_user_sex" :value="scope.row.gender"/>
        </template>
      </el-table-column>
      <el-table-column label="器官系统" align="center" prop="organSystem">
        <template #default="scope">
          <dict-tag :options="collection_organ_system" :value="scope.row.organSystem"/>
        </template>
      </el-table-column>
      <el-table-column label="输入数据" align="center" prop="inputData" />
      <el-table-column label="预测结果" align="center" prop="predictionResult" />
      <el-table-column label="风险等级" align="center" prop="riskLevel" />
      <el-table-column label="风险评分" align="center" prop="riskScore" />
      <el-table-column label="健康建议" align="center" prop="healthAdvice" />
      <el-table-column label="关键发现" align="center" prop="keyFindings" />
      <el-table-column label="数据完整率(%)" align="center" prop="completionRate" />
      <el-table-column label="缺失的核心特征列表" align="center" prop="missingKeyFeatures" />
      <el-table-column label="调用模型时间" align="center" prop="callTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.callTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="模型返回时间" align="center" prop="responseTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.responseTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" />
      <el-table-column label="错误信息" align="center" prop="errorMsg" />
      <el-table-column label="备注" align="center" prop="remarks" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['healthReport:aiPredictionReport:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['healthReport:aiPredictionReport:remove']">删除</el-button>
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

    <!-- 添加或修改报告列表对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="aiPredictionReportRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="报告编号" prop="reportNo">
          <el-input v-model="form.reportNo" placeholder="请输入报告编号" />
        </el-form-item>
        <el-form-item label="采集主表ID" prop="collectionId">
          <el-input v-model="form.collectionId" placeholder="请输入采集主表ID" />
        </el-form-item>
        <el-form-item label="会员ID" prop="memberId">
          <el-input v-model="form.memberId" placeholder="请输入会员ID" />
        </el-form-item>
        <el-form-item label="模型编码" prop="modelCode">
          <el-input v-model="form.modelCode" placeholder="请输入模型编码" />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="请输入模型名称" />
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
        <el-form-item label="器官系统" prop="organSystem">
          <el-select v-model="form.organSystem" placeholder="请选择器官系统">
            <el-option
              v-for="dict in collection_organ_system"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="输入数据" prop="inputData">
          <el-input v-model="form.inputData" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="预测结果" prop="predictionResult">
          <el-input v-model="form.predictionResult" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="风险等级" prop="riskLevel">
          <el-input v-model="form.riskLevel" placeholder="请输入风险等级" />
        </el-form-item>
        <el-form-item label="风险评分" prop="riskScore">
          <el-input v-model="form.riskScore" placeholder="请输入风险评分" />
        </el-form-item>
        <el-form-item label="健康建议" prop="healthAdvice">
          <el-input v-model="form.healthAdvice" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="关键发现" prop="keyFindings">
          <el-input v-model="form.keyFindings" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="数据完整率(%)" prop="completionRate">
          <el-input v-model="form.completionRate" placeholder="请输入数据完整率(%)" />
        </el-form-item>
        <el-form-item label="缺失的核心特征列表" prop="missingKeyFeatures">
          <el-input v-model="form.missingKeyFeatures" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="调用模型时间" prop="callTime">
          <el-date-picker clearable
            v-model="form.callTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择调用模型时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="模型返回时间" prop="responseTime">
          <el-date-picker clearable
            v-model="form.responseTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择模型返回时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="错误信息" prop="errorMsg">
          <el-input v-model="form.errorMsg" type="textarea" placeholder="请输入内容" />
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

<script setup name="AiPredictionReport">
import { listAiPredictionReport, getAiPredictionReport, delAiPredictionReport, addAiPredictionReport, updateAiPredictionReport } from "@/api/healthReport/aiPredictionReport"

const { proxy } = getCurrentInstance()
const { sys_user_sex, collection_organ_system } = proxy.useDict('sys_user_sex', 'collection_organ_system')

const aiPredictionReportList = ref([])
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
    reportNo: null,
    modelCode: null,
    modelName: null,
    gender: null,
    organSystem: null,
  },
  rules: {
    reportNo: [
      { required: true, message: "报告编号不能为空", trigger: "blur" }
    ],
    collectionId: [
      { required: true, message: "采集主表ID不能为空", trigger: "blur" }
    ],
    memberId: [
      { required: true, message: "会员ID不能为空", trigger: "blur" }
    ],
    modelCode: [
      { required: true, message: "模型编码不能为空", trigger: "blur" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询报告列表列表 */
function getList() {
  loading.value = true
  listAiPredictionReport(queryParams.value).then(response => {
    aiPredictionReportList.value = response.rows
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
    reportNo: null,
    collectionId: null,
    memberId: null,
    modelCode: null,
    modelName: null,
    gender: null,
    organSystem: null,
    inputData: null,
    predictionResult: null,
    riskLevel: null,
    riskScore: null,
    healthAdvice: null,
    keyFindings: null,
    completionRate: null,
    missingKeyFeatures: null,
    callTime: null,
    responseTime: null,
    status: null,
    errorMsg: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remarks: null,
    delFlag: null
  }
  proxy.resetForm("aiPredictionReportRef")
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
  title.value = "添加报告列表"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id || ids.value
  getAiPredictionReport(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改报告列表"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["aiPredictionReportRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateAiPredictionReport(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addAiPredictionReport(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除报告列表编号为"' + _ids + '"的数据项？').then(function() {
    return delAiPredictionReport(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('healthReport/aiPredictionReport/export', {
    ...queryParams.value
  }, `aiPredictionReport_${new Date().getTime()}.xlsx`)
}

getList()
</script>
