<template>
  <el-dropdown trigger="click" @command="handleSetLanguage">
    <div class="lang-select">
      <svg-icon icon-class="language" class="lang-icon" />
      <span class="lang-text">{{ currentLanguage }}</span>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item :disabled="language === 'zh-CN'" command="zh-CN">
          简体中文
        </el-dropdown-item>
        <el-dropdown-item :disabled="language === 'en-US'" command="en-US">
          English
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale } from '@/i18n'

const { locale } = useI18n()

const language = computed(() => locale.value)

const currentLanguage = computed(() => {
  return locale.value === 'zh-CN' ? '简体中文' : 'English'
})

function handleSetLanguage(lang) {
  setLocale(lang)
  location.reload()
}
</script>

<style lang="scss" scoped>
.lang-select {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
  height: 100%;

  &:hover {
    background-color: rgba(0, 0, 0, 0.05);
  }

  .lang-icon {
    font-size: 18px;
    margin-right: 4px;
  }

  .lang-text {
    font-size: 14px;
    color: #606266;
  }
}
</style>
