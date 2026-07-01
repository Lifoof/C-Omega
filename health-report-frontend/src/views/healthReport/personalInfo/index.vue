<template>
  <div class="page-container">
    <div class="page-header">
      <h3>{{ $t('personalInfo.title') }}</h3>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>{{ $t('common.create') }}
      </el-button>
    </div>

    <div class="search-bar">
      <el-input
          v-model="queryParams.name"
          :placeholder="$t('personalInfo.name')"
          clearable
          class="search-input"
          @keyup.enter="handleQuery"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="queryParams.gender" clearable :placeholder="$t('personalInfo.gender')" class="search-select">
        <el-option :label="$t('personalInfo.male')" :value="1" />
        <el-option :label="$t('personalInfo.female')" :value="2" />
      </el-select>
      <el-select v-model="queryParams.relationship" clearable :placeholder="$t('personalInfo.relationship')" class="search-select">
        <el-option :label="$t('personalInfo.relationSelf')" :value="1" />
        <el-option :label="$t('personalInfo.relationFamily')" :value="2" />
        <el-option :label="$t('personalInfo.relationFriend')" :value="3" />
      </el-select>
      <el-button type="primary" @click="handleQuery"><el-icon><Search /></el-icon>{{ $t('common.search') }}</el-button>
      <el-button @click="resetQuery"><el-icon><RefreshRight /></el-icon>{{ $t('common.reset') }}</el-button>
    </div>

    <div class="table-wrapper">
      <el-table :data="memberInfoList" v-loading="loading" stripe>
        <el-table-column prop="memberNo" :label="$t('personalInfo.memberNo')" width="160" />
        <el-table-column prop="name" :label="$t('personalInfo.name')" width="120" />
        <el-table-column :label="$t('personalInfo.relationship')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.relationship === 1 ? '' : row.relationship === 2 ? 'success' : 'warning'" size="small">
              {{ row.relationship === 1 ? $t('personalInfo.relationSelf') : row.relationship === 2 ? $t('personalInfo.relationFamily') : $t('personalInfo.relationFriend') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('personalInfo.gender')" width="80">
          <template #default="{ row }">
            {{ row.gender === 1 ? $t('personalInfo.male') : row.gender === 2 ? $t('personalInfo.female') : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="birthDate" :label="$t('personalInfo.birthDate')" width="120" />
        <el-table-column prop="contact" :label="$t('personalInfo.contact')" width="140" />
        <el-table-column prop="createTime" :label="$t('personalInfo.uploadTime')" width="180" />
        <el-table-column :label="$t('common.operation')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleUpdate(row)">{{ $t('common.edit') }}</el-button>
            <el-popconfirm :title="$t('common.deleteConfirm')" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">{{ $t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="queryParams.pageNum"
            :page-size="queryParams.pageSize"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="getList"
        />
      </div>
    </div>

    <!-- 添加或修改个人档案对话框 -->
    <el-dialog :title="title" v-model="open" width="640px" append-to-body>
      <el-form ref="memberInfoRef" :model="form" :rules="rules" label-width="120px" style="max-width: 640px;">
        <el-form-item v-if="form.id" :label="$t('personalInfo.memberNo')">
          <el-input v-model="form.memberNo" disabled />
        </el-form-item>
        <el-form-item :label="$t('personalInfo.name')" prop="name">
          <el-input v-model="form.name" :placeholder="$t('personalInfo.name')" />
        </el-form-item>
        <el-form-item :label="$t('personalInfo.relationship')" prop="relationship">
          <el-select v-model="form.relationship" style="width: 100%">
            <el-option :label="$t('personalInfo.relationSelf')" :value="1" />
            <el-option :label="$t('personalInfo.relationFamily')" :value="2" />
            <el-option :label="$t('personalInfo.relationFriend')" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('personalInfo.gender')" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">{{ $t('personalInfo.male') }}</el-radio>
            <el-radio :value="2">{{ $t('personalInfo.female') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="$t('personalInfo.birthDate')">
          <el-date-picker
              v-model="form.birthDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              :placeholder="$t('personalInfo.birthDate')"
          />
        </el-form-item>

        <!-- 国家/城市/地区 级联联动 -->
        <el-form-item :label="$t('personalInfo.country')">
          <el-select v-model="form.country" filterable style="width: 100%" @change="onCountryChange" :placeholder="$t('personalInfo.country')">
            <el-option v-for="c in countryList" :key="c.code" :label="c.label" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('personalInfo.city')">
          <el-select v-model="form.city" filterable style="width: 100%" @change="onCityChange" :placeholder="$t('personalInfo.city')" :disabled="!cityList.length">
            <el-option v-for="c in cityList" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('personalInfo.district')">
          <el-select v-model="form.district" filterable allow-create style="width: 100%" :placeholder="$t('personalInfo.district')" :disabled="!districtList.length">
            <el-option v-for="d in districtList" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>

        <el-form-item :label="$t('personalInfo.contact')">
          <el-input v-model="form.contact" :placeholder="$t('personalInfo.contact')" />
        </el-form-item>
        <el-form-item :label="$t('personalInfo.address')">
          <el-input v-model="form.address" :placeholder="$t('personalInfo.address')" />
        </el-form-item>
        <el-form-item :label="$t('personalInfo.ethnicity')">
          <el-input v-model="form.ethnicity" :placeholder="$t('personalInfo.ethnicity')" />
        </el-form-item>
        <el-form-item :label="$t('personalInfo.birthplace')">
          <el-input v-model="form.birthplace" :placeholder="$t('personalInfo.birthplace')" />
        </el-form-item>
        <el-form-item :label="$t('personalInfo.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="$t('personalInfo.remark')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ $t('common.save') }}</el-button>
          <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MemberInfo">
import { ref, reactive, computed } from 'vue'
import { Search, RefreshRight, Plus } from '@element-plus/icons-vue'
import { listMemberInfo, getMemberInfo, delMemberInfo, addMemberInfo, updateMemberInfo } from "@/api/healthReport/personalInfo"

const { proxy } = getCurrentInstance()

const memberInfoList = ref([])
const open = ref(false)
const loading = ref(true)
const total = ref(0)
const title = ref("")
const memberInfoRef = ref()

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    relationship: null,
    gender: null,
  },
  rules: {
    name: [
      { required: true, message: () => proxy.$t('personalInfo.nameRequired'), trigger: "blur" }
    ],
    relationship: [
      { required: true, message: () => proxy.$t('personalInfo.relationshipRequired'), trigger: "change" }
    ],
    gender: [
      { required: true, message: () => proxy.$t('personalInfo.genderRequired'), trigger: "change" }
    ],
  }
})

const { queryParams, form, rules } = toRefs(data)

// ===== 国家/城市/地区 级联数据 =====
const countryList = [
  { code: 'CN', label: '中国 China' },
  { code: 'US', label: '美国 United States' },
  { code: 'JP', label: '日本 Japan' },
  { code: 'KR', label: '韩国 South Korea' },
  { code: 'GB', label: '英国 United Kingdom' },
  { code: 'DE', label: '德国 Germany' },
  { code: 'FR', label: '法国 France' },
  { code: 'AU', label: '澳大利亚 Australia' },
  { code: 'CA', label: '加拿大 Canada' },
  { code: 'SG', label: '新加坡 Singapore' },
]

const regionData = {
  CN: {
    '北京': ['东城区','西城区','朝阳区','海淀区','丰台区','通州区','顺义区','大兴区','昌平区','房山区'],
    '上海': ['浦东新区','黄浦区','徐汇区','静安区','长宁区','虹口区','杨浦区','闵行区','宝山区','嘉定区'],
    '广州': ['天河区','越秀区','荔湾区','海珠区','白云区','番禺区','花都区','南沙区','增城区','黄埔区'],
    '深圳': ['福田区','罗湖区','南山区','宝安区','龙岗区','龙华区','坪山区','光明区','盐田区'],
    '杭州': ['上城区','拱墅区','西湖区','滨江区','萧山区','余杭区','临平区','钱塘区','富阳区','临安区'],
    '成都': ['锦江区','青羊区','金牛区','武侯区','成华区','龙泉驿区','新都区','温江区','双流区','郫都区'],
    '南京': ['玄武区','秦淮区','建邺区','鼓楼区','浦口区','栖霞区','雨花台区','江宁区','六合区'],
    '武汉': ['江岸区','江汉区','硚口区','汉阳区','武昌区','青山区','洪山区','东西湖区','蔡甸区'],
    '重庆': ['渝中区','大渡口区','江北区','沙坪坝区','九龙坡区','南岸区','北碚区','渝北区','巴南区'],
    '天津': ['和平区','河东区','河西区','南开区','河北区','红桥区','东丽区','西青区','津南区','北辰区'],
    '西安': ['碑林区','新城区','莲湖区','雁塔区','未央区','灞桥区','长安区','高新区'],
    '苏州': ['姑苏区','虎丘区','吴中区','相城区','吴江区','昆山市','太仓市','常熟市','张家港市'],
  },
  US: {
    'New York': ['Manhattan','Brooklyn','Queens','Bronx','Staten Island'],
    'Los Angeles': ['Downtown','Hollywood','Beverly Hills','Santa Monica','Pasadena'],
    'Chicago': ['Loop','Lincoln Park','Wicker Park','Hyde Park'],
    'San Francisco': ['Financial District','Mission','SoMa','Marina'],
    'Houston': ['Downtown','Midtown','Galleria','Memorial'],
  },
  JP: {
    '東京': ['千代田区','中央区','港区','新宿区','渋谷区','豊島区','文京区','品川区','目黒区','世田谷区'],
    '大阪': ['北区','中央区','天王寺区','浪速区','淀川区'],
    '名古屋': ['中区','東区','千種区','名東区','昭和区'],
    '京都': ['上京区','中京区','下京区','東山区','左京区'],
  },
  KR: {
    '서울': ['강남구','서초구','송파구','강동구','마포구','용산구','종로구'],
    '부산': ['해운대구','수영구','남구','중구','사하구'],
  },
  GB: {
    'London': ['Westminster','Camden','Islington','Hackney','Greenwich'],
    'Manchester': ['City Centre','Salford','Trafford','Stockport'],
    'Birmingham': ['City Centre','Edgbaston','Solihull'],
  },
}

const cityList = computed(() => {
  if (!form.value.country || !regionData[form.value.country]) return []
  return Object.keys(regionData[form.value.country])
})

const districtList = computed(() => {
  if (!form.value.country || !form.value.city || !regionData[form.value.country]) return []
  return regionData[form.value.country][form.value.city] || []
})

function onCountryChange() {
  form.value.city = ''
  form.value.district = ''
}

function onCityChange() {
  form.value.district = ''
}

/** 查询个人档案列表 */
function getList() {
  loading.value = true
  listMemberInfo(queryParams.value).then(response => {
    memberInfoList.value = response.rows
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
    userId: null,
    memberNo: null,
    name: null,
    relationship: 1,
    gender: 1,
    birthDate: null,
    country: 'CN',
    city: null,
    district: null,
    contact: null,
    address: null,
    ethnicity: null,
    birthplace: null,
    remark: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    deleted: null
  }
  proxy.resetForm("memberInfoRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  queryParams.value.name = null
  queryParams.value.relationship = null
  queryParams.value.gender = null
  handleQuery()
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = proxy.$t('personalInfo.createTitle')
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const _id = row.id
  getMemberInfo(_id).then(response => {
    form.value = response.data
    open.value = true
    title.value = proxy.$t('personalInfo.editTitle')
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["memberInfoRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateMemberInfo(form.value).then(response => {
          proxy.$modal.msgSuccess(proxy.$t('common.success'))
          open.value = false
          getList()
        })
      } else {
        addMemberInfo(form.value).then(response => {
          proxy.$modal.msgSuccess(proxy.$t('common.success'))
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id
  proxy.$modal.confirm(proxy.$t('common.deleteConfirm')).then(function() {
    return delMemberInfo(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess(proxy.$t('common.success'))
  }).catch(() => {})
}

getList()
</script>

<style lang="scss" scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 500;
    color: #303133;
  }
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 20px;
  background: #fff;
  border-radius: 4px;

  .search-input {
    width: 200px;
  }

  .search-select {
    width: 140px;
  }
}

.table-wrapper {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

:deep(.el-dialog__body) {
  padding: 20px 20px 10px;
}
</style>
