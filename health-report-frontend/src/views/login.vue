<template>
  <div class="auth-page-shell login-page">
    <div class="lang-switch-wrapper">
      <lang-select />
    </div>

    <div class="login-container">
<!--      &lt;!&ndash; 左侧图片区域 &ndash;&gt;
      <div class="left-image-area">
        <img src="@/assets/images/medical/health-system.png" alt="医疗体检系统" class="medical-img" />
      </div>-->

      <!-- 中间间隔 -->
<!--      <div class="spacer"></div>-->

      <!-- 右侧：原来的登录区域（完全保持不变） -->
      <div class="login-box-wrapper">
        <!-- 品牌信息 -->
        <div class="brand-section">
          <div class="logo">
            <el-icon :size="48" color="#14b8a6"><FirstAidKit /></el-icon>
          </div>
          <h1 class="brand-title">{{ $t('auth.brandTitle') }}</h1>
          <p class="brand-desc">{{ $t('auth.brandDesc') }}</p>

          <div class="features-list">
            <div class="feature-item">
              <img src="@/assets/images/medical/blood-test.png" class="feature-icon" />
              <span>{{ $t('auth.feature1') }}</span>
            </div>
            <div class="feature-item">
              <img src="@/assets/images/medical/chart.png" class="feature-icon" />
              <span>{{ $t('auth.feature2') }}</span>
            </div>
            <div class="feature-item">
              <img src="@/assets/images/medical/consultation.png" class="feature-icon" />
              <span>{{ $t('auth.feature3') }}</span>
            </div>
            <div class="feature-item">
              <img src="@/assets/images/medical/ultrasound.png" class="feature-icon" />
              <span>{{ $t('auth.feature4') }}</span>
            </div>
          </div>
        </div>

        <!-- 登录卡片 -->
        <div class="auth-card login-card">
          <h2 class="auth-title login-title">{{ $t('auth.title') }}</h2>

          <el-tabs v-model="loginTab" class="login-tabs" stretch>
            <!-- 手机号登录 -->
            <el-tab-pane :label="$t('auth.phoneLogin')" name="phone">
              <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules" @submit.prevent="handlePhoneLogin">
                <el-form-item prop="phone">
                  <el-input v-model="phoneForm.phone" :placeholder="$t('auth.phonePlaceholder')" prefix-icon="Iphone" size="large" />
                </el-form-item>

                <el-tabs v-model="phoneLoginType" class="inner-tabs" type="border-card">
                  <!-- 手机号 + 验证码 -->
                  <el-tab-pane :label="$t('auth.codeLogin')" name="code">
                    <el-form-item prop="code">
                      <div class="code-input">
                        <el-input v-model="phoneForm.code" :placeholder="$t('auth.codePlaceholder')" prefix-icon="Message" size="large" />
                        <el-button :disabled="codeCooldown > 0" @click="sendPhoneCode" type="primary" plain size="large">
                          {{ codeCooldown > 0 ? `${codeCooldown}s` : $t('auth.sendCode') }}
                        </el-button>
                      </div>
                    </el-form-item>
                  </el-tab-pane>

                  <!-- 手机号 + 密码 -->
                  <el-tab-pane :label="$t('auth.passwordLogin')" name="password">
                    <el-form-item prop="password">
                      <el-input v-model="phoneForm.password" type="password" :placeholder="$t('auth.passwordPlaceholder')" prefix-icon="Lock" show-password size="large" />
                    </el-form-item>
                  </el-tab-pane>
                </el-tabs>

                <el-form-item>
                  <el-button type="primary" class="login-btn" native-type="submit" :loading="loading" size="large">{{ $t('auth.loginBtn') }}</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <!-- 邮箱登录 -->
            <el-tab-pane :label="$t('auth.emailLogin')" name="email">
              <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" @submit.prevent="handleEmailLogin">
                <el-form-item prop="email">
                  <el-input v-model="emailForm.email" :placeholder="$t('auth.emailPlaceholder')" prefix-icon="Message" size="large" />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input v-model="emailForm.password" type="password" :placeholder="$t('auth.passwordPlaceholder')" prefix-icon="Lock" show-password size="large" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" class="login-btn" native-type="submit" :loading="loading" size="large">{{ $t('auth.loginBtn') }}</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <div class="login-footer">
            <router-link class="auth-link" to="/forgot-password">{{ $t('auth.forgotPassword') }}</router-link>
            <span class="divider">|</span>
            <router-link class="auth-link" to="/register">{{ $t('auth.goToRegister') }}</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive, onUnmounted, getCurrentInstance} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import useUserStore from '@/store/modules/user'
import {sendSmsCode} from "@/api/login"
import LangSelect from '@/components/LangSelect'
import {FirstAidKit} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const {proxy} = getCurrentInstance()

const loginTab = ref('phone')
const phoneLoginType = ref('code')
const loading = ref(false)
const codeCooldown = ref(0)
let cooldownTimer = null

const phoneForm = reactive({phone: '', code: '', password: ''})
const emailForm = reactive({email: '', password: ''})
const phoneFormRef = ref()
const emailFormRef = ref()
const redirect = ref(undefined)

watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, {immediate: true})

