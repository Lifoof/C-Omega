import router from '@/router'
import { ElMessageBox } from 'element-plus'
import { login, logout, getInfo, smsLogin } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isHttp, isEmpty } from "@/utils/validate"
import useLockStore from '@/store/modules/lock'
import defAva from '@/assets/images/profile.jpg'
import { startRefreshServerTime, stopRefreshServerTime } from '@/utils/request'

const useUserStore = defineStore(
    'user',
    {
      state: () => ({
        token: getToken(),
        id: '',
        name: '',
        nickName: '',
        avatar: '',
        roles: [],
        permissions: []
      }),
      actions: {
        // 账号密码登录（手机号/邮箱/用户名）
        login(loginData) {
          return new Promise((resolve, reject) => {
            login(loginData).then(res => {
              setToken(res.token)
              this.token = res.token
              console.log(res)
              // ===================== 新增：保存时间戳 =====================
              localStorage.setItem('timeStop', res.timeStop)
              // 2. 启动【5分钟定时任务】拉取最新服务端时间戳
              startRefreshServerTime()
              useLockStore().unlockScreen()
              resolve(res)
            }).catch(error => {
              reject(error)
            })
          })
        },

        // ======================
        // 新增：短信验证码登录
        // ======================
        loginBySms(smsData) {
          return new Promise((resolve, reject) => {
            smsLogin(smsData).then(res => {
              setToken(res.token)
              this.token = res.token
              // ===================== 新增：保存时间戳 =====================
              localStorage.setItem('timeStop', res.timeStop)
              // 2. 启动【5分钟定时任务】拉取最新服务端时间戳
              startRefreshServerTime()
              useLockStore().unlockScreen()
              resolve(res)  // 返回响应数据，供前端获取 roles
            }).catch(error => {
              reject(error)
            })
          })
        },

        // 获取用户信息
        getInfo() {
          return new Promise((resolve, reject) => {
            getInfo().then(res => {
              const user = res.user
              let avatar = user.avatar || ""
              if (!isHttp(avatar)) {
                avatar = (isEmpty(avatar)) ? defAva : import.meta.env.VITE_APP_BASE_API + avatar
              }
              if (res.roles && res.roles.length > 0) {
                this.roles = res.roles
                this.permissions = res.permissions
              } else {
                this.roles = ['ROLE_DEFAULT']
              }
              this.id = user.userId
              this.name = user.userName
              this.nickName = user.nickName
              this.avatar = avatar

              /*if(res.isDefaultModifyPwd) {
                ElMessageBox.confirm('您的密码还是初始密码，请修改密码！', '安全提示', {
                  confirmButtonText: '确定',
                  cancelButtonText: '取消',
                  type: 'warning'
                }).then(() => {
                  router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
                }).catch(() => {})
              }*/
              if(!res.isDefaultModifyPwd && res.isPasswordExpired) {
                ElMessageBox.confirm('您的密码已过期，请尽快修改密码！', '安全提示', {
                  confirmButtonText: '确定',
                  cancelButtonText: '取消',
                  type: 'warning'
                }).then(() => {
                  router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
                }).catch(() => {})
              }
              resolve(res)
            }).catch(error => {
              reject(error)
            })
          })
        },

        // 退出系统
        logOut() {
          return new Promise((resolve, reject) => {
            logout(this.token).then(() => {
              this.token = ''
              this.roles = []
              this.permissions = []
              removeToken()
              // ===================== 新增：清除时间戳 =====================
              localStorage.removeItem('timeStop')
              // ========= 停止定时刷新 =========
              stopRefreshServerTime()
              resolve()
            }).catch(error => {
              reject(error)
            })
          })
        }
      }
    })

export default useUserStore