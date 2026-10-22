import request from '@/utils/request'
import { unwrapPage } from '@/utils/format'

// AC-10 商品列表（含 SKU 摘要）
export function listProducts(query) {
  return request({ url: '/products', method: 'get', params: query }).then((res) => unwrapPage(res.data))
}

export function getProduct(id) {
  return request({ url: '/products/' + id, method: 'get' })
}

export function createProduct(data) {
  return request({ url: '/products', method: 'post', data })
}

export function updateProduct(id, data) {
  return request({ url: '/products/' + id, method: 'put', data })
}

// 批量上下架，留审计
export function changeProductStatus(ids, status) {
  return request({ url: '/products/status', method: 'post', data: { ids, status } })
}

// 违禁词校验
export function checkBannedWords(text) {
  return request({ url: '/check-banned-words', method: 'post', data: { text } })
}

export function listCategories() {
  return request({ url: '/categories', method: 'get' })
}

export function createCategory(data) {
  return request({ url: '/categories', method: 'post', data })
}
