import request from '@/utils/request'
import { unwrapPage } from '@/utils/format'

// AC-11 SKU 库存总览
export function listSkus(query) {
  return request({ url: '/skus', method: 'get', params: query }).then((res) => unwrapPage(res.data))
}

export function stockInbound(data) {
  return request({ url: '/stock/inbounds', method: 'post', data })
}

export function listInbounds(query) {
  return request({ url: '/stock/inbounds', method: 'get', params: query }).then((res) => unwrapPage(res.data))
}

export function stockFreeze(data) {
  return request({ url: '/stock/freeze', method: 'post', data })
}

export function setThreshold(data) {
  return request({ url: '/stock/threshold', method: 'post', data })
}

export function listFlows(query) {
  return request({ url: '/stock/flows', method: 'get', params: query }).then((res) => unwrapPage(res.data))
}

export function createStocktake(data) {
  return request({ url: '/stock/stocktakes', method: 'post', data })
}

export function listStocktakes(query) {
  return request({ url: '/stock/stocktakes', method: 'get', params: query }).then((res) => unwrapPage(res.data))
}

export function confirmStocktake(id) {
  return request({ url: '/stock/stocktakes/' + id + '/confirm', method: 'post' })
}
