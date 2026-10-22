import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'
import router from '../router'

// 创建 axios 实例：baseURL 为 /admin-api，与契约唯一来源保持一致
const service = axios.create({
  baseURL: '/admin-api',
  timeout: 15000
})

// 请求拦截器：自动携带 X-Token
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['X-Token'] = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理 AjaxResult{code,msg,data}
service.interceptors.response.use(
  (res) => {
    // 二进制（CSV 导出等）直接返回
    if (res.request.responseType === 'blob' || res.request.responseType === 'arraybuffer') {
      return res.data
    }
    const code = res.data.code ?? 200
    const msg = res.data.msg || '请求失败'
    if (code !== 200) {
      if (code === 401) {
        removeToken()
        router.push('/login')
        ElMessage.error('登录已失效，请重新登录')
      } else {
        ElMessage.error(msg)
      }
      return Promise.reject(new Error(msg))
    }
    return res.data
  },
  (error) => {
    let message = error.message
    if (message === 'Network Error') {
      message = '后端接口连接异常'
    } else if (message.includes('timeout')) {
      message = '系统接口请求超时'
    } else if (message.includes('Request failed with status code')) {
      message = '系统接口 ' + message.slice(-3) + ' 异常'
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
