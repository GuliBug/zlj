import request from '@/utils/request'

// AC-09 管理端登录
export function login(data) {
  return request({ url: '/login', method: 'post', data })
}

// AC-09 看板
export function getDashboard() {
  return request({ url: '/dashboard', method: 'get' })
}
