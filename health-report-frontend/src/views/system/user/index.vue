<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!--用户数据-->
      <el-col>
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="120px">
          <el-form-item :label="$t('system.user.name')" prop="nickName">
            <el-input v-model="queryParams.nickName" :placeholder="$t('system.user.nickNamePlaceholder')" clearable style="width: 240px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item :label="$t('system.user.phonenumber')" prop="phonenumber">
            <el-input v-model="queryParams.phonenumber" :placeholder="$t('system.user.phonePlaceholder')" clearable style="width: 240px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item :label="$t('system.user.email')" prop="email">
            <el-input v-model="queryParams.email" :placeholder="$t('system.user.emailPlaceholder')" clearable style="width: 240px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item :label="$t('system.user.status')" prop="status">
            <el-select v-model="queryParams.status" :placeholder="$t('system.user.statusPlaceholder')" clearable style="width: 240px">
              <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">{{ $t('system.user.search') }}</el-button>
            <el-button icon="Refresh" @click="resetQuery">{{ $t('system.user.reset') }}</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:user:add']">{{ $t('system.user.add') }}</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:user:edit']">{{ $t('system.user.edit') }}</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:user:remove']">{{ $t('system.user.delete') }}</el-button>
          </el-col>
          <!--          <el-col :span="1.5">
                      <el-button type="info" plain icon="Upload" @click="handleImport" v-hasPermi="['system:user:import']">{{ $t('system.user.import') }}</el-button>
                    </el-col>-->
          <el-col :span="1.5">
            <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['system:user:export']">{{ $t('system.user.export') }}</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column :label="$t('system.user.userId')" align="center" key="userId" prop="userId" v-if="columns.userId.visible" />
          <!--          <el-table-column :label="$t('system.user.userName')" align="center" key="userName" prop="userName" v-if="columns.userName.visible" :show-overflow-tooltip="true" />-->
          <el-table-column :label="$t('system.user.name')" align="center" key="nickName" prop="nickName" v-if="columns.nickName.visible" :show-overflow-tooltip="true" />
          <el-table-column :label="$t('system.user.phonenumber')" align="center" key="phonenumber" prop="phonenumber" v-if="columns.phonenumber.visible" width="120" />
          <el-table-column :label="$t('system.user.email')" align="center" key="email" prop="email" v-if="columns.email.visible" width="120" />
          <el-table-column :label="$t('system.user.country')" align="center" key="country" v-if="columns.country.visible" width="100" :show-overflow-tooltip="true">
            <template #default="scope">{{ getCountryLabel(scope.row.country) }}</template>
          </el-table-column>
          <el-table-column :label="$t('system.user.city')" align="center" key="city" v-if="columns.city.visible" width="100" :show-overflow-tooltip="true">
            <template #default="scope">{{ getCityLabel(scope.row.city) }}</template>
          </el-table-column>
          <el-table-column :label="$t('system.user.district')" align="center" key="district" v-if="columns.district.visible" width="100" :show-overflow-tooltip="true">
            <template #default="scope">{{ getDistrictLabel(scope.row.district) }}</template>
          </el-table-column>
          <el-table-column :label="$t('system.user.address')" align="center" key="address" prop="address" v-if="columns.address.visible" width="150" :show-overflow-tooltip="true" />
          <el-table-column :label="$t('system.user.status')" align="center" key="status" v-if="columns.status.visible">
            <template #default="scope">
              <el-switch
                  v-model="scope.row.status"
                  active-value="0"
                  inactive-value="1"
                  @change="handleStatusChange(scope.row)"
              ></el-switch>
            </template>
          </el-table-column>
          <el-table-column :label="$t('system.user.operation')" align="center" width="150" class-name="small-padding fixed-width">
            <template #default="scope">
              <el-tooltip :content="$t('system.user.edit')" placement="top" v-if="scope.row.userId !== 1">
                <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:user:edit']"></el-button>
              </el-tooltip>
              <el-tooltip :content="$t('system.user.delete')" placement="top" v-if="scope.row.userId !== 1">
                <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:user:remove']"></el-button>
              </el-tooltip>
              <el-tooltip :content="$t('system.user.resetPwd')" placement="top" v-if="scope.row.userId !== 1">
                <el-button link type="primary" icon="Key" @click="handleResetPwd(scope.row)" v-hasPermi="['system:user:resetPwd']"></el-button>
              </el-tooltip>
              <!--              <el-tooltip :content="$t('system.user.authRole')" placement="top" v-if="scope.row.userId !== 1">
                              <el-button link type="primary" icon="CircleCheck" @click="handleAuthRole(scope.row)" v-hasPermi="['system:user:edit']"></el-button>
                            </el-tooltip>-->
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-col>
    </el-row>

    <!-- 添加或修改用户配置对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="userRef" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('system.user.nickNameLabel')" prop="nickName">
              <el-input v-model="form.nickName" :placeholder="$t('system.user.nickNamePlaceholder')" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.userId == undefined || form.phonenumber">
            <el-form-item :label="$t('system.user.phoneLabel')" prop="phonenumber">
              <el-input v-model="form.phonenumber" :placeholder="$t('system.user.phonePlaceholder')" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12" v-if="form.userId == undefined || form.email">
            <el-form-item :label="$t('system.user.emailLabel')" prop="email">
              <el-input v-model="form.email" :placeholder="$t('system.user.emailPlaceholder')" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" :label="$t('system.user.password')" prop="password">
              <el-input v-model="form.password" :placeholder="$t('system.user.passwordPlaceholder')" type="password" maxlength="20" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('system.user.country')">
              <el-select v-model="form.country" filterable style="width: 100%" @change="onCountryChange" :placeholder="$t('system.user.country')">
                <el-option v-for="c in countryList" :key="c.code" :label="locale.value === 'en-US' ? c.labelEn : c.label" :value="c.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('system.user.city')">
              <el-select v-model="form.city" filterable style="width: 100%" @change="onCityChange" :placeholder="$t('system.user.city')" :disabled="!cityList.length">
                <el-option v-for="c in cityList" :key="c" :label="getCityLabel(c)" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('system.user.district')">
              <el-select v-model="form.district" filterable allow-create style="width: 100%" :placeholder="$t('system.user.district')" :disabled="!districtList.length">
                <el-option v-for="d in districtList" :key="d" :label="getDistrictLabel(d)" :value="d" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item :label="$t('system.user.address')">
              <el-input v-model="form.address" :placeholder="$t('system.user.address')" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('system.user.status')">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('system.user.roleLabel')" prop="roleIds">
              <el-select v-model="form.roleIds" multiple :placeholder="$t('system.user.selectPlaceholder')">
                <el-option v-for="item in roleOptions" :key="item.roleId" :label="getRoleLabel(item.roleName)"
                           :value="item.roleId" :disabled="item.status == '1'"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">{{ $t('common.confirm') }}</el-button>
          <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 用户导入对话框 -->
    <el-dialog :title="upload.title" v-model="upload.open" width="400px" append-to-body>
      <el-upload ref="uploadRef" :limit="1" accept=".xlsx, .xls" :headers="upload.headers" :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading" :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :on-change="handleFileChange" :on-remove="handleFileRemove" :auto-upload="false" drag>
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">{{ $t('collection.dragOrClick') }}</div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <div class="el-upload__tip">
              <el-checkbox v-model="upload.updateSupport" />{{ $t('system.user.updateExisting') }}
            </div>
            <span>{{ $t('system.user.importTip') }}</span>
            <el-link type="primary" underline="never" style="font-size: 12px; vertical-align: baseline" @click="importTemplate">{{ $t('system.user.downloadTemplate') }}</el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFileForm">{{ $t('common.confirm') }}</el-button>
          <el-button @click="upload.open = false">{{ $t('common.cancel') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="User">
import {getToken} from "@/utils/auth"
import useAppStore from '@/store/modules/app'
import {changeUserStatus, listUser, resetUserPwd, delUser, getUser, updateUser, addUser} from "@/api/system/user"
import {useI18n} from 'vue-i18n'

const router = useRouter()
const appStore = useAppStore()
const {t: $t, locale} = useI18n()
const {proxy} = getCurrentInstance()
const {sys_normal_disable, sys_user_sex} = proxy.useDict("sys_normal_disable", "sys_user_sex")

const userList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const initPassword = ref(undefined)
const postOptions = ref([])
const roleOptions = ref([])
// 角色名称国际化映射
const roleTranslations = {
  '管理员': { zh: '管理员', en: 'Administrator' },
  '普通角色': { zh: '普通角色', en: 'Regular Role' },
}
/*** 用户导入参数 */
const upload = reactive({
  // 是否显示弹出层（用户导入）
  open: false,
  // 弹出层标题（用户导入）
  title: "",
  // 是否禁用上传
  isUploading: false,
  // 是否更新已经存在的用户数据
  updateSupport: 0,
  // 设置上传的请求头部
  headers: {Authorization: "Bearer " + getToken()},
  // 上传的地址
  url: import.meta.env.VITE_APP_BASE_API + "/system/user/importData"
})
// 列显隐信息
const columns = ref({
  userId: {label: $t('system.user.userId'), visible: true},
  userName: {label: $t('system.user.userName'), visible: true},
  nickName: {label: $t('system.user.name'), visible: true},
  deptName: {label: $t('system.user.deptName'), visible: true},
  phonenumber: {label: $t('system.user.phonenumber'), visible: true},
  email: {label: $t('system.user.email'), visible: true},
  country: {label: $t('system.user.country'), visible: true},
  city: {label: $t('system.user.city'), visible: true},
  district: {label: $t('system.user.district'), visible: true},
  address: {label: $t('system.user.address'), visible: true},
  status: {label: $t('system.user.status'), visible: true},
  createTime: {label: $t('system.user.createTime'), visible: true}
})

// ===== 国家/城市/地区 级联数据 =====
const countryList = [
  {code: 'CN', label: '中国', labelEn: 'China'},
  {code: 'US', label: '美国', labelEn: 'United States'},
  {code: 'JP', label: '日本', labelEn: 'Japan'},
  {code: 'KR', label: '韩国', labelEn: 'South Korea'},
  {code: 'GB', label: '英国', labelEn: 'United Kingdom'},
  {code: 'DE', label: '德国', labelEn: 'Germany'},
  {code: 'FR', label: '法国', labelEn: 'France'},
  {code: 'AU', label: '澳大利亚', labelEn: 'Australia'},
  {code: 'CA', label: '加拿大', labelEn: 'Canada'},
  {code: 'SG', label: '新加坡', labelEn: 'Singapore'},
]

// 城市翻译映射
const cityTranslations = {
  // 中国城市
  '北京': {zh: '北京', en: 'Beijing'},
  '上海': {zh: '上海', en: 'Shanghai'},
  '广州': {zh: '广州', en: 'Guangzhou'},
  '深圳': {zh: '深圳', en: 'Shenzhen'},
  '杭州': {zh: '杭州', en: 'Hangzhou'},
  '成都': {zh: '成都', en: 'Chengdu'},
  '南京': {zh: '南京', en: 'Nanjing'},
  '武汉': {zh: '武汉', en: 'Wuhan'},
  '重庆': {zh: '重庆', en: 'Chongqing'},
  '天津': {zh: '天津', en: 'Tianjin'},
  '西安': {zh: '西安', en: "Xi'an"},
  '苏州': {zh: '苏州', en: 'Suzhou'},
  // 美国城市
  'New York': {zh: '纽约', en: 'New York'},
  'Los Angeles': {zh: '洛杉矶', en: 'Los Angeles'},
  'Chicago': {zh: '芝加哥', en: 'Chicago'},
  'San Francisco': {zh: '旧金山', en: 'San Francisco'},
  'Houston': {zh: '休斯顿', en: 'Houston'},
  // 日本城市
  '東京': {zh: '东京', en: 'Tokyo'},
  '大阪': {zh: '大阪', en: 'Osaka'},
  '名古屋': {zh: '名古屋', en: 'Nagoya'},
  '京都': {zh: '京都', en: 'Kyoto'},
  // 韩国城市
  '서울': {zh: '首尔', en: 'Seoul'},
  '부산': {zh: '釜山', en: 'Busan'},
  // 英国城市
  'London': {zh: '伦敦', en: 'London'},
  'Manchester': {zh: '曼彻斯特', en: 'Manchester'},
  'Birmingham': {zh: '伯明翰', en: 'Birmingham'},
}

// 区县翻译映射
const districtTranslations = {
  // 北京
  '东城区': {zh: '东城区', en: 'Dongcheng District'},
  '西城区': {zh: '西城区', en: 'Xicheng District'},
  '朝阳区': {zh: '朝阳区', en: 'Chaoyang District'},
  '海淀区': {zh: '海淀区', en: 'Haidian District'},
  '丰台区': {zh: '丰台区', en: 'Fengtai District'},
  '通州区': {zh: '通州区', en: 'Tongzhou District'},
  '顺义区': {zh: '顺义区', en: 'Shunyi District'},
  '大兴区': {zh: '大兴区', en: 'Daxing District'},
  '昌平区': {zh: '昌平区', en: 'Changping District'},
  '房山区': {zh: '房山区', en: 'Fangshan District'},
  // 上海
  '浦东新区': {zh: '浦东新区', en: 'Pudong New Area'},
  '黄浦区': {zh: '黄浦区', en: 'Huangpu District'},
  '徐汇区': {zh: '徐汇区', en: 'Xuhui District'},
  '静安区': {zh: '静安区', en: 'Jing\'an District'},
  '长宁区': {zh: '长宁区', en: 'Changning District'},
  '虹口区': {zh: '虹口区', en: 'Hongkou District'},
  '杨浦区': {zh: '杨浦区', en: 'Yangpu District'},
  '闵行区': {zh: '闵行区', en: 'Minhang District'},
  '宝山区': {zh: '宝山区', en: 'Baoshan District'},
  '嘉定区': {zh: '嘉定区', en: 'Jiading District'},
  // 广州
  '天河区': {zh: '天河区', en: 'Tianhe District'},
  '越秀区': {zh: '越秀区', en: 'Yuexiu District'},
  '荔湾区': {zh: '荔湾区', en: 'Liwan District'},
  '海珠区': {zh: '海珠区', en: 'Haizhu District'},
  '白云区': {zh: '白云区', en: 'Baiyun District'},
  '番禺区': {zh: '番禺区', en: 'Panyu District'},
  '花都区': {zh: '花都区', en: 'Huadu District'},
  '南沙区': {zh: '南沙区', en: 'Nansha District'},
  '增城区': {zh: '增城区', en: 'Zengcheng District'},
  '黄埔区': {zh: '黄埔区', en: 'Huangpu District'},
  // 深圳
  '福田区': {zh: '福田区', en: 'Futian District'},
  '罗湖区': {zh: '罗湖区', en: 'Luohu District'},
  '南山区': {zh: '南山区', en: 'Nanshan District'},
  '宝安区': {zh: '宝安区', en: 'Bao\'an District'},
  '龙岗区': {zh: '龙岗区', en: 'Longgang District'},
  '龙华区': {zh: '龙华区', en: 'Longhua District'},
  '坪山区': {zh: '坪山区', en: 'Pingshan District'},
  '光明区': {zh: '光明区', en: 'Guangming District'},
  '盐田区': {zh: '盐田区', en: 'Yantian District'},
  // 美国
  'Manhattan': {zh: '曼哈顿', en: 'Manhattan'},
  'Brooklyn': {zh: '布鲁克林', en: 'Brooklyn'},
  'Queens': {zh: '皇后区', en: 'Queens'},
  'Bronx': {zh: '布朗克斯', en: 'Bronx'},
  'Staten Island': {zh: '史泰登岛', en: 'Staten Island'},
  'Downtown': {zh: '市中心', en: 'Downtown'},
  'Hollywood': {zh: '好莱坞', en: 'Hollywood'},
  'Beverly Hills': {zh: '比佛利山庄', en: 'Beverly Hills'},
  'Santa Monica': {zh: '圣莫尼卡', en: 'Santa Monica'},
  'Pasadena': {zh: '帕萨迪纳', en: 'Pasadena'},
  'Loop': {zh: '卢普区', en: 'Loop'},
  'Lincoln Park': {zh: '林肯公园', en: 'Lincoln Park'},
  'Wicker Park': {zh: '柳条公园', en: 'Wicker Park'},
  'Hyde Park': {zh: '海德公园', en: 'Hyde Park'},
  'Financial District': {zh: '金融区', en: 'Financial District'},
  'Mission': {zh: '教会区', en: 'Mission'},
  'SoMa': {zh: '南市场', en: 'SoMa'},
  'Marina': {zh: '滨海区', en: 'Marina'},
  'Midtown': {zh: '中城区', en: 'Midtown'},
  'Galleria': {zh: '商业街', en: 'Galleria'},
  'Memorial': {zh: '纪念区', en: 'Memorial'},
}

const regionData = {
  CN: {
    '北京': ['东城区', '西城区', '朝阳区', '海淀区', '丰台区', '通州区', '顺义区', '大兴区', '昌平区', '房山区'],
    '上海': ['浦东新区', '黄浦区', '徐汇区', '静安区', '长宁区', '虹口区', '杨浦区', '闵行区', '宝山区', '嘉定区'],
    '广州': ['天河区', '越秀区', '荔湾区', '海珠区', '白云区', '番禺区', '花都区', '南沙区', '增城区', '黄埔区'],
    '深圳': ['福田区', '罗湖区', '南山区', '宝安区', '龙岗区', '龙华区', '坪山区', '光明区', '盐田区'],
    '杭州': ['上城区', '拱墅区', '西湖区', '滨江区', '萧山区', '余杭区', '临平区', '钱塘区', '富阳区', '临安区'],
    '成都': ['锦江区', '青羊区', '金牛区', '武侯区', '成华区', '龙泉驿区', '新都区', '温江区', '双流区', '郫都区'],
    '南京': ['玄武区', '秦淮区', '建邺区', '鼓楼区', '浦口区', '栖霞区', '雨花台区', '江宁区', '六合区'],
    '武汉': ['江岸区', '江汉区', '硚口区', '汉阳区', '武昌区', '青山区', '洪山区', '东西湖区', '蔡甸区'],
    '重庆': ['渝中区', '大渡口区', '江北区', '沙坪坝区', '九龙坡区', '南岸区', '北碚区', '渝北区', '巴南区'],
    '天津': ['和平区', '河东区', '河西区', '南开区', '河北区', '红桥区', '东丽区', '西青区', '津南区', '北辰区'],
    '西安': ['碑林区', '新城区', '莲湖区', '雁塔区', '未央区', '灞桥区', '长安区', '高新区'],
    '苏州': ['姑苏区', '虎丘区', '吴中区', '相城区', '吴江区', '昆山市', '太仓市', '常熟市', '张家港市'],
  },
  US: {
    'New York': ['Manhattan', 'Brooklyn', 'Queens', 'Bronx', 'Staten Island'],
    'Los Angeles': ['Downtown', 'Hollywood', 'Beverly Hills', 'Santa Monica', 'Pasadena'],
    'Chicago': ['Loop', 'Lincoln Park', 'Wicker Park', 'Hyde Park'],
    'San Francisco': ['Financial District', 'Mission', 'SoMa', 'Marina'],
    'Houston': ['Downtown', 'Midtown', 'Galleria', 'Memorial'],
  },
  JP: {
    '東京': ['千代田区', '中央区', '港区', '新宿区', '渋谷区', '豊島区', '文京区', '品川区', '目黒区', '世田谷区'],
    '大阪': ['北区', '中央区', '天王寺区', '浪速区', '淀川区'],
    '名古屋': ['中区', '東区', '千種区', '名東区', '昭和区'],
    '京都': ['上京区', '中京区', '下京区', '東山区', '左京区'],
  },
  KR: {
    '서울': ['강남구', '서초구', '송파구', '강동구', '마포구', '용산구', '종로구'],
    '부산': ['해운대구', '수영구', '남구', '중구', '사하구'],
  },
  GB: {
    'London': ['Westminster', 'Camden', 'Islington', 'Hackney', 'Greenwich'],
    'Manchester': ['City Centre', 'Salford', 'Trafford', 'Stockport'],
    'Birmingham': ['City Centre', 'Edgbaston', 'Solihull'],
  },
}

// 获取国家显示标签
function getCountryLabel(code) {
  if (!code) return ''
  const country = countryList.find(c => c.code === code)
  if (!country) return code
  return locale.value === 'en-US' ? country.labelEn : country.label
}

// 获取城市显示标签
function getCityLabel(city) {
  if (!city) return ''
  const translation = cityTranslations[city]
  if (translation) {
    return locale.value === 'en-US' ? translation.en : translation.zh
  }
  return city
}

// 获取区县显示标签
function getDistrictLabel(district) {
  if (!district) return ''
  const translation = districtTranslations[district]
  if (translation) {
    return locale.value === 'en-US' ? translation.en : translation.zh
  }
  return district
}

// 获取角色显示标签
function getRoleLabel(roleName) {
  if (!roleName) return ''
  const translation = roleTranslations[roleName]
  if (translation) {
    return locale.value === 'en-US' ? translation.en : translation.zh
  }
  return roleName
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

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userName: undefined,
    phonenumber: undefined,
    email: undefined,
    status: undefined
  },
  rules: {
    userName: [{required: true, message: () => $t('system.user.userNameRequired'), trigger: "blur"}, {
      min: 2,
      max: 20,
      message: () => $t('system.user.userNameRule'),
      trigger: "blur"
    }],
    nickName: [{required: true, message: () => $t('system.user.nickNameRequired'), trigger: "blur"}],
    password: [{required: true, message: () => $t('system.user.passwordRequired'), trigger: "blur"}, {
      min: 5,
      max: 20,
      message: () => $t('system.user.passwordRule'),
      trigger: "blur"
    }, {pattern: /^[^<>"'|\\]+$/, message: () => $t('system.profile.pwdInvalid'), trigger: "blur"}],
    email: [{required: true, message: () => $t('system.user.emailRequired'), trigger: "blur"}, {
      type: "email",
      message: () => $t('system.user.emailRule'),
      trigger: ["blur", "change"]
    }],
    phonenumber: [{required: true, message: () => $t('system.user.phoneRequired'), trigger: "blur"}, {
      pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
      message: () => $t('system.user.phoneRule'),
      trigger: "blur"
    }],
    roleIds: [{required: true, message: () => $t('system.user.roleRequired'), trigger: "change"}]
  }
})

const {queryParams, form, rules} = toRefs(data)

/** 查询用户列表 */
function getList() {
  loading.value = true
  listUser(queryParams.value).then(res => {
    loading.value = false
    userList.value = res.rows
    total.value = res.total
  })
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

/** 删除按钮操作 */
function handleDelete(row) {
  const userIds = row.userId || ids.value
  proxy.$modal.confirm($t('system.user.confirmDelete', {ids: userIds})).then(function () {
    return delUser(userIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess($t('common.success'))
  }).catch(() => {
  })
}

/** 导出按钮操作 */
function handleExport() {
  const lang = locale.value === 'en-US' ? 'en' : 'zh'
  proxy.download(`system/user/export?language=${lang}`, {
    ...queryParams.value,
  }, `user_${new Date().getTime()}.xlsx`)
}

/** 用户状态修改  */
function handleStatusChange(row) {
  let text = row.status === "0" ? $t('system.user.enable') : $t('system.user.disable')
  proxy.$modal.confirm($t('system.user.statusChangeConfirm', {text: text, userName: row.userName})).then(function () {
    return changeUserStatus(row.userId, row.status)
  }).then(() => {
    proxy.$modal.msgSuccess(text + $t('common.success'))
  }).catch(function () {
    row.status = row.status === "0" ? "1" : "0"
  })
}

/** 更多操作 */
function handleCommand(command, row) {
  switch (command) {
    case "handleResetPwd":
      handleResetPwd(row)
      break
    case "handleAuthRole":
      handleAuthRole(row)
      break
    default:
      break
  }
}

/** 跳转角色分配 */
function handleAuthRole(row) {
  const userId = row.userId
  router.push("/system/user-auth/role/" + userId)
}

/** 重置密码按钮操作 */
function handleResetPwd(row) {
  proxy.$prompt($t('system.user.resetPwdPrompt', {userName: row.userName}), $t('common.prompt'), {
    confirmButtonText: $t('common.confirm'),
    cancelButtonText: $t('common.cancel'),
    closeOnClickModal: false,
    inputPattern: /^.{5,20}$/,
    inputErrorMessage: $t('system.user.passwordRule'),
    inputValidator: (value) => {
      if (/<|>|"|'|\||\\/.test(value)) {
        return $t('system.profile.pwdInvalid')
      }
    },
  }).then(({value}) => {
    resetUserPwd(row.userId, value).then(() => {
      proxy.$modal.msgSuccess($t('system.user.resetPwdSuccess') + value)
    })
  }).catch(() => {
  })
}

/** 选择条数  */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.userId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 导入按钮操作 */
function handleImport() {
  upload.title = $t('system.user.importTitle')
  upload.open = true
  upload.selectedFile = null
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download("system/user/importTemplate", {}, `user_template_${new Date().getTime()}.xlsx`)
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
  upload.open = false
  upload.isUploading = false
  proxy.$refs["uploadRef"].handleRemove(file)
  proxy.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", $t('collection.importResult'), {dangerouslyUseHTMLString: true})
  getList()
}

/** 提交上传文件 */
function submitFileForm() {
  const file = upload.selectedFile
  if (!file || file.length === 0 || !file.name.toLowerCase().endsWith('.xls') && !file.name.toLowerCase().endsWith('.xlsx')) {
    proxy.$modal.msgError($t('system.user.selectFile'))
    return
  }
  proxy.$refs["uploadRef"].submit()
}

/** 重置操作表单 */
function reset() {
  form.value = {
    userId: undefined,
    deptId: undefined,
    userName: undefined,
    nickName: undefined,
    password: undefined,
    phonenumber: undefined,
    email: undefined,
    sex: undefined,
    status: "0",
    remark: undefined,
    postIds: [],
    roleIds: []
  }
  proxy.resetForm("userRef")
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  getUser().then(response => {
    postOptions.value = response.posts
    roleOptions.value = response.roles
    open.value = true
    title.value = $t('system.user.addTitle')
    form.value.password = initPassword.value
  })
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const userId = row.userId || ids.value
  getUser(userId).then(response => {
    form.value = response.data
    postOptions.value = response.posts
    roleOptions.value = response.roles
    form.value.postIds = response.postIds
    form.value.roleIds = response.roleIds
    open.value = true
    title.value = $t('system.user.editTitle')
    form.value.password = ""
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["userRef"].validate(valid => {
    if (valid) {
      if (form.value.userId != undefined) {
        updateUser(form.value).then(() => {
          proxy.$modal.msgSuccess($t('common.success'))
          open.value = false
          getList()
        })
      } else {
        addUser(form.value).then(() => {
          proxy.$modal.msgSuccess($t('common.success'))
          open.value = false
          getList()
        })
      }
    }
  })
}

onMounted(() => {
  getList()
  proxy.getConfigKey("sys.user.initPassword").then(response => {
    initPassword.value = response.msg
  })
})
</script>
