import { reactive } from 'vue'
import { api, getToken } from './api'

/** 全局轻状态：购物车角标 / 立即购买透传 / 结算选中的地址 */
export const store = reactive({
  cartCount: 0,
  buyNow: null,        // {skuId, quantity, name, image, price, flavor, spec}
  checkoutAddressId: null,
  selectedStoreId: Number(localStorage.getItem('storeId')) || null
})

export async function refreshCartCount() {
  if (!getToken()) { store.cartCount = 0; return }
  try {
    const d = await api.cart()
    store.cartCount = d.totalQuantity || 0
  } catch { /* 忽略 */ }
}

export function selectStore(id, name) {
  store.selectedStoreId = id
  localStorage.setItem('storeId', id)
  localStorage.setItem('storeName', name || '')
}
export function selectedStoreName() {
  return localStorage.getItem('storeName') || ''
}
