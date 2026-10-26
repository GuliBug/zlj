/**
 * E2E 端到端测试：正隆记槟榔商城全需求链路
 * 对象：运行中的后端 http://localhost:8080（真实 HTTP，覆盖 H5 客户端 + 管理端接口）
 * 运行：node tests/e2e/e2e-mall.mjs
 * 输出：控制台结果 + tests/e2e/last-run.json（供生成测试报告）
 */
import { writeFileSync } from 'node:fs'

const B = 'http://localhost:8080'
const results = []
let buyerToken = ''
let adminToken = ''
let managerToken = ''

async function req(method, path, body, token) {
  const r = await fetch(B + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { 'X-Token': token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await r.text()
  try {
    return JSON.parse(text)
  } catch {
    return { code: r.status, raw: text }
  }
}

function check(id, name, cond, detail) {
  results.push({ id, name, pass: !!cond, detail: detail === undefined ? '' : String(detail) })
  console.log(`${cond ? '✅ PASS' : '❌ FAIL'} [${id}] ${name}${detail ? ' — ' + String(detail).slice(0, 160) : ''}`)
}

const sleep = (ms) => new Promise((s) => setTimeout(s, ms))

async function waitUp() {
  for (let i = 0; i < 60; i++) {
    try {
      const r = await fetch(B + '/api/products')
      if (r.ok) return true
    } catch {
      /* retry */
    }
    await sleep(1000)
  }
  throw new Error('后端未就绪：http://localhost:8080')
}

const yuan = (fen) => (fen / 100).toFixed(2)

;(async () => {
  await waitUp()
  const phone = '139' + String(Date.now()).slice(-8)

  // ===================== 一、会员与登录（FR-01 / FR-07 / PL-07） =====================
  const badLogin = await req('POST', '/api/member/login', { phone, smsCode: '000000' })
  check('TC-FR-01-01', '错误验证码登录被拒绝', badLogin.code !== 200, badLogin.msg)

  const login = await req('POST', '/api/member/login', { phone, smsCode: '123456' })
  buyerToken = login.data?.token
  check('TC-FR-01-01', '手机号+验证码登录（新号自动注册）', login.code === 200 && !!buyerToken, `token 前缀 ${String(buyerToken).slice(0, 4)}`)

  const noAuth = await req('GET', '/api/cart')
  check('TC-PL-01', '未登录访问购物车被拦截', noAuth.code !== 200, noAuth.msg)

  const me = await req('GET', '/api/member/me', undefined, buyerToken)
  check(
    'TC-PL-07',
    '会员信息手机号脱敏展示',
    me.code === 200 && /\*{4}/.test(me.data?.phone || ''),
    `phone=${me.data?.phone}, level=${me.data?.levelName}`
  )

  // 字段级校验：登录（FC-001）
  const emptyPhone = await req('POST', '/api/member/login', { phone: '', smsCode: '123456' })
  check('FC-001', '登录手机号为空被拒（手机号不能为空）', emptyPhone.code !== 200, emptyPhone.msg)

  // ===================== 二、商品目录（FR-02） =====================
  const list = await req('GET', '/api/products?page=1&size=10')
  check('TC-FR-02-01', '商品列表分页结构 {total,rows}', list.code === 200 && Array.isArray(list.data?.rows) && typeof list.data?.total === 'number', `共 ${list.data?.total} 条`)
  check('TC-FR-02-01', '买家列表不返回下架商品', (list.data?.rows || []).every((p) => p.status === 'ON'), `首条 ${list.data?.rows?.[0]?.name}`)

  const pid = list.data.rows[0].id
  const detail = await req('GET', `/api/products/${pid}`)
  const d = detail.data || {}
  check('TC-FR-02-02', '详情含 SKU 列表与库存', detail.code === 200 && Array.isArray(d.skus) && d.skus.length > 0, `SKU ${d.skus?.length} 个`)
  check('TC-FR-02-02', '警示语硬编码强制返回', d.warnText === '长期过量咀嚼槟榔，有害口腔健康', d.warnText)
  check('TC-FR-02-05', '未成年人提示强制返回', d.minorText === '不向未成年人销售', d.minorText)

  const search = await req('GET', '/api/products?keyword=' + encodeURIComponent('正隆记'))
  check('TC-FR-02-01', '关键字搜索命中', search.code === 200 && (search.data?.rows || []).length > 0, `命中 ${search.data?.total} 条`)

  const cats = await req('GET', '/api/categories')
  check(
    'TC-FR-02-01',
    '分类树返回父子结构（供首页分档渲染）',
    cats.code === 200 && Array.isArray(cats.data) && cats.data.length > 0 && cats.data.every((c) => c.name && Array.isArray(c.children || [])),
    `${(cats.data || []).length} 个顶级分类`
  )

  // ===================== 三、购物车（FR-03） =====================
  // 选一个有库存且价格适中的 SKU
  const skuList = []
  for (const row of (await req('GET', '/api/products?page=1&size=20')).data.rows) {
    const det = await req('GET', `/api/products/${row.id}`)
    for (const s of det.data.skus || []) if (s.stock > 5) skuList.push({ ...s, productName: det.data.name, productId: row.id })
  }
  const cheap = skuList.sort((a, b) => a.price - b.price)[0]
  const big = skuList.sort((a, b) => b.stock - a.stock)[0]

  await req('POST', '/api/cart', { skuId: cheap.id, quantity: 1 }, buyerToken)
  await req('POST', '/api/cart', { skuId: cheap.id, quantity: 2 }, buyerToken)
  const cart = await req('GET', '/api/cart', undefined, buyerToken)
  const cartRow = (cart.data?.rows || []).find((r) => r.skuId === cheap.id)
  check('TC-FR-03-01', '重复加购同 SKU 数量累加', cartRow?.quantity === 3, `quantity=${cartRow?.quantity}`)

  const cartId = cartRow?.id
  await req('POST', '/api/cart/update', { id: cartId, quantity: 4, checked: true }, buyerToken)
  const cart2 = await req('GET', '/api/cart', undefined, buyerToken)
  check('TC-FR-03-02', '购物车改数量与勾选生效', (cart2.data?.rows || []).find((r) => r.id === cartId)?.quantity === 4)

  // 全选/全不选与删除（FR-03-2 / FR-03-3），totalQuantity 为角标数据源
  await req('POST', '/api/cart/check-all', { checked: false }, buyerToken)
  const cartUnchecked = await req('GET', '/api/cart', undefined, buyerToken)
  check('TC-FR-03-02', '全不选后所有行未勾选', cartUnchecked.code === 200 && (cartUnchecked.data?.rows || []).every((r) => !r.checked))
  await req('POST', '/api/cart/check-all', { checked: true }, buyerToken)
  const cartChecked = await req('GET', '/api/cart', undefined, buyerToken)
  check(
    'TC-FR-03-02',
    '全选后所有行勾选（totalQuantity 同步）',
    cartChecked.code === 200 && (cartChecked.data?.rows || []).every((r) => r.checked) && cartChecked.data?.totalQuantity > 0,
    `totalQuantity=${cartChecked.data?.totalQuantity}`
  )
  await req('POST', '/api/cart/delete', { ids: [cartId] }, buyerToken)
  const cartAfterDel = await req('GET', '/api/cart', undefined, buyerToken)
  check('TC-FR-03-03', '购物车行删除生效', !(cartAfterDel.data?.rows || []).some((r) => r.id === cartId), `剩余 ${(cartAfterDel.data?.rows || []).length} 行`)

  // 字段级校验：购物车（FC-020 / FC-021）
  const cartBadQty = await req('POST', '/api/cart', { skuId: cheap.id, quantity: 0 }, buyerToken)
  const cartBadSku = await req('POST', '/api/cart', { skuId: 99999999, quantity: 1 }, buyerToken)
  check('FC-020', '购物车非法参数被拒（quantity≤0 / SKU 不存在）', cartBadQty.code !== 200 && cartBadSku.code !== 200, `${cartBadQty.msg} / ${cartBadSku.msg}`)
  const cartOverMax = await req('POST', '/api/cart', { skuId: cheap.id, quantity: 1000 }, buyerToken)
  check('FC-021', '单 SKU 数量上限 999（超出被拒）', cartOverMax.code !== 200, cartOverMax.msg)

  // 地址（FR-08-1）
  const addr = await req('POST', '/api/addresses', { receiver: '张三', phone: '13800001114', region: '湖南省湘潭市岳塘区', detail: '正隆街 1 号 3 栋 501', isDefault: true }, buyerToken)
  check('TC-FR-08-01', '地址新增（纯文本）', addr.code === 200, addr.msg)
  const addresses = await req('GET', '/api/addresses', undefined, buyerToken)
  const addressId = addresses.data?.[0]?.id
  check('TC-FR-08-01', '地址列表返回默认地址', addresses.code === 200 && addresses.data?.some((a) => a.isDefault), `共 ${addresses.data?.length} 条`)
  check('TC-FR-08-01', '地址手机号校验（非法手机号拒绝）', (await req('POST', '/api/addresses', { receiver: '李四', phone: '123', region: 'x', detail: 'y' }, buyerToken)).code !== 200)

  // 字段级校验：地址（FC-010 / FC-012 / FC-013 / FC-014）
  const addrNoReceiver = await req('POST', '/api/addresses', { receiver: ' ', phone: '13800001114', region: '湖南省', detail: 'x' }, buyerToken)
  check('FC-010', '地址收货人必填（空白拒绝）', addrNoReceiver.code !== 200, addrNoReceiver.msg)
  const addrNoRegion = await req('POST', '/api/addresses', { receiver: '李四', phone: '13800001114', region: '', detail: 'x' }, buyerToken)
  check('FC-012', '地址所在地区必填', addrNoRegion.code !== 200, addrNoRegion.msg)
  const addrNoDetail = await req('POST', '/api/addresses', { receiver: '李四', phone: '13800001114', region: '湖南省', detail: '' }, buyerToken)
  check('FC-013', '地址详细地址必填', addrNoDetail.code !== 200, addrNoDetail.msg)
  await req('POST', '/api/addresses', { receiver: '默认A', phone: '13800001114', region: '湖南省湘潭市', detail: 'A 号', isDefault: true }, buyerToken)
  await req('POST', '/api/addresses', { receiver: '默认B', phone: '13800001114', region: '湖南省湘潭市', detail: 'B 号', isDefault: true }, buyerToken)
  const addrList2 = await req('GET', '/api/addresses', undefined, buyerToken)
  check('FC-014', '重复设默认后仅一条默认地址（互斥）', addrList2.code === 200 && addrList2.data.filter((a) => a.isDefault).length === 1, `默认地址 ${addrList2.data.filter((a) => a.isDefault).length} 条`)

  // 地址编辑 / 设默认 / 删除（FR-08-1）
  const addrC = await req('POST', '/api/addresses', { receiver: '赵六', phone: '13800001115', region: '湖南省长沙市', detail: '临时地址' }, buyerToken)
  const addrCId = addrC.data?.id
  const addrUpd = await req('PUT', `/api/addresses/${addrCId}`, { receiver: '赵六六', phone: '13800001115', region: '湖南省长沙市', detail: '编辑后的地址' }, buyerToken)
  const addrUpdList = await req('GET', '/api/addresses', undefined, buyerToken)
  check(
    'TC-FR-08-01',
    '地址编辑生效',
    addrUpd.code === 200 && (addrUpdList.data || []).some((a) => a.id === addrCId && a.receiver === '赵六六' && /编辑后/.test(a.detail)),
    addrUpd.msg
  )
  await req('POST', `/api/addresses/${addrCId}/default`, undefined, buyerToken)
  const addrDefaultList = await req('GET', '/api/addresses', undefined, buyerToken)
  check(
    'TC-FR-08-01',
    '设为默认接口生效（唯一默认）',
    addrDefaultList.data.find((a) => a.id === addrCId)?.isDefault === true && addrDefaultList.data.filter((a) => a.isDefault).length === 1
  )
  const addrDel = await req('DELETE', `/api/addresses/${addrCId}`, undefined, buyerToken)
  const addrDelList = await req('GET', '/api/addresses', undefined, buyerToken)
  check('TC-FR-08-01', '地址删除生效', addrDel.code === 200 && !(addrDelList.data || []).some((a) => a.id === addrCId), addrDel.msg)

  // ===================== 四、管理端基础（FR-10 / FR-16 / PL-02） =====================
  const badAdmin = await req('POST', '/admin-api/login', { username: 'admin', password: 'wrong' })
  check('TC-PL-02', '管理端错误密码登录被拒', badAdmin.code !== 200, badAdmin.msg)
  const adminLogin = await req('POST', '/admin-api/login', { username: 'admin', password: 'admin123' })
  adminToken = adminLogin.data?.token
  check('TC-PL-02', '管理员登录（ADM- 令牌）', adminLogin.code === 200 && String(adminToken).startsWith('ADM-'), adminLogin.data?.nickname)
  managerToken = (await req('POST', '/admin-api/login', { username: 'manager', password: 'manager123' })).data?.token

  const dash = await req('GET', '/admin-api/dashboard', undefined, adminToken)
  const dd = dash.data || {}
  check(
    'TC-FR-16-01',
    '工作台核心指标齐全（GMV/订单/支付率/待发货/待售后/趋势）',
    dash.code === 200 && dd.gmvToday !== undefined && dd.payRate !== undefined && dd.pendingShip !== undefined && dd.pendingAftersale !== undefined && Array.isArray(dd.salesTrend),
    `GMV=${yuan(dd.gmvToday || 0)} 元, 支付率=${dd.payRate}%`
  )

  // 违禁词（FR-02-3）
  const banned = await req('POST', '/admin-api/check-banned-words', { text: '本品提神醒脑，具有保健功效' }, adminToken)
  check('TC-FR-02-03', '违禁词校验命中功效宣传词', banned.code === 200 && (banned.data?.hits || []).length >= 2, `命中 ${JSON.stringify(banned.data?.hits)}`)

  const bannedProduct = await req(
    'POST',
    '/admin-api/products',
    {
      name: '测试违禁商品',
      categoryId: 1,
      brand: '正隆记',
      origin: '湖南湘潭',
      description: '提神醒脑',
      images: ['/img/a.jpg', '/img/b.jpg', '/img/c.jpg'],
      detailImages: [],
      skus: [{ flavor: '原味', spec: '单包', barcode: 'TEST-BAN-' + Date.now(), price: 1000, costPrice: 500, stock: 10 }]
    },
    adminToken
  )
  check('TC-FR-02-03', '含违禁词商品保存被阻断', bannedProduct.code !== 200, bannedProduct.msg)

  // 字段级校验：商品/分类管理（FC-070 ~ FC-076）
  const pBase = { brand: '正隆记', origin: '湖南湘潭', description: '字段校验专用', images: [], detailImages: [], skus: [{ flavor: '原味', spec: '单包', barcode: 'FC-' + Date.now(), price: 100, costPrice: 50, stock: 1 }] }
  const pNoName = await req('POST', '/admin-api/products', { ...pBase, name: '', categoryId: 1 }, adminToken)
  check('FC-070', '商品名称必填', pNoName.code !== 200, pNoName.msg)
  const pNoCat = await req('POST', '/admin-api/products', { ...pBase, name: 'FC 分类校验', categoryId: null }, adminToken)
  check('FC-071', '商品分类必填', pNoCat.code !== 200, pNoCat.msg)
  const pNoSku = await req('POST', '/admin-api/products', { ...pBase, name: 'FC SKU 校验', categoryId: 1, skus: [] }, adminToken)
  check('FC-072', 'SKU 列表必填', pNoSku.code !== 200, pNoSku.msg)
  const pZeroPrice = await req('POST', '/admin-api/products', { ...pBase, name: 'FC 价格校验', categoryId: 1, skus: [{ ...pBase.skus[0], price: 0 }] }, adminToken)
  check('FC-073', 'SKU 售价必须大于 0', pZeroPrice.code !== 200, pZeroPrice.msg)
  const pNegStock = await req('POST', '/admin-api/products', { ...pBase, name: 'FC 库存校验', categoryId: 1, skus: [{ ...pBase.skus[0], stock: -1 }] }, adminToken)
  check('FC-074', 'SKU 库存必须大于等于 0', pNegStock.code !== 200, pNegStock.msg)
  const pBadStatus = await req('POST', '/admin-api/products/status', { ids: [1], status: 'AAA' }, adminToken)
  check('FC-075', '商品状态枚举校验（非法状态拒绝）', pBadStatus.code !== 200, pBadStatus.msg)
  const catNoName = await req('POST', '/admin-api/categories', { name: '', parentId: null }, adminToken)
  check('FC-076', '分类名称必填', catNoName.code !== 200, catNoName.msg)

  // H5-13 契约锚点：买家详情接口回传 detailImages[]（主图与详情图分离，202610-H5买家端 增量）
  const mediaProd = await req(
    'POST',
    '/admin-api/products',
    {
      name: 'H5-13 详情图回传锚点',
      categoryId: 1,
      brand: '正隆记',
      origin: '湖南湘潭',
      description: '详情图契约回传回归用',
      images: ['/img/m1.jpg', '/img/m2.jpg'],
      detailImages: ['/img/d1.jpg', '/img/d2.jpg'],
      status: 'ON',
      skus: [{ flavor: '原味', spec: '单包', barcode: 'H513-' + Date.now(), price: 100, costPrice: 50, stock: 5 }]
    },
    adminToken
  )
  const mediaDetail = await req('GET', `/api/products/${mediaProd.data?.id}`)
  check(
    'TC-H5-13',
    '买家详情接口回传 detailImages[]（主图与详情图分离）',
    mediaDetail.code === 200 && (mediaDetail.data?.images || []).length === 2 && Array.isArray(mediaDetail.data?.detailImages) && mediaDetail.data.detailImages.length === 2,
    `images=${mediaDetail.data?.images?.length} detailImages=${mediaDetail.data?.detailImages?.length}`
  )

  // 高价商品用于金额上限风控（FR-04-7）
  const expensive = await req(
    'POST',
    '/admin-api/products',
    {
      name: '测试高价礼盒（风控用）',
      categoryId: 1,
      brand: '正隆记',
      origin: '湖南湘潭',
      description: '风控测试商品',
      images: ['/img/x1.jpg', '/img/x2.jpg', '/img/x3.jpg'],
      detailImages: [],
      skus: [{ flavor: '礼盒', spec: '单盒', barcode: 'TEST-EXP-' + Date.now(), price: 600000, costPrice: 500000, stock: 5 }]
    },
    adminToken
  )
  check('TC-FR-10-01', '商品新增（含 SKU/图片/分类）成功', expensive.code === 200, expensive.msg)
  const expId = expensive.data?.id
  // 新增商品默认草稿态（OFF），上架后才对买家可见
  await req('POST', '/admin-api/products/status', { ids: [expId], status: 'ON' }, adminToken)
  const expDetail = expId ? await req('GET', `/admin-api/products/${expId}`, undefined, adminToken) : { data: null }

  // ===================== 五、下单 / 幂等 / 库存预占（FR-04） =====================
  // 字段级校验：下单（FC-030 ~ FC-037）
  const oNoKey = await req('POST', '/api/orders', { idempotentKey: '', fulfillmentType: 'EXPRESS', addressId, items: [{ skuId: cheap.id, quantity: 1 }] }, buyerToken)
  check('FC-030', '下单幂等键必填', oNoKey.code !== 200, oNoKey.msg)
  const oNoItems = await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-ni', fulfillmentType: 'EXPRESS', addressId, items: [] }, buyerToken)
  check('FC-031', '下单明细必填', oNoItems.code !== 200, oNoItems.msg)
  const oBadType = await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-bt', fulfillmentType: 'DUMMY', addressId, items: [{ skuId: cheap.id, quantity: 1 }] }, buyerToken)
  check('FC-032', '履约方式枚举校验', oBadType.code !== 200, oBadType.msg)
  const oNoStore = await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-ns', fulfillmentType: 'PICKUP', items: [{ skuId: cheap.id, quantity: 1 }] }, buyerToken)
  check('FC-033', '自提订单必须选择门店', oNoStore.code !== 200, oNoStore.msg)
  const st1 = (await req('GET', '/api/stores')).data[0]
  await req('PUT', `/admin-api/stores/${st1.id}`, { name: st1.name, address: st1.address, phone: st1.phone, status: 'CLOSED' }, adminToken)
  const oClosedStore = await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-sc', fulfillmentType: 'PICKUP', storeId: st1.id, items: [{ skuId: cheap.id, quantity: 1 }] }, buyerToken)
  await req('PUT', `/admin-api/stores/${st1.id}`, { name: st1.name, address: st1.address, phone: st1.phone, status: 'OPEN' }, adminToken)
  check('FC-034', '已关闭门店禁止自提下单', oClosedStore.code !== 200 && /门店已关闭/.test(oClosedStore.msg || ''), oClosedStore.msg)
  const oNoAddr = await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-na', fulfillmentType: 'EXPRESS', items: [{ skuId: cheap.id, quantity: 1 }] }, buyerToken)
  check('FC-035', '快递订单必须选择收货地址', oNoAddr.code !== 200, oNoAddr.msg)
  const phone2 = '138' + String(Date.now()).slice(-8)
  const login2 = await req('POST', '/api/member/login', { phone: phone2, smsCode: '123456' })
  const addr2 = await req('POST', '/api/addresses', { receiver: '王五', phone: phone2, region: '湖南省湘潭市', detail: '他人地址 1 号' }, login2.data?.token)
  const oForeignAddr = await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-fa', fulfillmentType: 'EXPRESS', addressId: addr2.data?.id, items: [{ skuId: cheap.id, quantity: 1 }] }, buyerToken)
  check('FC-036', '使用他人地址下单被拒（无权使用）', oForeignAddr.code !== 200, oForeignAddr.msg)
  const oNegQty = await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-nq', fulfillmentType: 'EXPRESS', addressId, items: [{ skuId: cheap.id, quantity: -1 }] }, buyerToken)
  const oBadSku = await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-bs', fulfillmentType: 'EXPRESS', addressId, items: [{ skuId: 99999999, quantity: 1 }] }, buyerToken)
  check('FC-037', '下单明细行参数校验（quantity≤0 / SKU 不存在）', oNegQty.code !== 200 && oBadSku.code !== 200, `${oNegQty.msg} / ${oBadSku.msg}`)

  const qtyForFree = Math.max(1, Math.ceil(9900 / cheap.price))
  const key1 = 'e2e-' + Date.now() + '-a'
  const order1 = await req(
    'POST',
    '/api/orders',
    { idempotentKey: key1, fulfillmentType: 'EXPRESS', addressId, usePoints: false, items: [{ skuId: cheap.id, quantity: qtyForFree }] },
    buyerToken
  )
  const o1 = order1.data || {}
  check('TC-FR-04-01', '运费规则：商品合计满 99 元包邮', order1.code === 200 && o1.freight === 0, `商品合计 ${yuan(o1.goodsAmount || 0)} 元，运费 ${yuan(o1.freight || 0)} 元`)

  const order1Dup = await req(
    'POST',
    '/api/orders',
    { idempotentKey: key1, fulfillmentType: 'EXPRESS', addressId, usePoints: false, items: [{ skuId: cheap.id, quantity: qtyForFree }] },
    buyerToken
  )
  check('TC-FR-04-02', '同幂等键重复提交不产生重复订单', order1Dup.code === 200 && order1Dup.data?.orderNo === o1.orderNo, `单号 ${o1.orderNo} → ${order1Dup.data?.orderNo}`)

  const skuAfterReserve = await req('GET', `/api/products/${cheap.productId}`)
  const skuRow = (skuAfterReserve.data.skus || []).find((s) => s.id === cheap.id)
  check('TC-FR-04-03', '下单预占库存（可用减少）', skuRow && skuRow.stock === cheap.stock - qtyForFree, `可用 ${cheap.stock} → ${skuRow?.stock}`)

  const overStock = await req(
    'POST',
    '/api/orders',
    { idempotentKey: 'e2e-' + Date.now() + '-b', fulfillmentType: 'EXPRESS', addressId, items: [{ skuId: cheap.id, quantity: 99999 }] },
    buyerToken
  )
  check('TC-FR-04-03', '库存不足下单失败并返回缺货信息', overStock.code !== 200 && /缺货|库存不足/.test(overStock.msg || ''), overStock.msg)

  // BUG-12 回归锚点：base64 主图（>255 字符）+ 库存 1 的商品下单成功，订单项快照图回读仍为 dataURL
  const bug12Img = 'data:image/jpeg;base64,' + 'A'.repeat(512)
  const bug12Create = await req(
    'POST',
    '/admin-api/products',
    {
      name: 'BUG12 回归·库存1带图商品',
      categoryId: 1,
      brand: '正隆记',
      origin: '湖南湘潭',
      description: 'dataURL 快照落库回归用',
      images: [bug12Img],
      detailImages: [],
      skus: [{ flavor: '原味', spec: '单包', barcode: 'BUG12-' + Date.now(), price: 990, costPrice: 500, stock: 1 }]
    },
    adminToken
  )
  const bug12Id = bug12Create.data?.id
  await req('POST', '/admin-api/products/status', { ids: [bug12Id], status: 'ON' }, adminToken)
  const bug12Sku = bug12Id ? (await req('GET', `/api/products/${bug12Id}`)).data?.skus?.[0] : null
  const bug12Order = bug12Sku
    ? await req('POST', '/api/orders', { idempotentKey: 'e2e-bug12-' + Date.now(), fulfillmentType: 'EXPRESS', addressId, items: [{ skuId: bug12Sku.id, quantity: 1 }] }, buyerToken)
    : { code: 0, msg: 'BUG12 商品未创建' }
  const bug12Detail = bug12Order.code === 200 ? await req('GET', `/api/orders/${bug12Order.data?.orderNo}`, undefined, buyerToken) : { data: null }
  const bug12ItemImg = (bug12Detail.data?.items || [])[0]?.image || ''
  check('TC-FR-04-01', 'BUG-12 回归：库存 1 的 dataURL 主图商品下单成功且快照图回读为 dataURL', bug12Order.code === 200 && bug12ItemImg.startsWith('data:image/'), `下单 ${bug12Order.msg}，快照图前缀 ${bug12ItemImg.slice(0, 22)}`)

  // 金额上限：6000 元 > 5000 元
  const expSkuId = (expDetail.data?.skus || [])[0]?.id ?? null
  const overLimit = expSkuId
    ? await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-c', fulfillmentType: 'EXPRESS', addressId, items: [{ skuId: expSkuId, quantity: 1 }] }, buyerToken)
    : { code: 0, msg: '高价 SKU 未创建' }
  check('TC-FR-04-07', '单笔实付超 5000 元被风控拦截', overLimit.code !== 200, overLimit.msg)

  // 支付（FR-04-6 / FR-07-2）
  const pay1 = await req('POST', `/api/orders/${o1.orderNo}/pay`, undefined, buyerToken)
  check('TC-FR-04-06', '模拟支付成功（回调+查询双通道语义）', pay1.code === 200, pay1.msg)

  const orderDetail1 = await req('GET', `/api/orders/${o1.orderNo}`, undefined, buyerToken)
  check('TC-FR-04-06', '支付后订单转待发货', orderDetail1.data?.status === 'PAID', orderDetail1.data?.statusText)

  const skuAfterPay = await req('GET', `/api/products/${cheap.productId}`)
  const skuRow2 = (skuAfterPay.data.skus || []).find((s) => s.id === cheap.id)
  check('TC-FR-11-05', '支付成功后库存实扣', skuRow2 && skuRow2.stock === cheap.stock - qtyForFree, `可用 ${skuRow2?.stock}`)

  // 已售累计（FR-02-6）：实扣时 SKU 级与商品级同步增加
  const skuRowBeforePay = (skuAfterReserve.data.skus || []).find((s) => s.id === cheap.id)
  check(
    'TC-FR-02-06',
    '支付实扣后已售累计（SKU 级与商品级）',
    skuRow2 && skuRowBeforePay && skuRow2.sales === skuRowBeforePay.sales + qtyForFree && skuAfterPay.data.sales === skuAfterReserve.data.sales + qtyForFree,
    `SKU 已售 ${skuRowBeforePay?.sales}→${skuRow2?.sales}，商品已售 ${skuAfterReserve.data.sales}→${skuAfterPay.data.sales}`
  )

  const me2 = await req('GET', '/api/member/me', undefined, buyerToken)
  check('TC-FR-07-02', '支付成功按实付发放积分（1 元=1 积分）', me2.data?.points >= Math.floor((o1.payAmount || 0) / 100), `积分 ${me2.data?.points}，实付 ${yuan(o1.payAmount || 0)} 元`)

  const payAgain = await req('POST', `/api/orders/${o1.orderNo}/pay`, undefined, buyerToken)
  check('TC-FR-04-06', '重复支付被拒（幂等）', payAgain.code !== 200, payAgain.msg)

  const alerts = await req('GET', '/admin-api/alerts?page=1&size=50', undefined, adminToken)
  check(
    'TC-PL-04',
    '资损监控登记重复支付告警',
    alerts.code === 200 && (alerts.data?.rows || []).some((a) => a.type === 'DUPLICATE_PAY'),
    `告警数 ${alerts.data?.total}`
  )

  // 取消待支付订单（FR-05-2）
  const stockBeforeCancel = (await req('GET', `/api/products/${cheap.productId}`)).data.skus.find((s) => s.id === cheap.id).stock
  const order2 = await req(
    'POST',
    '/api/orders',
    { idempotentKey: 'e2e-' + Date.now() + '-d', fulfillmentType: 'EXPRESS', addressId, items: [{ skuId: cheap.id, quantity: 1 }] },
    buyerToken
  )
  await req('POST', `/api/orders/${order2.data.orderNo}/cancel`, undefined, buyerToken)
  const detail2 = await req('GET', `/api/orders/${order2.data.orderNo}`, undefined, buyerToken)
  const prodAfterCancel = await req('GET', `/api/products/${cheap.productId}`)
  const skuAfterCancelRow = prodAfterCancel.data.skus.find((s) => s.id === cheap.id)
  const stockAfterCancel = skuAfterCancelRow.stock
  check('TC-FR-05-02', '取消待支付订单并释放预占', detail2.data?.status === 'CLOSED' && stockAfterCancel === stockBeforeCancel, `状态 ${detail2.data?.status}，可用 ${stockBeforeCancel}→${stockAfterCancel}`)
  check('TC-FR-02-06', '取消待支付订单不影响已售（仅预占未实扣）', skuAfterCancelRow.sales === skuRow2.sales && prodAfterCancel.data.sales === skuAfterPay.data.sales, `SKU 已售 ${skuAfterCancelRow.sales}，商品已售 ${prodAfterCancel.data.sales}`)
  check('TC-O-04', '禁止状态回跳（已关闭订单再次支付被拒）', (await req('POST', `/api/orders/${order2.data.orderNo}/pay`, undefined, buyerToken)).code !== 200)

  // 订单列表筛选（FR-05-1）
  const paidList = await req('GET', '/api/orders?status=PAID&page=1&size=10', undefined, buyerToken)
  check('TC-FR-05-01', '订单列表按状态筛选（待发货）', paidList.code === 200 && (paidList.data?.rows || []).every((o) => o.status === 'PAID'), `共 ${paidList.data?.total} 单`)

  // 积分抵扣下单（FR-07-2）：抵扣上限 5%，服务端计价，取消后返还
  const meBeforeDeduct = await req('GET', '/api/member/me', undefined, buyerToken)
  const pointsBefore = meBeforeDeduct.data?.points || 0
  const storeNow = (await req('GET', '/api/stores')).data[0]
  const deductOrder = await req(
    'POST',
    '/api/orders',
    { idempotentKey: 'e2e-' + Date.now() + '-pt', fulfillmentType: 'PICKUP', storeId: storeNow.id, usePoints: true, items: [{ skuId: cheap.id, quantity: 1 }] },
    buyerToken
  )
  const od = deductOrder.data || {}
  const expectDeduct = Math.floor((od.goodsAmount + od.freight) * 5 / 100)
  check(
    'TC-FR-07-02',
    '积分抵扣下单（抵扣≤5% 且金额为服务端计算）',
    deductOrder.code === 200 && od.pointsDeduction === Math.min(pointsBefore, expectDeduct) && od.payAmount === od.goodsAmount + od.freight - od.pointsDeduction,
    `积分 ${pointsBefore}，抵扣 ${od.pointsDeduction} 分，实付 ${yuan(od.payAmount || 0)} 元`
  )
  const meAfterDeduct = await req('GET', '/api/member/me', undefined, buyerToken)
  check('TC-FR-07-02', '下单抵扣后积分账本扣减', meAfterDeduct.data?.points === pointsBefore - od.pointsUsed, `${pointsBefore} → ${meAfterDeduct.data?.points}`)
  await req('POST', `/api/orders/${od.orderNo}/cancel`, undefined, buyerToken)
  const meAfterCancelPts = await req('GET', '/api/member/me', undefined, buyerToken)
  check('TC-FR-07-02', '取消订单后抵扣积分返还', meAfterCancelPts.data?.points === pointsBefore, `${meAfterDeduct.data?.points} → ${meAfterCancelPts.data?.points}`)

  // ===================== 六、发货 / 收货（FR-12-2 / FR-05-2） =====================
  const shipBad = await req('POST', `/admin-api/orders/${o1.orderNo}/ship`, { company: '', no: '' }, adminToken)
  check('TC-FR-12-02', '发货必填物流公司+单号', shipBad.code !== 200, shipBad.msg)
  const ship = await req('POST', `/admin-api/orders/${o1.orderNo}/ship`, { company: '顺丰速运', no: 'SF' + Date.now() }, adminToken)
  check('TC-FR-12-02', '发货成功（仅待发货可发）', ship.code === 200, ship.msg)
  check('TC-FR-12-04', '订单列表返回超时发货标识字段', (await req('GET', '/admin-api/orders?page=1&size=5', undefined, adminToken)).data?.rows?.every((r) => 'shipOverdue' in r))

  const confirmBad = await req('POST', `/api/orders/${order2.data.orderNo}/confirm`, undefined, buyerToken)
  check('TC-FR-05-02', '非待收货订单确认收货被拒', confirmBad.code !== 200, confirmBad.msg)
  const confirm = await req('POST', `/api/orders/${o1.orderNo}/confirm`, undefined, buyerToken)
  check('TC-FR-05-02', '确认收货转已完成', confirm.code === 200 && (await req('GET', `/api/orders/${o1.orderNo}`, undefined, buyerToken)).data?.status === 'COMPLETED')

  // 批量发货失败行回显（FR-12-1）
  const batch = await req('POST', '/admin-api/orders/batch-ship', { rows: [{ orderNo: 'NOT-EXIST-0001', company: '顺丰速运', no: 'SF0001' }] }, adminToken)
  check('TC-FR-12-01', '批量发货失败行回显原因', batch.code === 200 && (batch.data?.failures || []).length === 1, JSON.stringify(batch.data?.failures))

  const csv = await fetch(B + '/admin-api/orders/export', { headers: { 'X-Token': adminToken } })
  const csvText = await csv.text()
  check('TC-FR-12-03', '订单导出 CSV 含表头与金额列', csv.ok && /订单号/.test(csvText) && /实付金额|金额/.test(csvText), `长度 ${csvText.length}`)

  // ===================== 七、售后闭环（FR-06 / FR-13） =====================
  const reasons = await req('GET', '/api/aftersales/reasons', undefined, buyerToken)
  check('TC-FR-06-02', '售后原因枚举接口', reasons.code === 200 && (reasons.data || []).length >= 3, `${(reasons.data || []).length} 项`)

  // 字段级校验：售后申请（FC-060 ~ FC-063）
  const asNoParam = await req('POST', '/api/aftersales', { orderNo: null, skuId: cheap.id, type: 'REFUND_ONLY', reason: '质量问题', images: [] }, buyerToken)
  check('FC-060', '售后申请参数不完整被拒', asNoParam.code !== 200, asNoParam.msg)
  const asBadReason = await req('POST', '/api/aftersales', { orderNo: o1.orderNo, skuId: cheap.id, type: 'REFUND_ONLY', reason: '随便问问', images: [] }, buyerToken)
  check('FC-061', '售后原因枚举校验（非法原因拒绝）', asBadReason.code !== 200, asBadReason.msg)
  const asBadType = await req('POST', '/api/aftersales', { orderNo: o1.orderNo, skuId: cheap.id, type: 'REFUND', reason: '质量问题', images: [] }, buyerToken)
  check('FC-062', '售后类型枚举校验（非法类型拒绝）', asBadType.code !== 200, asBadType.msg)
  const wpOrder = await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-wp', fulfillmentType: 'EXPRESS', addressId, items: [{ skuId: cheap.id, quantity: 1 }] }, buyerToken)
  const asWaitPay = await req('POST', '/api/aftersales', { orderNo: wpOrder.data.orderNo, skuId: cheap.id, type: 'REFUND_ONLY', reason: '质量问题', images: [] }, buyerToken)
  check('FC-063', '待支付订单不可申请售后', asWaitPay.code !== 200 && /已完成|已发货/.test(asWaitPay.msg || ''), asWaitPay.msg)
  await req('POST', `/api/orders/${wpOrder.data.orderNo}/cancel`, undefined, buyerToken)

  const as1 = await req(
    'POST',
    '/api/aftersales',
    { orderNo: o1.orderNo, skuId: cheap.id, type: 'REFUND_ONLY', reason: '质量问题', description: '包装破损', images: ['/img/proof1.jpg'] },
    buyerToken
  )
  const asNo = as1.data?.afterSaleNo
  check('TC-FR-06-01', '仅退款申请受理并生成工单', as1.code === 200 && !!asNo, asNo)

  const asDetail1 = await req('GET', `/api/aftersales/${asNo}`, undefined, buyerToken)
  check('TC-FR-06-05', '售后流转记录（提交/受理）自动生成', (asDetail1.data?.flow || []).length >= 2, `节点 ${(asDetail1.data?.flow || []).map((f) => f.action).join(' → ')}`)

  const auditNoOpinion = await req('POST', `/admin-api/aftersales/${asNo}/audit`, { approve: true, opinion: '' }, adminToken)
  check('TC-FR-13-01', '审核意见必填校验', auditNoOpinion.code !== 200, auditNoOpinion.msg)

  const audit = await req('POST', `/admin-api/aftersales/${asNo}/audit`, { approve: true, opinion: '核实包装破损，同意退款' }, adminToken)
  const asAfterAudit = await req('GET', `/admin-api/aftersales/${asNo}`, undefined, adminToken)
  check('TC-FR-13-02', '审核通过触发退款流程', audit.code === 200 && ['REFUNDING', 'REFUNDED'].includes(asAfterAudit.data?.status), asAfterAudit.data?.statusText)
  check('TC-FR-13-01', '审核后流转记录实时追加', (asAfterAudit.data?.flow || []).length >= 3, `${(asAfterAudit.data?.flow || []).length} 个节点`)

  const refund1 = await req('POST', `/admin-api/aftersales/${asNo}/refund`, undefined, adminToken)
  const asAfterRefund = await req('GET', `/api/aftersales/${asNo}`, undefined, buyerToken)
  check('TC-FR-13-04', '退款成功（服务端金额+原路退回语义）', refund1.code === 200 && asAfterRefund.data?.status === 'REFUNDED', `${asAfterRefund.data?.status}，退款 ${yuan(asAfterRefund.data?.refundAmount || 0)} 元`)

  const refund2 = await req('POST', `/admin-api/aftersales/${asNo}/refund`, undefined, adminToken)
  const asAfterRefund2 = await req('GET', `/api/aftersales/${asNo}`, undefined, buyerToken)
  check('TC-FR-13-04', '重复退款幂等（不重复退款）', refund2.code === 200 && asAfterRefund2.data?.status === 'REFUNDED', `退款单号 ${asAfterRefund2.data?.refundNo}`)
  check('TC-FR-06-05', '退款完成后时间线含 4+ 节点且可双端查看', (asAfterRefund2.data?.flow || []).length >= 4, `${(asAfterRefund2.data?.flow || []).length} 节点`)

  const me3 = await req('GET', '/api/member/me', undefined, buyerToken)
  check('TC-FR-07-02', '退款后按比例扣减积分', me3.code === 200, `当前积分 ${me3.data?.points}`)

  // 撤销（FR-06-4）：订单退款后回到已完成，可再次申请→撤销
  const asCancel = await req('POST', '/api/aftersales', { orderNo: o1.orderNo, skuId: cheap.id, type: 'REFUND_ONLY', reason: '不想要了', images: [] }, buyerToken)
  const cancelRes = await req('POST', `/api/aftersales/${asCancel.data?.afterSaleNo}/cancel`, undefined, buyerToken)
  const asCancelled = await req('GET', `/api/aftersales/${asCancel.data?.afterSaleNo}`, undefined, buyerToken)
  check('TC-FR-06-04', '买家可在审核前撤销售后', cancelRes.code === 200 && asCancelled.data?.status === 'CLOSED', asCancelled.data?.status)

  // 退货退款全链路（FR-13-2）：申请→审核通过→买家寄回→商家确认收货→退款
  const asReturn = await req(
    'POST',
    '/api/aftersales',
    { orderNo: o1.orderNo, skuId: cheap.id, type: 'RETURN_REFUND', reason: '质量问题', description: '外包装受潮', images: ['/img/proof2.jpg'] },
    buyerToken
  )
  const returnNo = asReturn.data?.afterSaleNo
  await req('POST', `/admin-api/aftersales/${returnNo}/audit`, { approve: true, opinion: '同意退货，请寄回' }, adminToken)
  const afterApprove = await req('GET', `/api/aftersales/${returnNo}`, undefined, buyerToken)
  check('TC-FR-13-02', '退货退款审核通过进入待买家寄回', afterApprove.data?.status === 'WAIT_RETURN', afterApprove.data?.statusText)

  const retLogiEmpty = await req('POST', `/api/aftersales/${returnNo}/return-logistics`, { company: '', no: '' }, buyerToken)
  check('FC-067', '退货物流公司/单号必填校验', retLogiEmpty.code !== 200, retLogiEmpty.msg)

  const logistics = await req('POST', `/api/aftersales/${returnNo}/return-logistics`, { company: '顺丰速运', no: 'SF' + Date.now() }, buyerToken)
  check('TC-FR-06-02', '买家填写退货物流单号', logistics.code === 200, logistics.msg)

  const recv = await req('POST', `/admin-api/aftersales/${returnNo}/confirm-return`, undefined, adminToken)
  const afterRecv = await req('GET', `/admin-api/aftersales/${returnNo}`, undefined, adminToken)
  check('TC-FR-13-02', '商家确认收货后触发退款（退款中/已退款）', recv.code === 200 && ['REFUNDING', 'REFUNDED'].includes(afterRecv.data?.status), afterRecv.data?.statusText)

  const refundReturn = await req('POST', `/admin-api/aftersales/${returnNo}/refund`, undefined, adminToken)
  const afterReturn = await req('GET', `/api/aftersales/${returnNo}`, undefined, buyerToken)
  check('TC-FR-13-02', '退货退款完成并回补库存', refundReturn.code === 200 && afterReturn.data?.status === 'REFUNDED', `${afterReturn.data?.status}，退款 ${yuan(afterReturn.data?.refundAmount || 0)} 元`)
  check('TC-FR-06-05', '退货退款流转记录 6 节点', (afterReturn.data?.flow || []).length >= 6, `${(afterReturn.data?.flow || []).length} 节点`)

  const asLimit = await req('POST', '/api/aftersales', { orderNo: o1.orderNo, skuId: cheap.id, type: 'REFUND_ONLY', reason: '其他' }, buyerToken)
  check('TC-FR-06-04', '同一订单每 SKU 售后次数上限 3 次（第 4 次被拒）', asLimit.code !== 200 && /3 次/.test(asLimit.msg || ''), asLimit.msg)

  // ===================== 八、到店自提与核销（FR-04-8 / ST） =====================
  const stores = await req('GET', '/api/stores')
  const storeId = stores.data?.[0]?.id
  check('TC-ST-01', '单店模式：仅默认门店且无切换', stores.code === 200 && (stores.data || []).length === 1 && stores.data[0].name === '正隆记总号', `门店 ${(stores.data || []).map((s) => s.name).join('/')}`)

  const pickupOrder = await req(
    'POST',
    '/api/orders',
    { idempotentKey: 'e2e-' + Date.now() + '-p', fulfillmentType: 'PICKUP', storeId, usePoints: false, items: [{ skuId: cheap.id, quantity: 1 }] },
    buyerToken
  )
  check('TC-FR-04-08', '到店自提下单免运费', pickupOrder.code === 200 && pickupOrder.data?.freight === 0, `运费 ${yuan(pickupOrder.data?.freight || 0)} 元`)
  await req('POST', `/api/orders/${pickupOrder.data.orderNo}/pay`, undefined, buyerToken)
  const pickupDetail = await req('GET', `/api/orders/${pickupOrder.data.orderNo}`, undefined, buyerToken)
  check('TC-FR-04-08', '自提支付后进入待自提', pickupDetail.data?.status === 'PICKUP_WAIT', pickupDetail.data?.statusText)
  
    // BUG-13 回归锚点：管理端待自提行返回门店名（核销工作台门店列数据源）
    const pickupAdminRow = ((await req('GET', '/admin-api/orders?status=PICKUP_WAIT&page=1&size=20', undefined, adminToken)).data?.rows || []).find((r) => r.orderNo === pickupOrder.data.orderNo)
    check('TC-ST-01', 'BUG-13 回归：待自提行返回门店名', pickupAdminRow?.storeName === '正隆记总号', `storeName=${pickupAdminRow?.storeName}`)

  const vc1 = await req('GET', `/api/orders/${pickupOrder.data.orderNo}/verify-code`, undefined, buyerToken)
  check('TC-AC-07', '动态核销码 6 位（每分钟刷新）', vc1.code === 200 && /^\d{6}$/.test(String(vc1.data?.code)), `码 ${vc1.data?.code}, 剩余 ${vc1.data?.expiresInSec}s`)

  const vc2 = await req('GET', `/api/orders/${pickupOrder.data.orderNo}/verify-code`, undefined, buyerToken)
  const vcStable = vc2.data?.code === vc1.data?.code || (vc1.data?.expiresInSec || 0) < 5
  check('TC-AC-07', '同一分钟窗口内核销码稳定（防截图盗用）', vc2.code === 200 && vcStable, `码1=${vc1.data?.code} 码2=${vc2.data?.code}`)

  // 字段级校验：核销（FC-052 / FC-053）
  const vNoParam = await req('POST', '/admin-api/verify', { orderNo: pickupOrder.data.orderNo, code: null, storeId }, adminToken)
  check('FC-052', '核销参数不完整被拒', vNoParam.code !== 200, vNoParam.msg)
  const vNotPickup = await req('POST', '/admin-api/verify', { orderNo: o1.orderNo, code: '123456', storeId }, adminToken)
  check('FC-053', '非待自提订单核销被拒', vNotPickup.code !== 200 && /待自提/.test(vNotPickup.msg || ''), vNotPickup.msg)

  const verifyBad = await req('POST', '/admin-api/verify', { orderNo: pickupOrder.data.orderNo, code: '000000', storeId }, adminToken)
  check('TC-ST-02', '错误核销码被拒并返回原因', verifyBad.code !== 200, verifyBad.msg)

  const verifyOk = await req('POST', '/admin-api/verify', { orderNo: pickupOrder.data.orderNo, code: vc1.data?.code, storeId }, adminToken)
  const pickupAfter = await req('GET', `/api/orders/${pickupOrder.data.orderNo}`, undefined, buyerToken)
  check('TC-ST-03', '正确核销码完成核销→订单已完成', verifyOk.code === 200 && pickupAfter.data?.status === 'COMPLETED', verifyOk.msg)

  const records = await req('GET', '/admin-api/verify-records?page=1&size=10', undefined, adminToken)
  check('TC-ST-03', '核销记录留痕可查（店员/时间/门店）', records.code === 200 && (records.data?.rows || []).some((r) => r.orderNo === pickupOrder.data.orderNo), `记录 ${records.data?.total} 条`)

  // ===================== 九、库存管理（FR-11） =====================
  const skus = await req('GET', '/admin-api/skus?page=1&size=50', undefined, adminToken)
  const allBalance = (skus.data?.rows || []).every((s) => s.physical === s.available + s.reserved + s.frozen)
  check('TC-FR-11-01', '库存三维恒等式：实物=可用+预占+冻结', skus.code === 200 && allBalance, `SKU ${skus.data?.rows?.length} 个`)

  const target = (skus.data?.rows || []).find((s) => s.id === big.id) || skus.data?.rows?.[0]
  const inboundBad = await req('POST', '/admin-api/stock/inbounds', { skuId: target.id, quantity: 10, batchNo: '' }, adminToken)
  check('TC-FR-11-02', '入库批次号必填校验', inboundBad.code !== 200, inboundBad.msg)

  const physicalBefore = target.physical
  const inbound = await req('POST', '/admin-api/stock/inbounds', { skuId: target.id, quantity: 10, batchNo: 'BATCH-' + Date.now(), supplier: '湘潭一厂', remark: 'E2E 入库' }, adminToken)
  const skusAfterIn = await req('GET', '/admin-api/skus?page=1&size=50', undefined, adminToken)
  const targetAfterIn = (skusAfterIn.data?.rows || []).find((s) => s.id === target.id)
  check('TC-FR-11-02', '入库单确认后实物库存增加', inbound.code === 200 && targetAfterIn?.physical === physicalBefore + 10, `实物 ${physicalBefore} → ${targetAfterIn?.physical}`)

  // 字段级校验：库存管理（FC-080 / FC-083 ~ FC-086）
  const inZeroQty = await req('POST', '/admin-api/stock/inbounds', { skuId: target.id, quantity: 0, batchNo: 'B-ZERO' }, adminToken)
  check('FC-080', '入库数量必须大于 0', inZeroQty.code !== 200, inZeroQty.msg)
  const badAction = await req('POST', '/admin-api/stock/freeze', { skuId: target.id, action: 'LOCK', reason: 'FC' }, adminToken)
  check('FC-083', '冻结/释放 action 枚举校验', badAction.code !== 200, badAction.msg)
  const badThreshold = await req('POST', '/admin-api/stock/threshold', { skuId: target.id, threshold: -5 }, adminToken)
  check('FC-084', '安全阈值非法值拒绝', badThreshold.code !== 200, badThreshold.msg)
  const stEmpty = await req('POST', '/admin-api/stock/stocktakes', { items: [], remark: 'FC' }, adminToken)
  check('FC-085', '盘点明细必填', stEmpty.code !== 200, stEmpty.msg)
  const stNegActual = await req('POST', '/admin-api/stock/stocktakes', { items: [{ skuId: target.id, actual: -1 }], remark: 'FC' }, adminToken)
  check('FC-086', '盘点实盘数非法值拒绝', stNegActual.code !== 200, stNegActual.msg)

  const flows = await req('GET', `/admin-api/stock/flows?skuId=${target.id}&page=1&size=20`, undefined, adminToken)
  check('TC-FR-11-07', '库存流水完整可追溯（含 IN 与关联单号）', flows.code === 200 && (flows.data?.rows || []).some((f) => f.type === 'IN' && f.relatedNo))

  const freeze = await req('POST', '/admin-api/stock/freeze', { skuId: target.id, action: 'FREEZE', reason: 'E2E 质检冻结' }, adminToken)
  const frozenSku = (await req('GET', '/admin-api/skus?page=1&size=50', undefined, adminToken)).data.rows.find((s) => s.id === target.id)
  check('TC-FR-11-04', '冻结后可用库存清零（预占不受影响）', freeze.code === 200 && frozenSku?.available === 0, `可用 ${frozenSku?.available}`)
  const inFrozen = await req('POST', '/admin-api/stock/inbounds', { skuId: target.id, quantity: 5, batchNo: 'B-FROZEN-' + Date.now() }, adminToken)
  check('FC-082', '冻结 SKU 禁止入库', inFrozen.code !== 200, inFrozen.msg)

  const buyFrozen = await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-f', fulfillmentType: 'EXPRESS', addressId, items: [{ skuId: target.id, quantity: 1 }] }, buyerToken)
  check('TC-FR-11-04', '冻结 SKU 买家端立即不可购', buyFrozen.code !== 200, buyFrozen.msg)
  await req('POST', '/admin-api/stock/freeze', { skuId: target.id, action: 'RELEASE', reason: 'E2E 解除冻结' }, adminToken)
  const released = (await req('GET', '/admin-api/skus?page=1&size=50', undefined, adminToken)).data.rows.find((s) => s.id === target.id)
  check('TC-FR-11-04', '释放冻结恢复可用', released?.available > 0, `可用 ${released?.available}`)

  const stocktake = await req('POST', '/admin-api/stock/stocktakes', { items: [{ skuId: target.id, actual: released.physical - 2 }], remark: 'E2E 盘点' }, adminToken)
  const stocktakeId = stocktake.data?.id
  check('TC-FR-11-03', '盘点单创建并自动计算差异', stocktake.code === 200 && !!stocktakeId, `ID ${stocktakeId}`)
  const selfConfirm = await req('POST', `/admin-api/stock/stocktakes/${stocktakeId}/confirm`, undefined, adminToken)
  check('TC-FR-11-03', '提交人自确认被拒（二级审批）', selfConfirm.code !== 200, selfConfirm.msg)
  const confirmSt = await req('POST', `/admin-api/stock/stocktakes/${stocktakeId}/confirm`, undefined, managerToken)
  const adjustFlow = await req('GET', `/admin-api/stock/flows?skuId=${target.id}&type=ADJUST&page=1&size=10`, undefined, adminToken)
  check('TC-FR-11-03', '第二人确认后按差异修正并生成 ADJUST 流水', confirmSt.code === 200 && (adjustFlow.data?.rows || []).length >= 1, confirmSt.msg)

  const threshold = await req('POST', '/admin-api/stock/threshold', { skuId: target.id, threshold: 30 }, adminToken)
  check('TC-FR-11-06', '安全阈值可配（留审计）', threshold.code === 200, threshold.msg)
  const auditForThreshold = await req('GET', '/admin-api/audit-logs?page=1&size=20', undefined, adminToken)
  check('TC-PL-03', '审计日志记录阈值修改（操作人/前后值）', (auditForThreshold.data?.rows || []).some((l) => /阈值|库存/.test(l.action + l.module)))

  // ===================== 十、商品上下架与可见性（FR-10-3 / E2E 收尾） =====================
  const createdBarcode = 'E2E-' + Date.now()
  const created = await req(
    'POST',
    '/admin-api/products',
    {
      name: 'E2E 测试商品 上下架用',
      categoryId: 1,
      brand: '正隆记',
      origin: '湖南湘潭',
      description: 'E2E 上下架验证',
      images: ['/img/t1.jpg', '/img/t2.jpg', '/img/t3.jpg'],
      detailImages: [],
      skus: [{ flavor: '原味', spec: '单包', barcode: createdBarcode, price: 1200, costPrice: 600, stock: 20 }]
    },
    adminToken
  )
  check('TC-FR-10-01', '商品新增成功（SKU 完整性校验通过）', created.code === 200, created.msg)
  const createdId = created.data?.id
  await req('POST', '/admin-api/products/status', { ids: [createdId], status: 'ON' }, adminToken)
  const buyerHasNew = await req('GET', '/api/products?keyword=' + encodeURIComponent('E2E 测试商品'))
  check('TC-FR-10-03', '新商品上架后买家端可见', (buyerHasNew.data?.rows || []).some((p) => p.id === createdId))

  const off = await req('POST', '/admin-api/products/status', { ids: [createdId], status: 'OFF' }, adminToken)
  const buyerAfterOff = await req('GET', '/api/products?keyword=' + encodeURIComponent('E2E 测试商品'))
  check('TC-FR-10-03', '批量下架即时生效（买家端不可见）', off.code === 200 && !(buyerAfterOff.data?.rows || []).some((p) => p.id === createdId))

  const detailOff = await req('GET', `/api/products/${createdId}`)
  check('TC-FR-02-04', '下架商品详情返回已下架态', detailOff.code === 200 && detailOff.data?.status === 'OFF', detailOff.data?.status)
  const orderOffProduct = await req('POST', '/api/orders', { idempotentKey: 'e2e-' + Date.now() + '-g', fulfillmentType: 'EXPRESS', addressId, items: [{ skuId: (await req('GET', `/api/products/${createdId}`)).data.skus[0].id, quantity: 1 }] }, buyerToken)
  check('TC-FR-02-04', '下架商品禁止下单', orderOffProduct.code !== 200, orderOffProduct.msg)

  // 到货提醒（FR-02-7）：下架商品可登记，在售商品拒绝
  const remindOff = await req('POST', `/api/products/${createdId}/remind`, undefined, buyerToken)
  check('TC-FR-02-07', '下架商品可登记到货提醒', remindOff.code === 200, remindOff.msg)
  const remindOn = await req('POST', `/api/products/${pid}/remind`, undefined, buyerToken)
  check('TC-FR-02-07', '在售商品登记到货提醒被拒', remindOn.code !== 200, remindOn.msg)

  // 购物车失效行（FR-03-4）：下架商品行 invalid=true，可一键清除
  await req('POST', '/api/cart', { skuId: (await req('GET', `/api/products/${createdId}`)).data.skus[0].id, quantity: 1 }, buyerToken)
  const cartWithInvalid = await req('GET', '/api/cart', undefined, buyerToken)
  check('TC-FR-03-04', '下架商品购物车行标记失效', cartWithInvalid.code === 200 && (cartWithInvalid.data?.rows || []).some((r) => r.invalid), `共 ${(cartWithInvalid.data?.rows || []).length} 行`)
  const cleared = await req('POST', '/api/cart/clear-invalid', undefined, buyerToken)
  const cartCleared = await req('GET', '/api/cart', undefined, buyerToken)
  check('TC-FR-03-04', '一键清除失效行', cleared.code === 200 && !(cartCleared.data?.rows || []).some((r) => r.invalid), `剩余 ${(cartCleared.data?.rows || []).length} 行`)

  // 管理端商品编辑改价（FR-10-2）：改价后买家端同步可见
  const editRes = await req(
    'PUT',
    `/admin-api/products/${createdId}`,
    {
      name: 'E2E 测试商品 上下架用',
      categoryId: 1,
      brand: '正隆记',
      origin: '湖南湘潭',
      description: 'E2E 上下架验证（改价后）',
      images: ['/img/t1.jpg', '/img/t2.jpg', '/img/t3.jpg'],
      detailImages: [],
      skus: [{ flavor: '原味', spec: '单包', barcode: createdBarcode, price: 1500, costPrice: 600, stock: 20 }]
    },
    adminToken
  )
  const priceAfterEdit = (await req('GET', `/api/products/${createdId}`)).data.skus[0].price
  check('TC-FR-10-02', '商品编辑改价生效（买家端同步）', editRes.code === 200 && priceAfterEdit === 1500, `售价 ${priceAfterEdit} 分`)

  const auditAfterOff = await req('GET', '/admin-api/audit-logs?page=1&size=20', undefined, adminToken)
  check('TC-PL-03', '上下架操作写入审计日志', (auditAfterOff.data?.rows || []).some((l) => /上下架|商品状态/.test(l.action + l.detail)))

  const auditModule = auditAfterOff.data?.rows?.[0]?.module
  const auditFiltered = auditModule
    ? await req('GET', `/admin-api/audit-logs?page=1&size=20&module=${encodeURIComponent(auditModule)}`, undefined, adminToken)
    : null
  check(
    'TC-PL-03',
    '审计日志按模块筛选生效且字段完整',
    !!auditFiltered && auditFiltered.code === 200 && (auditFiltered.data?.rows || []).every((l) => l.module === auditModule && l.operator && l.createdAt),
    `模块 ${auditModule}，命中 ${(auditFiltered?.data?.rows || []).length} 条`
  )

  // ===================== 十一、会员管理（FR-14） =====================
  const members = await req('GET', '/admin-api/members?page=1&size=10', undefined, adminToken)
  const memberRow = (members.data?.rows || []).find((m) => (m.phone || '').includes('****'))
  check('TC-FR-14-01', '会员列表手机号脱敏', members.code === 200 && !!memberRow, `共 ${members.data?.total} 人`)
  // 积分调整目标取当前买家本人，避免后续注册的会员排在列表首位导致错调
  const meInfo = await req('GET', '/api/member/me', undefined, buyerToken)
  const memberId = meInfo.data?.id ?? memberRow?.id
  const pointsAdj = await req('POST', `/admin-api/members/${memberId}/points`, { change: 50, reason: 'E2E 客诉补偿' }, adminToken)
  const logs = await req('GET', '/api/member/points/logs?page=1&size=10', undefined, buyerToken)
  check('TC-FR-14-03', '人工调整积分并留痕（原因必填）', pointsAdj.code === 200 && (logs.data?.rows || []).some((l) => /客诉补偿/.test(l.reason || '')))
  const adjustNoReason = await req('POST', `/admin-api/members/${memberId}/points`, { change: 10, reason: '' }, adminToken)
  check('TC-FR-14-03', '积分调整原因必填校验', adjustNoReason.code !== 200, adjustNoReason.msg)

  const adjustZero = await req('POST', `/admin-api/members/${memberId}/points`, { change: 0, reason: 'FC 校验' }, adminToken)
  check('FC-090', '积分变动值为 0 被拒', adjustZero.code !== 200, adjustZero.msg)

  const pointLogsFull = await req('GET', '/api/member/points/logs?page=1&size=20', undefined, buyerToken)
  check(
    'TC-FR-07-01',
    '积分流水字段完整（变动值/变动后余额/原因）',
    (pointLogsFull.data?.rows || []).length > 0 && pointLogsFull.data.rows.every((l) => 'change' in l && 'balanceAfter' in l && 'reason' in l),
    `流水 ${(pointLogsFull.data?.rows || []).length} 条`
  )

  const rules = await req('GET', '/admin-api/level-rules', undefined, adminToken)
  const newRules = (rules.data || []).map((r) => ({ levelCode: r.levelCode, levelName: r.levelName, threshold: r.threshold }))
  check('TC-FR-14-02', '等级规则读取（V0~V3 四档阈值）', rules.code === 200 && (rules.data || []).length >= 4, (rules.data || []).map((r) => `${r.levelCode}≥${yuan(r.threshold)}`).join(' '))
  const saveRules = await req('PUT', '/admin-api/level-rules', newRules, adminToken)
  check('TC-FR-14-02', '等级规则可配置（留审计）', saveRules.code === 200, saveRules.msg)

  // 等级升级联动（FR-07-1）：把 V1 阈值临时调到 1 分，支付后应升档（只升不降），再恢复原阈值
  const rulesLow = newRules.map((r) => (r.levelCode === 'V1' ? { ...r, threshold: 1 } : r))
  await req('PUT', '/admin-api/level-rules', rulesLow, adminToken)
  const upgradeOrder = await req(
    'POST',
    '/api/orders',
    { idempotentKey: 'e2e-' + Date.now() + '-lv', fulfillmentType: 'PICKUP', storeId: storeNow.id, usePoints: false, items: [{ skuId: cheap.id, quantity: 1 }] },
    buyerToken
  )
  if (upgradeOrder.code === 200) await req('POST', `/api/orders/${upgradeOrder.data.orderNo}/pay`, undefined, buyerToken)
  const meAfterLevel = await req('GET', '/api/member/me', undefined, buyerToken)
  check(
    'TC-FR-07-01',
    '累计实付达阈值后自动升档（只升不降）',
    meAfterLevel.data?.levelCode !== 'V0',
    `当前等级 ${meAfterLevel.data?.levelCode} / ${meAfterLevel.data?.levelName}`
  )
  await req('PUT', '/admin-api/level-rules', newRules, adminToken)

  // 说明：字段级校验中 FC-022（购物车 100 种类上限）与 FC-064（售后 7 天签收时限）
  // 受演示数据 SKU 规模与时间约束，转手工验证（见 docs/测试用例.md 六、M-08/M-09）。

  // ===================== 汇总 =====================
  const passed = results.filter((r) => r.pass).length
  const failed = results.length - passed
  const summary = {
    runAt: new Date().toLocaleString('zh-CN', { hour12: false }),
    base: B,
    total: results.length,
    passed,
    failed,
    rate: ((passed / results.length) * 100).toFixed(1),
    results
  }
  writeFileSync(new URL('./last-run.json', import.meta.url), JSON.stringify(summary, null, 2), 'utf-8')
  console.log(`\n===== E2E 汇总：${passed}/${results.length} 通过，失败 ${failed}，通过率 ${summary.rate}% =====`)
  if (failed > 0) process.exitCode = 1
})().catch((e) => {
  console.error('E2E 执行异常：', e)
  process.exitCode = 1
})
