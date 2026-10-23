import { reactive } from 'vue'
import { api, getToken } from './api'

/** 全局轻状态：购物车角标 / 立即购买透传 / 结算选中的地址 */
export const store = reactive({
  cartCount: 0,
  buyNow: null,        // {skuId, quantity, name, image, price, flavor, spec}
  checkoutAddressId: null,
  selectedStoreId: null,
  selectedStoreName: ''
})

export async function refreshCartCount() {
  if (!getToken()) { store.cartCount = 0; return }
  try {
    const d = await api.cart()
    store.cartCount = d.totalQuantity || 0
  } catch { /* 忽略 */ }
}

/** 单店模式：不提供手动切换门店，默认取第一家营业中的门店，其余仅展示 */
export function applyDefaultStore(list) {
  if (store.selectedStoreId || !(list || []).length) return
  const open = list.find(s => s.status === 'OPEN') || list[0]
  store.selectedStoreId = open.id
  store.selectedStoreName = open.name || ''
}

export function selectedStoreName() {
  return store.selectedStoreName
}
