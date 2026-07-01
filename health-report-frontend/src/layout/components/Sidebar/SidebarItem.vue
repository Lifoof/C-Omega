<template>
  <div v-if="!item.hidden">
    <template v-if="hasOneShowingChild(item.children, item) && (!onlyOneChild.children || onlyOneChild.noShowingChildren) && !item.alwaysShow">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path, onlyOneChild.query)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)" :class="{ 'submenu-title-noDropdown': !isNest }">
          <svg-icon :icon-class="onlyOneChild.meta.icon || (item.meta && item.meta.icon)"/>
          <template #title><span class="menu-title" :title="hasTitle(onlyOneChild.meta.title)">{{ getTranslatedTitle(onlyOneChild.meta.title) }}</span></template>
        </el-menu-item>
      </app-link>
    </template>

    <el-sub-menu v-else ref="subMenu" :index="resolvePath(item.path)" teleported>
      <template v-if="item.meta" #title>
        <svg-icon :icon-class="item.meta && item.meta.icon" />
        <span class="menu-title" :title="hasTitle(item.meta.title)">{{ getTranslatedTitle(item.meta.title) }}</span>
      </template>

      <sidebar-item
          v-for="(child, index) in item.children"
          :key="child.path + index"
          :is-nest="true"
          :item="child"
          :base-path="resolvePath(child.path)"
          class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { isExternal } from '@/utils/validate'
import AppLink from './Link'
import { getNormalPath } from '@/utils/ruoyi'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  // route object
  item: {
    type: Object,
    required: true
  },
  isNest: {
    type: Boolean,
    default: false
  },
  basePath: {
    type: String,
    default: ''
  }
})

const { t } = useI18n()
const onlyOneChild = ref({})

// 菜单标题映射：中文 -> i18n key
const menuTitleMap = {
  '首页': 'menu.dashboard',
  '管理看板': 'menu.dashboard',
  '系统管理': 'menu.system',
  '用户信息': 'menu.personalInfo',
  '个人信息': 'menu.personalInfo',
  '体检录入': 'menu.collection',
  '信息采集': 'menu.collection',
  '模型管理': 'menu.model',
  '报告管理': 'menu.report',
  '用户管理': 'menu.user',
  '角色管理': 'menu.role',
  '菜单管理': 'menu.menu',
  '部门管理': 'menu.dept',
  '字典管理': 'menu.dict',
  '参数设置': 'menu.config',
  '日志管理': 'menu.monitor',
  '操作日志': 'menu.operlog',
  '登录日志': 'menu.logininfor',
  '定时任务': 'menu.job',
  '任务日志': 'menu.joblog',
  '工具管理': 'menu.tool',
  '代码生成': 'menu.gen',
  '系统接口': 'menu.swagger',
  '表单构建': 'menu.form',
  '指标管理': 'menu.indicator',
  '指标年龄阈值': 'menu.ageThreshold',
  '帮助中心':'menu.helpCenter',
}

// 获取翻译后的菜单标题
function getTranslatedTitle(title) {
  // 如果是中文，转换为i18n key
  if (menuTitleMap[title]) {
    return t(menuTitleMap[title])
  }
  // 否则直接翻译（如果是i18n key）
  return t(title)
}

function hasOneShowingChild(children = [], parent) {
  if (!children) {
    children = []
  }
  const showingChildren = children.filter(item => {
    if (item.hidden) {
      return false
    }
    onlyOneChild.value = item
    return true
  })

  // When there is only one child router, the child router is displayed by default
  if (showingChildren.length === 1) {
    return true
  }

  // Show parent if there are no child router to display
  if (showingChildren.length === 0) {
    onlyOneChild.value = { ...parent, path: '', noShowingChildren: true }
    return true
  }

  return false
}

function resolvePath(routePath, routeQuery) {
  if (isExternal(routePath)) {
    return routePath
  }
  if (isExternal(props.basePath)) {
    return props.basePath
  }
  if (routeQuery) {
    let query = JSON.parse(routeQuery)
    return { path: getNormalPath(props.basePath + '/' + routePath), query: query }
  }
  return getNormalPath(props.basePath + '/' + routePath)
}

function hasTitle(title){
  if (title.length > 5) {
    return title
  } else {
    return ""
  }
}
</script>
