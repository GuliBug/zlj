import request from '@/utils/request'
import { unwrapPage } from '@/utils/format'

// AC-14 售后
export function listAftersales(query) {
  return request({ url: '/aftersales', method: 'get', params: query }).then((res) => unwrapPage(res.data))
}

export function getAftersale(no) {
  return request({ url: '/aftersales/' + no, method: 'get' })
}

export function auditAftersale(no, data) {
  return request({ url: '/aftersales/' + no + '/audit', method: 'post', data })
}

export function confirmReturn(no) {
  return request({ url: '/aftersales/' + no + '/confirm-return', method: 'post' })
}

// 幂等退款
export function refundAftersale(no) {
  return request({ url: '/aftersales/' + no + '/refund', method: 'post' })
}
