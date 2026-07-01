import defaultSettings from '@/settings'
import useSettingsStore from '@/store/modules/settings'
import i18n from '@/i18n'

/**
 * 动态修改标题
 */
export function useDynamicTitle() {
  const settingsStore = useSettingsStore()
  const title = i18n.global.t('common.appName')
  if (settingsStore.dynamicTitle) {
    document.title = settingsStore.title + ' - ' + title
  } else {
    document.title = title
  }
}