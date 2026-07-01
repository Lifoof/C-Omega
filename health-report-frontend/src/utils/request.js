import axios from 'axios'
import { ElNotification , ElMessageBox, ElMessage, ElLoading } from 'element-plus'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { tansParams, blobValidate } from '@/utils/ruoyi'
import cache from '@/plugins/cache'
import { saveAs } from 'file-saver'
import useUserStore from '@/store/modules/user'
import i18n from '@/i18n'

let downloadLoadingInstance
// 是否显示重新登录
export let isRelogin = { show: false }

// 获取翻译函数
const t = (key) => i18n.global.t(key)

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'
// 创建axios实例
const service = axios.create({
  // axios中请求配置有baseURL选项，表示请求URL公共部分
  baseURL: import.meta.env.VITE_APP_BASE_API,
  // 超时
  timeout: 120000
})

// 白名单：不携带时间戳、不校验
const whiteList = [
  '/login','/register','/captchaImage','/smsLogin','/sendSmsCode',
  '/sendEmailCode','/userRegister','/resetPassword','/verifyCode','/api/getServerTime', '/profile',
  '/common/download',
  '/common/uploadRoot',
  '/common/uploadModel',
  '/pdf/',
]

// request拦截器
service.interceptors.request.use(config => {
  // 是否需要设置 token
  const isToken = (config.headers || {}).isToken === false
  // 是否需要防止数据重复提交
  const isRepeatSubmit = (config.headers || {}).repeatSubmit === false
  // 间隔时间(ms)，小于此时间视为重复提交
  const interval = (config.headers || {}).interval || 1000
  if (getToken() && !isToken) {
    config.headers['Authorization'] = 'Bearer ' + getToken() // 让每个请求携带自定义token 请根据实际情况自行修改
  }
  // get请求映射params参数
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.params = {}
    config.url = url
  }
  if (!isRepeatSubmit && (config.method === 'post' || config.method === 'put')) {
    const requestObj = {
      url: config.url,
      data: typeof config.data === 'object' ? JSON.stringify(config.data) : config.data,
      time: new Date().getTime()
    }
    const requestSize = Object.keys(JSON.stringify(requestObj)).length // 请求数据大小
    const limitSize = 5 * 1024 * 1024 // 限制存放数据5M
    if (requestSize >= limitSize) {
      console.warn(`[${config.url}]: ` + t('request.dataSizeLimit'))
      return config
    }
    const sessionObj = cache.session.getJSON('sessionObj')
    if (sessionObj === undefined || sessionObj === null || sessionObj === '') {
      cache.session.setJSON('sessionObj', requestObj)
    } else {
      const s_url = sessionObj.url                // 请求地址
      const s_data = sessionObj.data              // 请求数据
      const s_time = sessionObj.time              // 请求时间
      if (s_data === requestObj.data && requestObj.time - s_time < interval && s_url === requestObj.url) {
        const message = t('request.repeatSubmit')
        console.warn(`[${s_url}]: ` + message)
        return Promise.reject(new Error(message))
      } else {
        cache.session.setJSON('sessionObj', requestObj)
      }
    }
  }
  // 白名单接口直接跳过
  const isWhite = whiteList.some(url => config.url.includes(url))
  if (!isWhite) {
    const timeStop = localStorage.getItem('timeStop')
    // 加到请求参数里
    config.params = {
      ...config.params,
      timeStop: timeStop
    }
  }
  return config
}, error => {
  console.log(error)
  Promise.reject(error)
})

