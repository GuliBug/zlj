// 商品图缺失/加载失败时的红金品牌占位图（与 H5 GoodsMedia 回退观感对齐）
export const PLACEHOLDER_IMG =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#8f1d22"/><stop offset="1" stop-color="#6d1216"/></linearGradient></defs><rect width="200" height="200" fill="url(#g)"/><rect x="14" y="14" width="172" height="172" fill="none" stroke="#C9A063" stroke-width="3"/><text x="100" y="92" text-anchor="middle" font-family="serif" font-size="34" fill="#F3D9A4">正隆記</text><text x="100" y="128" text-anchor="middle" font-family="serif" font-size="16" fill="#C9A063">始于 1916</text></svg>`
  )
