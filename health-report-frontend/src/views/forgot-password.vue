<template>
  <div class="auth-page-shell forgot-page">
    <div class="auth-card auth-card--wide forgot-card">
      <h2 class="auth-title forgot-title">{{ $t('auth.resetPassword') }}</h2>
      <el-steps :active="currentStep" finish-status="success" align-center style="margin-bottom: 24px;">
        <el-step :title="$t('auth.selectRecoveryMethod')" />
        <el-step :title="$t('auth.verifyIdentity')" />
        <el-step :title="$t('auth.setNewPassword')" />
        <el-step :title="$t('auth.complete')" />
      </el-steps>

      <div v-if="currentStep === 0" class="step-content">
        <el-radio-group v-model="recoveryType" class="recovery-options">
          <el-radio label="phone" size="large">{{ $t('auth.phoneRecovery') }}</el-radio>
          <el-radio label="email" size="large">{{ $t('auth.emailRecovery') }}</el-radio>
        </el-radio-group>
        <el-button type="primary" @click="currentStep = 1" class="step-btn">{{ $t('common.nextStep') }}</el-button>
      </div>

      <div v-if="currentStep === 1" class="step-content">
        <el-form ref="verifyFormRef" :model="form" :rules="verifyRules">
          <el-form-item prop="target">
            <el-input v-model="form.target" :placeholder="recoveryType === 'phone' ? $t('auth.phone') : $t('auth.email')" />
          </el-form-item>
          <el-form-item prop="code">
            <div class="code-input">
              <el-input v-model="form.code" :placeholder="$t('auth.verificationCode')" />
              <el-button :disabled="codeCooldown > 0" @click="handleSendCode" type="primary" plain>
                {{ codeCooldown > 0 ? $t('auth.resendCode', { seconds: codeCooldown }) : $t('auth.sendCode') }}
              </el-button>
            </div>
          </el-form-item>
        </el-form>
        <div class="step-actions">
          <el-button @click="handlePrevStep">{{ $t('common.prevStep') }}</el-button>
          <el-button type="primary" @click="verifyCode">{{ $t('common.nextStep') }}</el-button>
        </div>
      </div>

      <div v-if="currentStep === 2" class="step-content">
        <el-form ref="passwordFormRef" :model="form" :rules="passwordRules">
          <el-form-item prop="newPassword">
            <el-input v-model="form.newPassword" type="password" :placeholder="$t('auth.newPassword')" show-password />
          </el-form-item>
          <el-form-item prop="confirmNewPassword">
            <el-input v-model="form.confirmNewPassword" type="password" :placeholder="$t('auth.confirmNewPassword')" show-password />
          </el-form-item>
        </el-form>
        <el-button type="primary" @click="handleResetPassword" :loading="loading" class="step-btn">{{ $t('common.submit') }}</el-button>
      </div>

      <div v-if="currentStep === 3" class="step-content success-step">
        <el-result icon="success" :title="$t('auth.resetSuccess')">
          <template #extra>
            <el-button type="primary" @click="router.push('/login')">{{ $t('auth.login') }}</el-button>
          </template>
        </el-result>
      </div>

      <div class="forgot-footer" v-if="currentStep < 3">
        <router-link to="/login">{{ $t('auth.goToLogin') }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive, onUnmounted} from 'vue'
import {useRouter} from 'vue-router'
import {sendSmsCode, sendEmailCode, resetPassword, verifyCode as verifyCodeApi} from "@/api/login"

const router = useRouter()
const {proxy} = getCurrentInstance()

const currentStep = ref(0)
const recoveryType = ref('phone')
const loading = ref(false)
const codeCooldown = ref(0)

const form = reactive({
  target: '',
  code: '',
  newPassword: '',
  confirmNewPassword: ''
})

const verifyFormRef = ref()
const passwordFormRef = ref()
let cooldownTimer = null

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error(proxy.$t('auth.confirmPasswordRequired')))
  } else if (value !== form.newPassword) {
    callback(new Error(proxy.$t('auth.passwordMismatch')))
  } else {
    callback()
  }
}

