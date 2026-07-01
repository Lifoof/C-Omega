<template>
  <div class="page-container">
    <div class="page-header">
      <h3>{{ isEdit ? $t('personalInfo.editTitle') : $t('personalInfo.createTitle') }}</h3>
      <el-button @click="$router.back()">{{ $t('common.back') }}</el-button>
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
        <el-form-item :label="$t('personalInfo.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="3" :placeholder="$t('personalInfo.remark')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading">{{ $t('common.save') }}</el-button>
          <el-button @click="$router.back()">{{ $t('common.cancel') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance } from 'element-plus'
import { getMemberInfo as getPersonalInfoById, addMemberInfo as createPersonalInfo, updateMemberInfo } from "@/api/healthReport/personalInfo"

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  memberNo: '',
  name: '',
  relationship: 1,
  gender: 1,
  birthDate: '',
  country: 'CN',
  city: '',
  district: '',
  contact: '',
  address: '',
  ethnicity: '',
  birthplace: '',
  remark: '',
})

const { t: $t } = useI18n()

const rules = {
  name: [{ required: true, message: () => $t('personalInfo.nameRequired'), trigger: 'blur' }],
  relationship: [{ required: true, message: () => $t('personalInfo.relationshipRequired'), trigger: 'change' }],
  gender: [{ required: true, message: () => $t('personalInfo.genderRequired'), trigger: 'change' }],
  birthDate: [{ required: true, message: () => $t('personalInfo.birthDateRequired'), trigger: 'change' }],
}

// ===== 国家/城市/地区 级联数据 =====
interface CountryOption { code: string; label: string }
const countryList: CountryOption[] = [
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

const regionData: Record<string, Record<string, string[]>> = {
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
  if (!form.country || !regionData[form.country]) return []
  return Object.keys(regionData[form.country])
})

const districtList = computed(() => {
  if (!form.country || !form.city || !regionData[form.country]) return []
  return regionData[form.country][form.city] || []
})

function onCountryChange() {
  form.city = ''
  form.district = ''
}

function onCityChange() {
  form.district = ''
}

async function loadData() {
  if (!isEdit.value) return
  try {
    const res = await getPersonalInfoById(route.params.id as string)
    Object.assign(form, res.data?.data || res.data)
  } catch { /* handled */ }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    if (isEdit.value) {
      await updatePersonalInfo(form)
    } else {
      await createPersonalInfo(form)
    }
    ElMessage.success($t('common.saveSuccess'))
    router.push('/personalInfo')
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.page-container {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
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