const phoneRules = {
  phone: [
    {required: true, pattern: /^1[3-9]\d{9}$/, message: proxy.$t('auth.phoneInvalid'), trigger: 'blur'}
  ],
  code: [{
    validator: (_, value, callback) => {
      if (phoneLoginType.value === 'code' && !value) {
        callback(new Error(proxy.$t('auth.codeRequired')))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }],
  password: [{
    validator: (_, value, callback) => {
      if (phoneLoginType.value === 'password' && !value) {
        callback(new Error(proxy.$t('auth.passwordRequired')))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }]
}

const emailRules = {
  email: [{required: true, type: 'email', message: proxy.$t('auth.emailInvalid'), trigger: 'blur'}],
  password: [{required: true, min: 6, max: 20, message: proxy.$t('auth.passwordLength'), trigger: 'blur'}],
}

async function sendPhoneCode() {
  const p = String(phoneForm.phone).trim()
  if (!p || !/^1[3-9]\d{9}$/.test(p)) {
    proxy.$modal.msgError(proxy.$t('auth.phoneInvalid'))
    return
  }
  try {
    await sendSmsCode({
      mobile: p,
      smsmode: "0"
    })
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

async function handlePhoneLogin() {
  const valid = await phoneFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const phone = String(phoneForm.phone).trim()
    const query = route.query
    const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
      if (cur !== "redirect") {
        acc[cur] = query[cur]
      }
      return acc
    }, {})
    let redirectPath = redirect.value || '/'
    let res
    if (phoneLoginType.value === 'code') {
      res = await userStore.loginBySms({
        phone: phone,
        smsCode: phoneForm.code
      })
    } else {
      res = await userStore.login({
        username: phone,
        password: phoneForm.password
      })
    }
    // 登录成功后，直接判断
    const isAdmin = res.roles.includes('admin')
    if (!isAdmin) {
      redirectPath = '/personalInfo'
    } else {
      redirectPath = '/'
    }
    router.push({path: redirectPath, query: otherQueryParams})
  } catch (error) {
    loading.value = false
    // 拦截器已处理错误提示，此处不再重复显示
  }
}

async function handleEmailLogin() {
  const valid = await emailFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const query = route.query
    const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
      if (cur !== "redirect") {
        acc[cur] = query[cur]
      }
      return acc
    }, {})
    let redirectPath = redirect.value || '/'
    const res = await userStore.login({
      username: emailForm.email,
      password: emailForm.password
    })
    // 登录成功后，直接判断
    const isAdmin = res.roles.includes('admin')
    if (!isAdmin) {
      redirectPath = '/personalInfo'
    } else {
      redirectPath = '/'
    }
    router.push({path: redirectPath, query: otherQueryParams})
  } catch (error) {
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
  background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 50%, #0d9488 100%);
  padding: 20px;
  position: relative;
}

.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 900px;
  min-height: 600px;
}

/*
// 左侧图片区域（修改后）
.left-image-area {
  // 改为按内容自适应高度，固定宽度，保证比例
  width: 400px;
  // 移除固定高度，让高度随图片自适应
  // height: 700px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  // 给容器加一个最大高度，避免在大屏上过度拉伸
  max-height: 700px;

  .medical-img {
    // 关键：保证图片100%按原始比例显示，不拉伸
    width: 100%;
    height: auto;
    object-fit: contain;
    filter: drop-shadow(0 10px 30px rgba(0, 0, 0, 0.3));
  }
}

// 中间间隔
.spacer {
  width: 80px;
  flex-shrink: 0;
}
*/

// 右侧登录区域包装器
.login-box-wrapper {
  display: flex;
  flex: 1;
  max-width: 900px;
  min-height: 600px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

// 品牌区域
.brand-section {
  flex: 1;
  padding: 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.8) 0%, rgba(13, 148, 136, 0.3) 100%);
}

.logo {
  width: 64px;
  height: 64px;
  background: rgba(20, 184, 166, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  border: 2px solid rgba(20, 184, 166, 0.3);
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 16px;
  color: #fff;
  letter-spacing: 2px;
}

.brand-desc {
  font-size: 15px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 40px;
}

.features-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.9);
}

.feature-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
  filter: brightness(0) saturate(100%) invert(72%) sepia(10%) saturate(2400%) hue-rotate(125deg);
}

// 登录卡片 - 保持原有样式
.auth-card {
  width: 400px;
  background: rgba(255, 255, 255, 0.98);
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-title {
  text-align: center;
  color: #0f172a;
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 32px;
}

.login-tabs {
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

.inner-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }

  :deep(.el-tabs__content) {
    padding: 0;
  }

  :deep(.el-tabs__item) {
    font-size: 14px;
  }
}

.code-input {
  display: flex;
  gap: 12px;

  .el-input {
    flex: 1;
  }

  .el-button {
    min-width: 110px;
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
  background: #0d9488;
  border: none;
  margin-top: 8px;

  &:hover {
    background: #0f766e;
  }
}

.login-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
  font-size: 14px;
  color: #64748b;

  .auth-link {
    color: #0d9488;
    text-decoration: none;

    &:hover {
      color: #0f766e;
      text-decoration: underline;
    }
  }

  .divider {
    color: #cbd5e1;
    margin: 0 12px;
  }
}

:deep(.el-input__wrapper) {
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  border: 1px solid #e2e8f0;

  &:hover, &.is-focus {
    border-color: #0d9488;
    box-shadow: 0 0 0 2px rgba(13, 148, 136, 0.1);
  }
}

.lang-switch-wrapper {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 10;

  :deep(.lang-select) {
    background: rgba(255, 255, 255, 0.9);
    border-radius: 8px;
    padding: 8px 16px;

    .lang-text {
      color: #0f172a;
    }
  }
}

@media (max-width: 1200px) {
  .left-image-area {
    width: 300px;
  }

  .spacer {
    width: 40px;
  }
}

@media (max-width: 1000px) {
  .left-image-area {
    display: none;
  }

  .spacer {
    display: none;
  }

  .login-box-wrapper {
    max-width: 900px;
    margin: 0 auto;
  }
}

@media (max-width: 768px) {
  .login-box-wrapper {
    flex-direction: column;
    max-width: 420px;
  }

  .brand-section {
    display: none;
  }

  .auth-card {
    width: 100%;
  }
}
</style>
