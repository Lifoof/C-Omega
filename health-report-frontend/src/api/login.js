import request from '@/utils/request'

// 登录
export function login(data) {
  return request({
    url: '/login',
    method: 'post',
    data: data
  })
}

// 短信登录
export function smsLogin(data) {
  return request({
    url: '/smsLogin',
    method: 'post',
    data: data
  })
}


// 注册
export function userRegister(data) {
  return request({
    url: '/userRegister',
    headers: { isToken: false },
    method: 'post',
    data: data
  })
}

// 发送短信验证码
export function sendSmsCode(data) {
  return request({
    url: '/sendSmsCode',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 发送邮箱验证码
export function sendEmailCode(data) {
  return request({
    url: '/sendEmailCode',
    headers: { isToken: false },
    method: 'post',
    data: data
  })
}

// 重置密码
export function resetPassword(data) {
  return request({
    url: '/resetPassword',
    headers: { isToken: false },
    method: 'post',
    data: data
  })
}

// 验证验证码
export function verifyCode(data) {
  return request({
    url: '/verifyCode',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

// 解锁屏幕
export function unlockScreen(password) {
  return request({
    url: '/unlockscreen',
    method: 'post',
    data: { password }
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}
