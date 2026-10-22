import request from '@/utils/request'
import { unwrapPage } from '@/utils/format'

// AC-12 门店
export function listStores() {
  return request({ url: '/stores', method: 'get' })
}

export function createStore(data) {
  return request({ url: '/stores', method: 'post', data })
}

export function updateStore(id, data) {
  return request({ url: '/stores/' + id, method: 'put', data })
}

// 核销
export function verifyOrder(data) {
  return request({ url: '/verify', method: 'post', data })
}

export function listVerifyRecords(query) {
  return request({ url: '/verify-records', method: 'get', params: query }).then((res) => unwrapPage(res.data))
}
