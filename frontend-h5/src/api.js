/**
 * 后端接口封装（唯一契约：openspec/specs/api-contract/spec.md）
 * 统一响应 AjaxResult{code,msg,data}，code!==200 时抛错并 Toast msg。
 * 金额一律「分」。
 */
import { showToast } from './ui'

const TOKEN_KEY = 'token'

export function getToken() { return localStorage.getItem(TOKEN_KEY) || '' }
export function setToken(t) { t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY) }
export function isLoggedIn() { return !!getToken() }

async function request(method, path, body, silent = false) {
  const opt = { method, headers: { 'X-Token': getToken() } }
  if (body !== undefined) {
    opt.headers['Content-Type'] = 'application/json'
    opt.body = JSON.stringify(body)
  }
  let json
  try {
    const res = await fetch(path, opt)
    json = await res.json()
  } catch {
    if (!silent) showToast('网络异常，请稍后重试')
    throw new Error('网络异常')
  }
  if (json.code !== 200) {
    const err = new Error(json.msg || '请求失败')
    if (!silent) showToast(err.message)
    throw err
  }
  return json.data
}

// ---- 工具 ----
export function yuan(fen) { return (Number(fen || 0) / 100).toFixed(2) }
export function uuid() {
  if (crypto.randomUUID) return crypto.randomUUID()
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16)
  })
}
function qs(p = {}) {
  const s = new URLSearchParams()
  Object.entries(p).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') s.set(k, v) })
  const str = s.toString()
  return str ? '?' + str : ''
}

export const api = {
  // ---- AC-01 会员 ----
  login: (phone, smsCode) => request('POST', '/api/member/login', { phone, smsCode }),
  me: () => request('GET', '/api/member/me'),
  pointsLogs: (page = 1, size = 10) => request('GET', `/api/member/points/logs${qs({ page, size })}`),

  // ---- AC-02 门店 ----
  stores: () => request('GET', '/api/stores'),

  // ---- AC-03 商品 ----
  categories: () => request('GET', '/api/categories'),
  products: (p = {}) => request('GET', `/api/products${qs(p)}`),
  product: (id) => request('GET', `/api/products/${id}`),

  // ---- AC-04 购物车 ----
  cart: () => request('GET', '/api/cart'),
  addCart: (skuId, quantity) => request('POST', '/api/cart', { skuId, quantity }),
  updateCart: (id, data) => request('POST', '/api/cart/update', { id, ...data }),
  deleteCart: (ids) => request('POST', '/api/cart/delete', { ids }),
  checkAll: (checked) => request('POST', '/api/cart/check-all', { checked }),
  clearInvalid: () => request('POST', '/api/cart/clear-invalid'),

  // ---- AC-05 地址 ----
  addresses: () => request('GET', '/api/addresses'),
  addAddress: (d) => request('POST', '/api/addresses', d),
  updateAddress: (id, d) => request('PUT', `/api/addresses/${id}`, d),
  deleteAddress: (id) => request('DELETE', `/api/addresses/${id}`),
  setDefaultAddress: (id) => request('POST', `/api/addresses/${id}/default`),

  // ---- AC-06 订单 ----
  createOrder: (d) => request('POST', '/api/orders', d, true),
  orders: (p = {}) => request('GET', `/api/orders${qs(p)}`),
  orderDetail: (no) => request('GET', `/api/orders/${no}`),
  pay: (no) => request('POST', `/api/orders/${no}/pay`),
  cancelOrder: (no) => request('POST', `/api/orders/${no}/cancel`),
  confirmOrder: (no) => request('POST', `/api/orders/${no}/confirm`),

  // ---- AC-07 核销码 ----
  verifyCode: (no) => request('GET', `/api/orders/${no}/verify-code`),

  // ---- AC-08 售后 ----
  aftersaleReasons: () => request('GET', '/api/aftersales/reasons'),
  createAftersale: (d) => request('POST', '/api/aftersales', d),
  aftersales: (p = {}) => request('GET', `/api/aftersales${qs(p)}`),
  aftersaleDetail: (no) => request('GET', `/api/aftersales/${no}`),
  cancelAftersale: (no) => request('POST', `/api/aftersales/${no}/cancel`),
  returnLogistics: (no, d) => request('POST', `/api/aftersales/${no}/return-logistics`, d)
}
