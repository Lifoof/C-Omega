<template>
  <div class="page-container">
    <!-- 列表视图 -->
    <template v-if="!showForm">
      <div class="page-header">
        <h3>{{ $t('personalInfo.title') }}</h3>
        <div class="header-buttons">
          <el-button type="primary"  @click="handleImport">
            <el-icon><Upload /></el-icon>{{ $t('common.import') }}
          </el-button>
          <el-button type="primary"  @click="handleExport">
            <el-icon><Download /></el-icon>{{ $t('common.export') }}
          </el-button>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>{{ $t('common.create') }}
          </el-button>
        </div>
      </div>

      <div class="search-bar">
        <el-input
            v-model="query.name"
            :placeholder="$t('personalInfo.name')"
            clearable
            class="search-input"
            @keyup.enter="fetchData"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.gender" clearable :placeholder="$t('personalInfo.gender')" class="search-select">
          <el-option :label="$t('personalInfo.male')" :value="1" />
          <el-option :label="$t('personalInfo.female')" :value="2" />
        </el-select>
        <el-select v-model="query.relationship" clearable :placeholder="$t('personalInfo.relationship')" class="search-select">
          <el-option :label="$t('personalInfo.relationSelf')" :value="1" />
          <el-option :label="$t('personalInfo.relationFamily')" :value="2" />
          <el-option :label="$t('personalInfo.relationFriend')" :value="3" />
        </el-select>
        <el-select v-model="query.country" clearable :placeholder="$t('personalInfo.country')" class="search-select">
          <el-option
              v-for="c in countryList"
              :key="c.code"
              :label="locale === 'en-US' ? c.labelEn : c.label"
              :value="c.code"
          />
        </el-select>
        <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>{{ $t('common.search') }}</el-button>
        <el-button @click="resetQuery"><el-icon><RefreshRight /></el-icon>{{ $t('common.reset') }}</el-button>
      </div>

      <div class="table-wrapper">
        <el-table :data="tableData" v-loading="loading" stripe style="width: 100%;">
          <el-table-column prop="memberNo" :label="$t('personalInfo.memberNo')" min-width="200" />
          <el-table-column prop="name" :label="$t('personalInfo.name')" min-width="120" />
          <el-table-column :label="$t('personalInfo.relationship')" min-width="100">
            <template #default="{ row }">
              <el-tag :type="row.relationship === 1 ? '' : row.relationship === 2 ? 'success' : 'warning'" size="small">
                {{ row.relationship === 1 ? $t('personalInfo.relationSelf') : row.relationship === 2 ? $t('personalInfo.relationFamily') : $t('personalInfo.relationFriend') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('personalInfo.gender')" min-width="120">
            <template #default="{ row }">
              {{ row.gender === 1 ? $t('personalInfo.male') : row.gender === 2 ? $t('personalInfo.female') : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="birthDate" :label="$t('personalInfo.birthDate')" min-width="140" >
            <template #default="{ row }">
              {{ parseTime(row.birthDate, '{y}-{m}-{d}') }}
            </template>
          </el-table-column>
          <el-table-column prop="contact" :label="$t('personalInfo.contact')" min-width="140" />
          <el-table-column :label="$t('personalInfo.country')" min-width="120">
            <template #default="{ row }">
              {{ getCountryLabel(row.country) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('personalInfo.city')" min-width="140">
            <template #default="{ row }">
              {{ getCityLabel(row.city) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('common.operation')" min-width="140" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEdit(row)">{{ $t('common.edit') }}</el-button>
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
      <div class="page-header">
        <h3>{{ isEdit ? $t('personalInfo.editTitle') : $t('personalInfo.createTitle') }}</h3>
        <el-button @click="handleBack">{{ $t('common.back') }}</el-button>
      </div>
      <div class="form-card">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 640px;">
          <el-form-item v-if="isEdit" :label="$t('personalInfo.memberNo')">
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
          <el-form-item :label="$t('personalInfo.birthDate')" prop="birthDate">
            <el-date-picker
                v-model="form.birthDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                :placeholder="$t('personalInfo.birthDate')"
            />
          </el-form-item>

          <!-- 国家/城市/地区 级联联动 -->
          <!-- 国家（支持下拉+手动输入） -->
          <el-form-item :label="$t('personalInfo.country')">
            <el-select
                v-model="form.country"
                filterable
                allow-create
                style="width: 100%"
                @change="onCountryChange"
                :placeholder="$t('personalInfo.country')"
                :default-first-option="false"
            >
              <el-option
                  v-for="c in countryList"
                  :key="c.code"
                  :label="locale === 'en-US' ? c.labelEn : c.label"
                  :value="c.code"
              />
            </el-select>
          </el-form-item>

          <!-- 城市（支持下拉+手动输入） -->
          <el-form-item :label="$t('personalInfo.city')">
            <el-select
                v-model="form.city"
                filterable
                allow-create
                style="width: 100%"
                @change="onCityChange"
                :placeholder="$t('personalInfo.city')"
                :disabled="!cityList.length && !form.country"
                :default-first-option="false"
            >
              <el-option
                  v-for="c in cityList"
                  :key="c"
                  :label="getCityLabel(c)"
                  :value="c"
              />
            </el-select>
          </el-form-item>

<!--          &lt;!&ndash; 区县（支持下拉+手动输入） &ndash;&gt;
          <el-form-item :label="$t('personalInfo.district')">
            <el-select
                v-model="form.district"
                filterable
                allow-create
                style="width: 100%"
                :placeholder="$t('personalInfo.district')"
                :disabled="!districtList.length && !form.city"
                :default-first-option="false"
            >
              <el-option
                  v-for="d in districtList"
                  :key="d"
                  :label="getDistrictLabel(d)"
                  :value="d"
              />
            </el-select>
          </el-form-item>-->

          <el-form-item :label="$t('personalInfo.contact')">
            <el-input v-model="form.contact" :placeholder="$t('personalInfo.contact')" />
          </el-form-item>
          <el-form-item :label="$t('personalInfo.address')">
            <el-input v-model="form.address" :placeholder="$t('personalInfo.address')" />
          </el-form-item>
          <el-form-item :label="$t('personalInfo.remark')">
            <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="$t('personalInfo.remark')" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSubmit" :loading="submitLoading">{{ $t('common.save') }}</el-button>
            <el-button @click="handleBack">{{ $t('common.cancel') }}</el-button>
          </el-form-item>
        </el-form>
      </div>
    </template>

    <!-- 导入对话框 -->
    <el-dialog :title="$t('common.importTitle')" v-model="importDialogVisible" width="400px" append-to-body>
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
        <div class="el-upload__text">{{ $t('common.dragFileHere') }}<em>{{ $t('common.clickUpload') }}</em></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <span>{{ $t('common.onlyXlsx') }}</span>
            <el-link type="primary" underline="never" style="font-size: 12px; vertical-align: baseline" @click="downloadTemplate">{{ $t('common.downloadTemplate') }}</el-link>
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

<script setup lang="ts" name="PersonalInfo">
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { Search, RefreshRight, Plus, Upload, Download, UploadFilled } from '@element-plus/icons-vue'
import { listMemberInfo, getMemberInfo, delMemberInfo, addMemberInfo, updateMemberInfo, type MemberInfo } from "@/api/healthReport/personalInfo"
import { getToken } from "@/utils/auth"
import { download } from "@/utils/request"

const { proxy } = getCurrentInstance()
const { t: $t,locale } = useI18n()
const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<MemberInfo[]>([])
const total = ref(0)
const query = reactive({ name: '', relationship: undefined as number | undefined, gender: undefined as number | undefined, country: undefined as string | undefined, pageNum: 1, pageSize: 10 })

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
    const api = "/healthReport/memberInfo/importData"
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
const formRef = ref<FormInstance>()
const form = reactive<Partial<MemberInfo>>({
  memberNo: '',
  name: '',
  relationship: 1,
  gender: 1,
  birthDate: '',
  country: '',
  city: '',
  district: '',
  contact: '',
  address: '',
  ethnicity: '',
  birthplace: '',
  remark: '',
})

const rules = {
  name: [{ required: true, message: $t('personalInfo.nameRequired'), trigger: 'blur' }],
  relationship: [{ required: true, message: $t('personalInfo.relationshipRequired'), trigger: 'change' }],
  gender: [{ required: true, message: $t('personalInfo.genderRequired'), trigger: 'change' }],
  birthDate: [{ required: true, message: $t('personalInfo.birthDateRequired'), trigger: 'change' }],
}

// ===== 国家/城市/地区 级联数据 =====
const countryList = [
  { code: 'CN', label: '中国', labelEn: 'China' },
  { code: 'US', label: '美国', labelEn: 'United States' },
  { code: 'JP', label: '日本', labelEn: 'Japan' },
  { code: 'KR', label: '韩国', labelEn: 'South Korea' },
  { code: 'GB', label: '英国', labelEn: 'United Kingdom' },
  { code: 'DE', label: '德国', labelEn: 'Germany' },
  { code: 'FR', label: '法国', labelEn: 'France' },
  { code: 'AU', label: '澳大利亚', labelEn: 'Australia' },
  { code: 'CA', label: '加拿大', labelEn: 'Canada' },
  { code: 'SG', label: '新加坡', labelEn: 'Singapore' },
]

// 城市翻译映射
const cityTranslations = {
  '北京': { zh: '北京', en: 'Beijing' },
  '上海': { zh: '上海', en: 'Shanghai' },
  '广州': { zh: '广州', en: 'Guangzhou' },
  '深圳': { zh: '深圳', en: 'Shenzhen' },
  '杭州': { zh: '杭州', en: 'Hangzhou' },
  '成都': { zh: '成都', en: 'Chengdu' },
  '南京': { zh: '南京', en: 'Nanjing' },
  '武汉': { zh: '武汉', en: 'Wuhan' },
  '重庆': { zh: '重庆', en: 'Chongqing' },
  '天津': { zh: '天津', en: 'Tianjin' },
  '西安': { zh: '西安', en: "Xi'an" },
  '苏州': { zh: '苏州', en: 'Suzhou' },
  'New York': { zh: '纽约', en: 'New York' },
  'Los Angeles': { zh: '洛杉矶', en: 'Los Angeles' },
  'Chicago': { zh: '芝加哥', en: 'Chicago' },
  'San Francisco': { zh: '旧金山', en: 'San Francisco' },
  'Houston': { zh: '休斯顿', en: 'Houston' },
  '東京': { zh: '东京', en: 'Tokyo' },
  '大阪': { zh: '大阪', en: 'Osaka' },
  '名古屋': { zh: '名古屋', en: 'Nagoya' },
  '京都': { zh: '京都', en: 'Kyoto' },
  '서울': { zh: '首尔', en: 'Seoul' },
  '부산': { zh: '釜山', en: 'Busan' },
  'London': { zh: '伦敦', en: 'London' },
  'Manchester': { zh: '曼彻斯特', en: 'Manchester' },
  'Birmingham': { zh: '伯明翰', en: 'Birmingham' },
}

// 区县翻译映射
const districtTranslations = {
  '东城区': { zh: '东城区', en: 'Dongcheng District' },
  '西城区': { zh: '西城区', en: 'Xicheng District' },
  '朝阳区': { zh: '朝阳区', en: 'Chaoyang District' },
  '海淀区': { zh: '海淀区', en: 'Haidian District' },
  '丰台区': { zh: '丰台区', en: 'Fengtai District' },
  '通州区': { zh: '通州区', en: 'Tongzhou District' },
  '顺义区': { zh: '顺义区', en: 'Shunyi District' },
  '大兴区': { zh: '大兴区', en: 'Daxing District' },
  '昌平区': { zh: '昌平区', en: 'Changping District' },
  '房山区': { zh: '房山区', en: 'Fangshan District' },
  '浦东新区': { zh: '浦东新区', en: 'Pudong New Area' },
  '黄浦区': { zh: '黄浦区', en: 'Huangpu District' },
  '徐汇区': { zh: '徐汇区', en: 'Xuhui District' },
  '静安区': { zh: '静安区', en: 'Jing\'an District' },
  '长宁区': { zh: '长宁区', en: 'Changning District' },
  '虹口区': { zh: '虹口区', en: 'Hongkou District' },
  '杨浦区': { zh: '杨浦区', en: 'Yangpu District' },
  '闵行区': { zh: '闵行区', en: 'Minhang District' },
  '宝山区': { zh: '宝山区', en: 'Baoshan District' },
  '嘉定区': { zh: '嘉定区', en: 'Jiading District' },
  '天河区': { zh: '天河区', en: 'Tianhe District' },
  '越秀区': { zh: '越秀区', en: 'Yuexiu District' },
  '荔湾区': { zh: '荔湾区', en: 'Liwan District' },
  '海珠区': { zh: '海珠区', en: 'Haizhu District' },
  '白云区': { zh: '白云区', en: 'Baiyun District' },
  '番禺区': { zh: '番禺区', en: 'Panyu District' },
  '花都区': { zh: '花都区', en: 'Huadu District' },
  '南沙区': { zh: '南沙区', en: 'Nansha District' },
  '增城区': { zh: '增城区', en: 'Zengcheng District' },
  '黄埔区': { zh: '黄埔区', en: 'Huangpu District' },
  '福田区': { zh: '福田区', en: 'Futian District' },
  '罗湖区': { zh: '罗湖区', en: 'Luohu District' },
  '南山区': { zh: '南山区', en: 'Nanshan District' },
  '宝安区': { zh: '宝安区', en: 'Bao\'an District' },
  '龙岗区': { zh: '龙岗区', en: 'Longgang District' },
  '龙华区': { zh: '龙华区', en: 'Longhua District' },
  '坪山区': { zh: '坪山区', en: 'Pingshan District' },
  '光明区': { zh: '光明区', en: 'Guangming District' },
  '盐田区': { zh: '盐田区', en: 'Yantian District' },
  'Manhattan': { zh: '曼哈顿', en: 'Manhattan' },
  'Brooklyn': { zh: '布鲁克林', en: 'Brooklyn' },
  'Queens': { zh: '皇后区', en: 'Queens' },
  'Bronx': { zh: '布朗克斯', en: 'Bronx' },
  'Staten Island': { zh: '史泰登岛', en: 'Staten Island' },
  'Downtown': { zh: '市中心', en: 'Downtown' },
  'Hollywood': { zh: '好莱坞', en: 'Hollywood' },
  'Beverly Hills': { zh: '比佛利山庄', en: 'Beverly Hills' },
  'Santa Monica': { zh: '圣莫尼卡', en: 'Santa Monica' },
  'Pasadena': { zh: '帕萨迪纳', en: 'Pasadena' },
  'Loop': { zh: '卢普区', en: 'Loop' },
  'Lincoln Park': { zh: '林肯公园', en: 'Lincoln Park' },
  'Wicker Park': { zh: '柳条公园', en: 'Wicker Park' },
  'Hyde Park': { zh: '海德公园', en: 'Hyde Park' },
  'Financial District': { zh: '金融区', en: 'Financial District' },
  'Mission': { zh: '教会区', en: 'Mission' },
  'SoMa': { zh: '南市场', en: 'SoMa' },
  'Marina': { zh: '滨海区', en: 'Marina' },
  'Midtown': { zh: '中城区', en: 'Midtown' },
  'Galleria': { zh: '商业街', en: 'Galleria' },
  'Memorial': { zh: '纪念区', en: 'Memorial' },
}

// 获取城市显示标签
function getCityLabel(city) {
  const translation = cityTranslations[city]
  if (translation) {
    return locale.value === 'en-US' ? translation.en : translation.zh
  }
  return city
}

// 获取区县显示标签
function getDistrictLabel(district) {
  const translation = districtTranslations[district]
  if (translation) {
    return locale.value === 'en-US' ? translation.en : translation.zh
  }
  return district
}

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

// 国家变化时，重置城市和区县
function onCountryChange() {
  // 无论用户是下拉选择还是手动输入，都清空下级
  form.city = ''
  form.district = ''
}

// 城市变化时，重置区县
function onCityChange() {
  form.district = ''
}

// 城市列表计算：如果是手动输入的国家，直接返回空数组，让用户可以自由输入城市
const cityList = computed(() => {
  if (!form.country) return []
  // 如果 form.country 不在预设的 regionData 里，说明是用户手动输入的国家，不提供下拉选项
  if (!regionData[form.country]) return []
  return Object.keys(regionData[form.country])
})

// 区县列表计算：同理，如果是手动输入的城市，不提供下拉选项
const districtList = computed(() => {
  if (!form.country || !form.city) return []
  if (!regionData[form.country] || !regionData[form.country][form.city]) return []
  return regionData[form.country][form.city] || []
})

function getCountryLabel(code: string | undefined): string {
  if (!code) return '-'
  const country = countryList.find(c => c.code === code)
  if (!country) return code
  // 根据当前语言返回对应标签
  return locale.value === 'en-US' ? country.labelEn : country.label
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listMemberInfo({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      name: query.name,
      relationship: query.relationship,
      gender: query.gender,
      country: query.country,
    })
    tableData.value = res.data?.rows || res.rows || []
    total.value = res.data?.total || res.total || 0
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.name = ''
  query.relationship = undefined
  query.gender = undefined
  query.country = undefined
  query.pageNum = 1
  fetchData()
}

// 导入按钮操作
function handleImport() {
  importDialogVisible.value = true
  upload.selectedFile = null
}

// 导出按钮操作
function handleExport() {
  const languageType = locale.value === 'en-US' ? 'en' : 'zh'
  const fileName = locale.value === 'en-US'
      ? `User_Info_${new Date().getTime()}.xlsx`
      : `用户信息_${new Date().getTime()}.xlsx`
  download("/healthReport/memberInfo/export", {
    ...query,
    languageType
  }, fileName)
}

// 下载模板
async function downloadTemplate() {
  try {
    const languageType = locale.value === 'en-US' ? 'en' : 'zh'
    const fileName = locale.value === 'en-US'
        ? `User_Info_Template_${new Date().getTime()}.xlsx`
        : `用户信息模板_${new Date().getTime()}.xlsx`

    await download("/healthReport/memberInfo/importTemplate", { languageType }, fileName)

    ElMessage.success($t('collection.templateDownloadSuccess'))
  } catch (error) {
    ElMessage.error($t('collection.templateDownloadFailed'))
    console.error(error)
  }
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

  if (response.code === 200) {
    ElMessage({
      message: response.msg || '导入成功',
      type: 'success',
      dangerouslyUseHTMLString: true,
      duration: 2000,
    })
  } else {
    ElMessage({
      message: response.msg,
      type: 'error',
      dangerouslyUseHTMLString: true,
      duration: 3000,
    })
  }

  fetchData()
}

// 提交上传文件
function submitImport() {
  const file = upload.selectedFile
  if (!file || !file.name.toLowerCase().endsWith('.xls') && !file.name.toLowerCase().endsWith('.xlsx')) {
    ElMessage.error($t('common.onlyXlsx'))
    return
  }
  uploadRef.value?.submit()
}

async function handleDelete(row: MemberInfo) {
  try {
    await delMemberInfo(row.id)
    ElMessage.success($t('common.deleteSuccess'))
    fetchData()
  } catch { /* handled */ }
}

// 表单操作
function resetForm() {
  form.memberNo = ''
  form.name = ''
  form.relationship = 1
  form.gender = 1
  form.birthDate = ''
  form.country = ''
  form.city = ''
  form.district = ''
  form.contact = ''
  form.address = ''
  form.ethnicity = ''
  form.birthplace = ''
  form.remark = ''
}

function handleAdd() {
  resetForm()
  isEdit.value = false
  showForm.value = true
}

async function handleEdit(row: MemberInfo) {
  resetForm()
  isEdit.value = true
  try {
    const res = await getMemberInfo(row.id)
    Object.assign(form, res.data?.data || res.data)
    showForm.value = true
  } catch { /* handled */ }
}

function handleBack() {
  showForm.value = false
  isEdit.value = false
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateMemberInfo(form)
    } else {
      await addMemberInfo(form)
    }
    ElMessage.success($t('common.saveSuccess'))
    showForm.value = false
    fetchData()
  } catch { /* handled */ } finally {
    submitLoading.value = false
  }
}

onMounted(fetchData)
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

  .header-buttons {
    display: flex;
    gap: 10px;
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
  width: 100%;
  box-sizing: border-box;
  overflow-x: auto;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.form-card {
  background: #ffffff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
  padding: 30px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}
</style>
