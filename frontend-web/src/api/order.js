import request from '@/utils/request'
import { unwrapPage } from '@/utils/format'

// AC-13 订单列表（shipOverdue 超24h未发货）
export function listOrders(query) {
  return request({ url: '/orders', method: 'get', params: query }).then((res) => unwrapPage(res.data))
}

export function shipOrder(orderNo, data) {
  return request({ url: '/orders/' + orderNo + '/ship', method: 'post', data })
}

export function batchShip(rows) {
  return request({ url: '/orders/batch-ship', method: 'post', data: { rows } })
}

// 导出 CSV（blob 下载）
export function exportOrders(params) {
  return request({ url: '/orders/export', method: 'get', params, responseType: 'blob' })
}
