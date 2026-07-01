<template>
  <div class="auth-page-shell register-page">
    <div class="auth-card auth-card--wide register-card">
      <h2 class="auth-title register-title">{{ $t('auth.register') }}</h2>
      <el-tabs v-model="registerTab" class="register-tabs" stretch>
        <el-tab-pane :label="$t('auth.phoneRegister')" name="phone">
          <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" label-width="100px">
            <el-form-item :label="$t('auth.name')" prop="name">
              <el-input v-model="phoneForm.name" />
            </el-form-item>
            <el-form-item :label="$t('auth.phone')" prop="phone">
              <el-input v-model="phoneForm.phone" />
            </el-form-item>
            <el-form-item :label="$t('auth.verificationCode')" prop="code">
              <div class="code-input">
                <el-input v-model="phoneForm.code" />
                <el-button :disabled="codeCooldown > 0" @click="sendCode('phone')" type="primary" plain>
                  {{ codeCooldown > 0 ? $t('auth.resendCode', { seconds: codeCooldown }) : $t('auth.sendCode') }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item :label="$t('auth.password')" prop="password">
              <el-input v-model="phoneForm.password" type="password" show-password />
            </el-form-item>
            <el-form-item :label="$t('auth.confirmPassword')" prop="confirmPassword">
              <el-input v-model="phoneForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item :label="$t('auth.country')" prop="country">
              <el-select v-model="phoneForm.country" filterable
                         allow-create style="width: 100%" @change="onPhoneCountryChange">
                <el-option v-for="c in countryList" :key="c.code" :label="locale === 'en-US' ? c.labelEn : c.label" :value="c.code" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('auth.city')">
              <el-select v-model="phoneForm.city" filterable
                         allow-create style="width: 100%" @change="onPhoneCityChange" :disabled="!phoneCityList.length">
                <el-option v-for="c in phoneCityList" :key="c" :label="getCityLabel(c)" :value="c" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('auth.address')">
              <el-input v-model="phoneForm.address" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" @click="handlePhoneRegister" :loading="loading">{{ $t('auth.register') }}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane :label="$t('auth.emailRegister')" name="email">
          <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-width="100px">
            <el-form-item :label="$t('auth.name')" prop="name">
              <el-input v-model="emailForm.name" />
            </el-form-item>
            <el-form-item :label="$t('auth.email')" prop="email">
              <el-input v-model="emailForm.email" />
            </el-form-item>
            <el-form-item :label="$t('auth.verificationCode')" prop="code">
              <div class="code-input">
                <el-input v-model="emailForm.code" />
                <el-button :disabled="codeCooldown > 0" @click="sendCode('email')" type="primary" plain>
                  {{ codeCooldown > 0 ? $t('auth.resendCode', { seconds: codeCooldown }) : $t('auth.sendCode') }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item :label="$t('auth.password')" prop="password">
              <el-input v-model="emailForm.password" type="password" show-password />
            </el-form-item>
            <el-form-item :label="$t('auth.confirmPassword')" prop="confirmPassword">
              <el-input v-model="emailForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item :label="$t('auth.contact')">
              <el-input v-model="emailForm.contact" />
            </el-form-item>
            <el-form-item :label="$t('auth.country')" prop="country">
              <el-select v-model="emailForm.country" filterable
                         allow-create style="width: 100%" @change="onEmailCountryChange">
                <el-option v-for="c in countryList" :key="c.code" :label="locale === 'en-US' ? c.labelEn : c.label" :value="c.code" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('auth.city')">
              <el-select v-model="emailForm.city" filterable
                         allow-create style="width: 100%" @change="onEmailCityChange" :disabled="!emailCityList.length">
                <el-option v-for="c in emailCityList" :key="c" :label="getCityLabel(c)" :value="c" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('auth.address')">
              <el-input v-model="emailForm.address" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" @click="handleEmailRegister" :loading="loading">{{ $t('auth.register') }}</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <div class="register-footer">
        <router-link to="/login">{{ $t('auth.goToLogin') }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { userRegister, sendSmsCode, sendEmailCode } from "@/api/login"

const router = useRouter()
const { proxy } = getCurrentInstance()
const { locale } = useI18n()

const registerTab = ref('phone')
const loading = ref(false)
const codeCooldown = ref(0)

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
    return locale === 'en-US' ? translation.en : translation.zh
  }
  return city
}

// 获取区县显示标签
function getDistrictLabel(district) {
  const translation = districtTranslations[district]
  if (translation) {
    return locale === 'en-US' ? translation.en : translation.zh
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

const phoneForm = reactive({
  name: '', phone: '', code: '',
  password: '', confirmPassword: '',
  country: 'CN', city: '', district: '', address: ''
})
const emailForm = reactive({
  name: '', email: '', code: '',
  password: '', confirmPassword: '',
  country: 'CN', contact: '', city: '', district: '', address: ''
})

const phoneFormRef = ref()
const emailFormRef = ref()
let cooldownTimer = null

// 手机注册表单的城市和区/县列表
const phoneCityList = computed(() => {
  if (!phoneForm.country || !regionData[phoneForm.country]) return []
  return Object.keys(regionData[phoneForm.country])
})

const phoneDistrictList = computed(() => {
  if (!phoneForm.country || !phoneForm.city || !regionData[phoneForm.country]) return []
  return regionData[phoneForm.country][phoneForm.city] || []
})

// 邮箱注册表单的城市和区/县列表
const emailCityList = computed(() => {
  if (!emailForm.country || !regionData[emailForm.country]) return []
  return Object.keys(regionData[emailForm.country])
})

const emailDistrictList = computed(() => {
  if (!emailForm.country || !emailForm.city || !regionData[emailForm.country]) return []
  return regionData[emailForm.country][emailForm.city] || []
})

// 手机注册表单的联动方法
function onPhoneCountryChange() {
  phoneForm.city = ''
  phoneForm.district = ''
}

function onPhoneCityChange() {
  phoneForm.district = ''
}

// 邮箱注册表单的联动方法
function onEmailCountryChange() {
  emailForm.city = ''
  emailForm.district = ''
}

function onEmailCityChange() {
  emailForm.district = ''
}

const validateConfirmPassword = (rule, value, callback) => {
  const form = registerTab.value === 'phone' ? phoneForm : emailForm
  if (!value) {
    callback(new Error(proxy.$t('auth.confirmPasswordRequired')))
  } else if (value !== form.password) {
    callback(new Error(proxy.$t('auth.passwordMismatch')))
  } else {
    callback()
  }
}

const phoneRules = {
  name: [{ required: true, message: proxy.$t('auth.nameRequired'), trigger: 'blur' }],
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: proxy.$t('auth.phoneInvalid'), trigger: 'blur' }],
  code: [{ required: true, message: proxy.$t('auth.codeRequired'), trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 20, message: proxy.$t('auth.passwordLength'), trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
  country: [{ required: true, message: proxy.$t('auth.countryRequired'), trigger: 'change' }],
}

const emailRules = {
  name: [{ required: true, message: proxy.$t('auth.nameRequired'), trigger: 'blur' }],
  email: [{ required: true, type: 'email', message: proxy.$t('auth.emailInvalid'), trigger: 'blur' }],
  code: [{ required: true, message: proxy.$t('auth.codeRequired'), trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 20, message: proxy.$t('auth.passwordLength'), trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }],
  country: [{ required: true, message: proxy.$t('auth.countryRequired'), trigger: 'change' }],
}

// 发送验证码（适配后端）
async function sendCode(type) {
  const target = type === 'phone' ? phoneForm.phone : emailForm.email

  // 验证邮箱格式
  if (type === 'email') {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!target) {
      proxy.$modal.msgError('请输入邮箱地址')
      return
    }
    if (!emailRegex.test(target)) {
      proxy.$modal.msgError('请输入正确的邮箱地址')
      return
    }
  } else {
    // 验证手机号格式
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!target) {
      proxy.$modal.msgError('请输入手机号码')
      return
    }
    if (!phoneRegex.test(target)) {
      proxy.$modal.msgError('请输入正确的手机号码')
      return
    }
  }

  try {
    if (type === 'phone') {
      await sendSmsCode({
        mobile: target,
        smsmode: "1" // 1=注册
      })
    } else {
      await sendEmailCode({
        email: target,
        type: "register"
      })
    }
    proxy.$modal.msgSuccess(proxy.$t('auth.codeSent'))
    codeCooldown.value = 60
    cooldownTimer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0) clearInterval(cooldownTimer)
    }, 1000)
  } catch (error) {
    // 拦截器已处理错误提示，此处不再重复显示
  }
}

