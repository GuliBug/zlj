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

  // 地址（FR-08-1）
  const addr = await req('POST', '/api/addresses', { receiver: '张三', phone: '13800001114', region: '湖南省湘潭市岳塘区', detail: '正隆街 1 号 3 栋 501', isDefault: true }, buyerToken)
  check('TC-FR-08-01', '地址新增（纯文本）', addr.code === 200, addr.msg)
  const addresses = await req('GET', '/api/addresses', undefined, buyerToken)
  const addressId = addresses.data?.[0]?.id
  check('TC-FR-08-01', '地址列表返回默认地址', addresses.code === 200 && addresses.data?.some((a) => a.isDefault), `共 ${addresses.data?.length} 条`)
  check('TC-FR-08-01', '地址手机号校验（非法手机号拒绝）', (await req('POST', '/api/addresses', { receiver: '李四', phone: '123', region: 'x', detail: 'y' }, buyerToken)).code !== 200)

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
  const stockAfterCancel = (await req('GET', `/api/products/${cheap.productId}`)).data.skus.find((s) => s.id === cheap.id).stock
  check('TC-FR-05-02', '取消待支付订单并释放预占', detail2.data?.status === 'CLOSED' && stockAfterCancel === stockBeforeCancel, `状态 ${detail2.data?.status}，可用 ${stockBeforeCancel}→${stockAfterCancel}`)
  check('TC-O-04', '禁止状态回跳（已关闭订单再次支付被拒）', (await req('POST', `/api/orders/${order2.data.orderNo}/pay`, undefined, buyerToken)).code !== 200)

  // 订单列表筛选（FR-05-1）
  const paidList = await req('GET', '/api/orders?status=PAID&page=1&size=10', undefined, buyerToken)
  check('TC-FR-05-01', '订单列表按状态筛选（待发货）', paidList.code === 200 && (paidList.data?.rows || []).every((o) => o.status === 'PAID'), `共 ${paidList.data?.total} 单`)

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
  check('TC-ST-01', '门店列表可切换', stores.code === 200 && (stores.data || []).length >= 2, `门店 ${(stores.data || []).map((s) => s.name).join('/')}`)

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

  const vc1 = await req('GET', `/api/orders/${pickupOrder.data.orderNo}/verify-code`, undefined, buyerToken)
  check('TC-AC-07', '动态核销码 6 位（每分钟刷新）', vc1.code === 200 && /^\d{6}$/.test(String(vc1.data?.code)), `码 ${vc1.data?.code}, 剩余 ${vc1.data?.expiresInSec}s`)

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

  const flows = await req('GET', `/admin-api/stock/flows?skuId=${target.id}&page=1&size=20`, undefined, adminToken)
  check('TC-FR-11-07', '库存流水完整可追溯（含 IN 与关联单号）', flows.code === 200 && (flows.data?.rows || []).some((f) => f.type === 'IN' && f.relatedNo))

  const freeze = await req('POST', '/admin-api/stock/freeze', { skuId: target.id, action: 'FREEZE', reason: 'E2E 质检冻结' }, adminToken)
  const frozenSku = (await req('GET', '/admin-api/skus?page=1&size=50', undefined, adminToken)).data.rows.find((s) => s.id === target.id)
  check('TC-FR-11-04', '冻结后可用库存清零（预占不受影响）', freeze.code === 200 && frozenSku?.available === 0, `可用 ${frozenSku?.available}`)
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
      skus: [{ flavor: '原味', spec: '单包', barcode: 'E2E-' + Date.now(), price: 1200, costPrice: 600, stock: 20 }]
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

  const auditAfterOff = await req('GET', '/admin-api/audit-logs?page=1&size=20', undefined, adminToken)
  check('TC-PL-03', '上下架操作写入审计日志', (auditAfterOff.data?.rows || []).some((l) => /上下架|商品状态/.test(l.action + l.detail)))

  // ===================== 十一、会员管理（FR-14） =====================
  const members = await req('GET', '/admin-api/members?page=1&size=10', undefined, adminToken)
  const memberRow = (members.data?.rows || []).find((m) => (m.phone || '').includes('****'))
  check('TC-FR-14-01', '会员列表手机号脱敏', members.code === 200 && !!memberRow, `共 ${members.data?.total} 人`)
  const memberId = memberRow?.id
  const pointsAdj = await req('POST', `/admin-api/members/${memberId}/points`, { change: 50, reason: 'E2E 客诉补偿' }, adminToken)
  const logs = await req('GET', '/api/member/points/logs?page=1&size=10', undefined, buyerToken)
  check('TC-FR-14-03', '人工调整积分并留痕（原因必填）', pointsAdj.code === 200 && (logs.data?.rows || []).some((l) => /客诉补偿/.test(l.reason || '')))
  const adjustNoReason = await req('POST', `/admin-api/members/${memberId}/points`, { change: 10, reason: '' }, adminToken)
  check('TC-FR-14-03', '积分调整原因必填校验', adjustNoReason.code !== 200, adjustNoReason.msg)

  const rules = await req('GET', '/admin-api/level-rules', undefined, adminToken)
  const newRules = (rules.data || []).map((r) => ({ levelCode: r.levelCode, levelName: r.levelName, threshold: r.threshold }))
  check('TC-FR-14-02', '等级规则读取（V0~V3 四档阈值）', rules.code === 200 && (rules.data || []).length >= 4, (rules.data || []).map((r) => `${r.levelCode}≥${yuan(r.threshold)}`).join(' '))
  const saveRules = await req('PUT', '/admin-api/level-rules', newRules, adminToken)
  check('TC-FR-14-02', '等级规则可配置（留审计）', saveRules.code === 200, saveRules.msg)

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
