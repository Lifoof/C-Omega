<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, index) in levelList" :key="item.path">
        <span v-if="item.redirect === 'noRedirect' || index == levelList.length - 1" class="no-redirect">{{ getTranslatedTitle(item.meta.title) }}</span>
        <a v-else @click.prevent="handleLink(item)">{{ getTranslatedTitle(item.meta.title) }}</a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup>
import usePermissionStore from '@/store/modules/permission'
import useUserStore from '@/store/modules/user'
import { useI18n } from 'vue-i18n'
const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const userStore = useUserStore()
const { t } = useI18n()
const levelList = ref([])

// 菜单标题映射：中文 -> i18n key
const menuTitleMap = {
  '首页': 'menu.dashboard',
  '管理看板': 'menu.dashboard',
  '系统管理': 'menu.system',
  '用户信息': 'menu.personalInfo',
  '个人信息': 'menu.personalInfo',
  '体检录入': 'menu.collection',
  '信息采集': 'menu.collection',
  '生成报告': 'collection.generateReport',
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

// 获取翻译后的标题
function getTranslatedTitle(title) {
  // 如果是中文，转换为i18n key
  if (menuTitleMap[title]) {
    return t(menuTitleMap[title])
  }
  // 否则直接翻译（如果是i18n key）
  return t(title)
}

function getBreadcrumb() {
  // only show routes with meta.title
  let matched = []
  const pathNum = findPathNum(route.path)
  // multi-level menu
  if (pathNum > 2) {
    const reg = /\/\w+/gi
    const pathList = route.path.match(reg).map((item, index) => {
      if (index !== 0) item = item.slice(1)
      return item
    })
    getMatched(pathList, permissionStore.defaultRoutes, matched)
  } else {
    matched = route.matched.filter((item) => item.meta && item.meta.title)
  }
  // 判断是否为首页
  if (!isDashboard(matched[0])) {
    const isAdmin = userStore.roles?.includes('admin') ?? false
    if (isAdmin){
      matched = [{ path: "/index", meta: { title: "管理看板" } }].concat(matched)
    }else {
      matched = [{ path: "/personalInfo", meta: { title: "用户信息" } }].concat(matched)
    }


  }
  levelList.value = matched.filter(item => item.meta && item.meta.title && item.meta.breadcrumb !== false)
}
function findPathNum(str, char = "/") {
  let index = str.indexOf(char)
  let num = 0
  while (index !== -1) {
    num++
    index = str.indexOf(char, index + 1)
  }
  return num
}
function getMatched(pathList, routeList, matched) {
  let data = routeList.find(item => item.path == pathList[0] || (item.name += '').toLowerCase() == pathList[0])
  if (data) {
    matched.push(data)
    if (data.children && pathList.length) {
      pathList.shift()
      getMatched(pathList, data.children, matched)
    }
  }
}
function isDashboard(route) {
  const name = route && route.name
  if (!name) {
    return false
  }
  return name.trim() === 'Index'
}
function handleLink(item) {
  const { redirect, path } = item
  if (redirect) {
    router.push(redirect)
    return
  }
  router.push(path)
}

watchEffect(() => {
  // if you go to the redirect page, do not update the breadcrumbs
  if (route.path.startsWith('/redirect/')) {
    return
  }
  getBreadcrumb()
})
getBreadcrumb()
</script>

<style lang='scss' scoped>
.app-breadcrumb.el-breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 50px;

  .no-redirect {
    color: #97a8be;
    cursor: text;
  }
}
</style>