// 手机注册
async function handlePhoneRegister() {
  const valid = await phoneFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userRegister({
      username: phoneForm.phone,
      nickName: phoneForm.name,
      phone: phoneForm.phone,
      password: phoneForm.password,
      code: phoneForm.code,
      country: phoneForm.country,
      city: phoneForm.city,
      district: phoneForm.district,
      address: phoneForm.address,
      registerType: "phone"
    })
    proxy.$modal.msgSuccess(proxy.$t('auth.registerSuccess'))
    setTimeout(() => router.push('/login'), 1500)
  } catch (e) {
    loading.value = false
    // 拦截器已处理错误提示，此处不再重复显示
  }
}

// 邮箱注册
async function handleEmailRegister() {
  const valid = await emailFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userRegister({
      username: emailForm.email,
      nickName: emailForm.name,
      email: emailForm.email,
      password: emailForm.password,
      code: emailForm.code,
      country: emailForm.country,
      city: emailForm.city,
      district: emailForm.district,
      address: emailForm.address,
      contact: emailForm.contact,
      registerType: "email"
    })
    proxy.$modal.msgSuccess(proxy.$t('auth.registerSuccess'))
    setTimeout(() => router.push('/login'), 1500)
  } catch (e) {
    loading.value = false
    // 拦截器已处理错误提示，此处不再重复显示
  }
}

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>

<style lang="scss" scoped>
.auth-page-shell {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0c4a46 0%, #134e4a 50%, #115e59 100%);
  padding: 20px;
}

.auth-card {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  padding: 40px;
  width: 100%;
  max-width: 420px;
}

.auth-card--wide {
  max-width: 520px;
}

.auth-title {
  text-align: center;
  color: #0f172a;
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 30px;
}

.register-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 24px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    background-color: #e2e8f0;
  }

  :deep(.el-tabs__item) {
    font-size: 15px;
    color: #64748b;

    &.is-active {
      color: #0d9488;
      font-weight: 500;
    }
  }

  :deep(.el-tabs__active-bar) {
    background-color: #0d9488;
  }
}

.code-input {
  display: flex;
  gap: 12px;

  .el-input {
    flex: 1;
  }

  .el-button {
    min-width: 140px;
  }
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #0d9488 0%, #14b8a6 100%);
  border: none;

  &:hover {
    background: linear-gradient(135deg, #0f766e 0%, #0d9488 100%);
  }
}

.register-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;

  a {
    color: #0d9488;
    text-decoration: none;
    font-size: 14px;

    &:hover {
      color: #0f766e;
      text-decoration: underline;
    }
  }
}

:deep(.el-input__wrapper) {
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;

  &:hover, &:focus, &.is-focus {
    border-color: #0d9488;
    box-shadow: 0 0 0 2px rgba(13, 148, 136, 0.1);
  }
}

:deep(.el-form-item__label) {
  color: #374151;
}
</style>