// 响应拦截器
service.interceptors.response.use(res => {
      // 请求成功后清理重复提交缓存
      cache.session.remove('sessionObj')
      // 未设置状态码则默认成功状态
      const code = res.data.code || 200
      // 获取错误信息
      const msg = errorCode[code] || res.data.msg || errorCode['default']
      // 二进制数据则直接返回
      if (res.request.responseType ===  'blob' || res.request.responseType ===  'arraybuffer') {
        return res.data
      }
      // ===================== 【新增：时间戳超时处理】=====================
      if (msg.includes('时间戳') || msg.includes('超时') || msg.includes('鉴权失败')) {
        if (!isRelogin.show) {
          isRelogin.show = true
          ElMessageBox.confirm('登录已超时，请重新登录', '系统提示', {
            confirmButtonText: '重新登录',
            cancelButtonText: t('common.cancel'),
            type: 'warning'
          }).then(() => {
            isRelogin.show = false
            useUserStore().logOut().then(() => {
              location.href = '/login'
            })
          }).catch(() => {
            isRelogin.show = false
          })
        }
        return Promise.reject(msg)
      }
      if (code === 401) {
        if (!isRelogin.show) {
          isRelogin.show = true
          ElMessageBox.confirm(t('request.sessionExpired'), t('request.systemPrompt'), { confirmButtonText: t('request.relogin'), cancelButtonText: t('common.cancel'), type: 'warning' }).then(() => {
            isRelogin.show = false
            useUserStore().logOut().then(() => {
              location.href = '/index'
            })
          }).catch(() => {
            isRelogin.show = false
          })
        }
        return Promise.reject(t('request.invalidSession'))
      } else if (code === 500) {
        ElMessage({ message: msg, type: 'error' })
        return Promise.reject(new Error(msg))
      } else if (code === 601) {
        ElMessage({ message: msg, type: 'warning' })
        return Promise.reject(new Error(msg))
      } else if (code !== 200) {
        ElNotification.error({ title: msg })
        return Promise.reject('error')
      } else {
        return  Promise.resolve(res.data)
      }
    },
    error => {
      // 请求失败也清理缓存，避免缓存污染
      cache.session.remove('sessionObj')
      console.log('err' + error)
      let { message } = error
      if (message == "Network Error") {
        message = t('request.networkError')
      } else if (message.includes("timeout")) {
        message = t('request.timeout')
      } else if (message.includes("Request failed with status code")) {
        message = t('request.statusCodeError', { code: message.slice(-3) })
      }
      ElMessage({ message: message, type: 'error', duration: 5 * 1000 })
      return Promise.reject(error)
    }
)

// 通用下载方法
export function download(url, params, filename, config) {
  downloadLoadingInstance = ElLoading.service({ text: t('request.downloading'), background: "rgba(0, 0, 0, 0.7)", })
  const defaultConfig = {
    timeout: 60000, // 超时时间 60 秒
    ...config
  }
  return service.post(url, params, {
    transformRequest: [(params) => { return tansParams(params) }],
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    responseType: 'blob',
    ...defaultConfig
  }).then(async (data) => {
    const isBlob = blobValidate(data)
    if (isBlob) {
      const blob = new Blob([data])
      saveAs(blob, filename)
    } else {
      const resText = await data.text()
      const rspObj = JSON.parse(resText)
      const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
      ElMessage.error(errMsg)
    }
    downloadLoadingInstance.close()
  }).catch((r) => {
    console.error(r)
    ElMessage.error(t('request.downloadError'))
    downloadLoadingInstance.close()
  })
}
// ===================== 定时刷新服务器时间戳（防6分钟超时）=====================
// 5分钟刷新一次（后端6分钟过期，留1分钟安全缓冲）
export function startRefreshServerTime() {
  // 立即执行一次
  refreshTimeStamp()
  // 开启全局定时任务
  window.timeTask = setInterval(() => {
    refreshTimeStamp()
  }, 5 * 60 * 1000)
}

// 停止刷新（退出登录时用）
export function stopRefreshServerTime() {
  if (window.timeTask) {
    clearInterval(window.timeTask)
    window.timeTask = null
  }
}

// 请求后端获取最新时间戳
function refreshTimeStamp() {
  service.get('/api/getServerTime').then(res => {
    if (res) {
      localStorage.setItem('timeStop', res)
    }
  }).catch(err => {
    console.error('刷新时间戳失败', err)
  })
}
export default service
