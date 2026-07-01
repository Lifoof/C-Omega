<template>
  <el-form ref="pwdRef" :model="user" :rules="rules" label-width="80px">
    <el-form-item :label="$t('system.profile.oldPwd')" prop="oldPassword">
      <el-input v-model="user.oldPassword" :placeholder="$t('system.profile.oldPwdPlaceholder')" type="password" show-password />
    </el-form-item>
    <el-form-item :label="$t('system.profile.newPwd')" prop="newPassword">
      <el-input v-model="user.newPassword" :placeholder="$t('system.profile.newPwdPlaceholder')" type="password" show-password />
    </el-form-item>
    <el-form-item :label="$t('system.profile.confirmPwd')" prop="confirmPassword">
      <el-input v-model="user.confirmPassword" :placeholder="$t('system.profile.confirmPwdPlaceholder')" type="password" show-password/>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submit">{{ $t('system.profile.save') }}</el-button>
      <el-button type="danger" @click="close">{{ $t('system.profile.close') }}</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { updateUserPwd } from "@/api/system/user"
import { useI18n } from 'vue-i18n'

const { t: $t } = useI18n()
const { proxy } = getCurrentInstance()

const user = reactive({
  oldPassword: undefined,
  newPassword: undefined,
  confirmPassword: undefined
})

const equalToPassword = (rule, value, callback) => {
  if (user.newPassword !== value) {
    callback(new Error($t('system.profile.pwdMismatch')))
  } else {
    callback()
  }
}

const rules = ref({
  oldPassword: [{required: true, message: () => $t('system.profile.oldPwdRequired'), trigger: "blur"}],
  newPassword: [{required: true, message: () => $t('system.profile.newPwdRequired'), trigger: "blur"}, {
    min: 6,
    max: 20,
    message: () => $t('system.profile.pwdLength'),
    trigger: "blur"
  }, {pattern: /^[^<>"'|\\]+$/, message: () => $t('system.profile.pwdInvalid'), trigger: "blur"}],
  confirmPassword: [{
    required: true,
    message: () => $t('system.profile.confirmPwdRequired'),
    trigger: "blur"
  }, {required: true, validator: equalToPassword, trigger: "blur"}]
})

/** 提交按钮 */
function submit() {
  proxy.$refs.pwdRef.validate(valid => {
    if (valid) {
      updateUserPwd(user.oldPassword, user.newPassword).then(() => {
        proxy.$modal.msgSuccess($t('system.profile.updateSuccess'))
      })
    }
  })
}

/** 关闭按钮 */
function close() {
  proxy.$tab.closePage()
}
</script>