const validateTarget = (rule, value, callback) => {
  if (!value) {
    const message = recoveryType.value === 'phone'
        ? proxy.$t('auth.phoneRequired')
        : proxy.$t('auth.emailRequired')
    callback(new Error(message))
  } else {
    // 格式验证
    if (recoveryType.value === 'phone') {
      const phoneRegex = /^1[3-9]\d{9}$/
      if (!phoneRegex.test(value)) {
        callback(new Error(proxy.$t('auth.phoneInvalid')))
        return
      }
    } else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(value)) {
        callback(new Error(proxy.$t('auth.emailInvalid')))
        return
      }
    }
    callback()
  }
}

const verifyRules = {
  target: [{validator: validateTarget, trigger: 'blur'}],
  code: [{required: true, message: proxy.$t('auth.codeRequired'), trigger: 'blur'}],
}

const passwordRules = {
  newPassword: [{required: true, min: 6, max: 20, message: proxy.$t('auth.passwordLength'), trigger: 'blur'}],
  confirmNewPassword: [{required: true, validator: validateConfirmPassword, trigger: 'blur'}],
}

// 上一步（清空表单数据）
function handlePrevStep() {
  form.target = ''
  form.code = ''
  codeCooldown.value = 0
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = null
  }
  currentStep.value = 0
}

// 发送验证码
async function handleSendCode() {
  // 验证是否填写
  if (!form.target) {
    const message = recoveryType.value === 'phone'
        ? proxy.$t('auth.phoneRequired')
        : proxy.$t('auth.emailRequired')
    proxy.$modal.msgError(message)
    return
  }

  // 验证格式
  if (recoveryType.value === 'phone') {
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(form.target)) {
      proxy.$modal.msgError(proxy.$t('auth.phoneInvalid'))
      return
    }
  } else {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(form.target)) {
      proxy.$modal.msgError(proxy.$t('auth.emailInvalid'))
      return
    }
  }

  try {
    if (recoveryType.value === 'phone') {
      // 手机验证码 → 适配后端 /sms 接口
      await sendSmsCode({
        mobile: form.target,
        smsmode: "2" // 2=忘记密码
      })
    } else {
      // 邮箱验证码
      await sendEmailCode({
        email: form.target,
        type: "reset"
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

async function verifyCode() {
  const valid = await verifyFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // 调用后端验证码验证接口
    await verifyCodeApi({
      target: form.target,
      code: form.code,
      type: recoveryType.value
    })
    currentStep.value = 2
  } catch (error) {
    // 拦截器已处理错误提示，此处不再重复显示
  } finally {
    loading.value = false
  }
}

// 重置密码
async function handleResetPassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await resetPassword({
      target: form.target,
      code: form.code,
      newPassword: form.newPassword,
      type: recoveryType.value
    })
    currentStep.value = 3
  } catch (error) {
    loading.value = false
    proxy.$modal.msgError("重置失败：" + error.message)
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
  max-width: 560px;
}

.auth-title {
  text-align: center;
  color: #0f172a;
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 30px;
}

.step-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.recovery-options {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin: 20px 0;

  :deep(.el-radio__input.is-checked + .el-radio__label) {
    color: #0d9488;
  }

  :deep(.el-radio__input.is-checked .el-radio__inner) {
    border-color: #0d9488;
    background: #0d9488;
  }
}

.code-input {
  display: flex;
  gap: 12px;

  .el-input {
    flex: 1;
  }

  .el-button {
    min-width: 120px;
  }
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.step-btn {
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

.success-step {
  padding: 20px 0;
}

.forgot-footer {
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

:deep(.el-steps) {
  .el-step__head.is-success {
    color: #0d9488;
    border-color: #0d9488;
  }

  .el-step__title.is-success {
    color: #0d9488;
  }

  .el-step__head.is-process {
    color: #0d9488;
    border-color: #0d9488;
  }

  .el-step__title.is-process {
    color: #0d9488;
    font-weight: 500;
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
</style>