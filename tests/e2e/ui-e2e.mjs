// UI 级端到端验证：走真实浏览器操作路径，断言关键操作元素「可见且可点击」（未被遮挡）
// 覆盖 UC-01~UC-21（核心交易链路 + 全部页面渲染 + 售后真实申请 + 401 回跳）；前置：后端 8080 + H5 10086 已启动
// 每个断言点（页面渲染 / 按钮点击后）同步截图，归档到 tests/e2e/screenshots/h5
import { createRequire } from 'node:module'
import path from 'node:path'
import fs from 'node:fs'
import { fileURLToPath } from 'node:url'
const require = createRequire(import.meta.url)
const puppeteer = require(new URL('../../frontend-web/node_modules/puppeteer', import.meta.url).pathname.replace(/^\/([A-Za-z]):/, '$1:'))
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const OUT_DIR = path.join(ROOT, 'tests/e2e/screenshots/h5')
fs.mkdirSync(OUT_DIR, { recursive: true })

const results = []
let shotSeq = 0
function check(id, name, pass, detail = '') {
  results.push({ id, name, pass, detail })
  console.log(`${pass ? 'PASS' : 'FAIL'} ${id} ${name}${detail ? ' | ' + detail : ''}`)
}
// 断言 + 截图一体：每个步骤落一张 PNG（01-UC-01.png …）
async function step(id, name, pass, detail = '') {
  check(id, name, pass, detail)
  shotSeq++
  const file = `${String(shotSeq).padStart(2, '0')}-${id}.png`
  try {
    await page.screenshot({ path: path.join(OUT_DIR, file) })
    console.log(`📸 截图 ${file}`)
  } catch (e) {
    console.log(`截图失败 ${id}: ${e.message}`)
  }
}

// 判断元素是否「视觉可点」：中心点不被其他元素遮挡
async function visuallyClickable(page, selector) {
  return page.evaluate((sel) => {
    const el = document.querySelector(sel)
    if (!el) return { ok: false, why: '元素不存在' }
    const r = el.getBoundingClientRect()
    if (r.width === 0 || r.height === 0) return { ok: false, why: '尺寸为 0' }
    const cx = r.left + r.width / 2, cy = r.top + r.height / 2
    if (cy < 0 || cy > innerHeight) return { ok: false, why: `中心点在视口外 y=${Math.round(cy)}` }
    const top = document.elementFromPoint(cx, cy)
    if (el === top || el.contains(top) || (top && top.contains(el))) return { ok: true }
    return { ok: false, why: `被 ${top ? top.className || top.tagName : 'null'} 遮挡` }
  }, selector)
}

const browser = await puppeteer.launch({
  headless: 'new',
  executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  args: ['--no-sandbox', '--disable-gpu']
})
const page = await browser.newPage()
await page.setViewport({ width: 390, height: 844, isMobile: true })

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

// —— 接口级数据准备：为售后申请用例造一笔已完成订单（买家 13900002222）——
const B = 'http://localhost:8080'
async function api(method, p, body, token) {
  const r = await fetch(B + p, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { 'X-Token': token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  return r.json()
}
let asOrderNo = ''
let mediaProdId = 0
try {
  const admin = (await api('POST', '/admin-api/login', { username: 'admin', password: 'admin123' })).data?.token
  const bt2 = (await api('POST', '/api/member/login', { phone: '13900002222', smsCode: '123456' })).data?.token
  const prods = (await api('GET', '/api/products?page=1&size=20')).data?.rows || []
  const pool = []
  for (const p of prods) {
    const d = await api('GET', `/api/products/${p.id}`)
    for (const s of d.data?.skus || []) if (s.stock > 3 && s.price < 500000) pool.push(s)
  }
  const sku = pool.sort((a, b) => a.price - b.price)[0]
  if (bt2 && sku) {
    await api('POST', '/api/addresses', { receiver: '售后预备', phone: '13900002222', region: '湖南省湘潭市', detail: 'UI 售后预备地址 1 号' }, bt2)
    const addrId = ((await api('GET', '/api/addresses', undefined, bt2)).data || [])[0]?.id
    const o = await api('POST', '/api/orders', { idempotentKey: 'uias-' + Date.now(), fulfillmentType: 'EXPRESS', addressId: addrId, items: [{ skuId: sku.id, quantity: 1 }] }, bt2)
    if (o.code === 200) {
      await api('POST', `/api/orders/${o.data.orderNo}/pay`, undefined, bt2)
      await api('POST', `/admin-api/orders/${o.data.orderNo}/ship`, { company: '顺丰速运', no: 'SFAS' + Date.now() }, admin)
      await api('POST', `/api/orders/${o.data.orderNo}/confirm`, undefined, bt2)
      asOrderNo = o.data.orderNo
    }
  }
  // H5-13 多媒体用例数据：管理端创建多主图 + 详情图商品（dataURL 真实可渲染，避免依赖手工上传）
  try {
    const imgData = 'data:image/png;base64,' + fs.readFileSync(path.join(ROOT, 'tests/e2e/assets/sample.png')).toString('base64')
    const mediaCreated = await api('POST', '/admin-api/products', {
      name: 'UI 多媒体验证商品', categoryId: 1, brand: '正隆记', origin: '湖南湘潭', description: 'UI E2E 轮播与详情图用例',
      images: [imgData, imgData], detailImages: [imgData, imgData], status: 'ON',
      skus: [{ flavor: '原味', spec: '30/10', barcode: 'UIMD-' + Date.now(), price: 500, costPrice: 300, stock: 10 }]
    }, admin)
    mediaProdId = mediaCreated.data?.id || 0
  } catch (em) {
    console.log('多媒体用例数据准备失败：' + em.message)
  }
} catch (e) {
  console.log('售后数据准备失败：' + e.message)
}

try {
  // UC-01 登录（真实页面输入；H5 为 hash 路由）
  await page.goto('http://localhost:10086/#/login', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForSelector('input[type="tel"]', { timeout: 30000 })
  await sleep(800)
  const inputs = await page.$$('input[type="tel"]')
  await inputs[0].type('13900002222')
  if (inputs[1]) await inputs[1].type('123456')
  await page.evaluate(() => {
    const btn = [...document.querySelectorAll('button')].find((b) => /登录|注册/.test(b.innerText))
    if (btn) btn.click()
  })
  await sleep(2000)
  const token = await page.evaluate(() => localStorage.getItem('token'))
  await step('UC-01', 'H5 页面真实登录成功', !!token, token ? token.slice(0, 8) : 'localStorage 无 token')

  // UC-02 加入购物车（详情页真实点击：打开 SKU 弹层 → 选规格 → 确定）
  await page.goto('http://localhost:10086/#/product/1', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1800)
  await page.evaluate(() => {
    const btn = [...document.querySelectorAll('button')].find((b) => /加入购物车/.test(b.innerText))
    if (btn) btn.click()
  })
  await sleep(1200)
  const skuPicked = await page.evaluate(() => {
    const sheet = document.querySelector('.sheet')
    if (!sheet) return 'no-sheet'
    const opt = [...sheet.querySelectorAll('button')].find((b) => !b.disabled && !/确定|－|＋/.test(b.innerText))
    if (opt) { opt.click(); return 'picked' }
    return 'no-option'
  })
  await sleep(500)
  await page.evaluate(() => {
    const ok = document.querySelector('.sheet .sku-ok')
    if (ok && !ok.disabled) ok.click()
  })
  await sleep(1500)
  // 真实断言：购物车接口数量 > 0（角标在 UC-03 购物车页断言）
  const cart = await page.evaluate(async () => {
    const token = localStorage.getItem('token')
    const r = await fetch('/api/cart', { headers: { 'X-Token': token } })
    const j = await r.json()
    return j.data?.totalQuantity ?? 0
  })
  await step('UC-02', '详情页加入购物车（购物车数量增加）', cart > 0, `totalQuantity=${cart}`)

  // UC-03 购物车结算按钮可见且未被遮挡（回归：曾固定 bottom:0 被 TabBar 盖住）
  await page.goto('http://localhost:10086/#/cart', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1800)
  const badge = await page.evaluate(() => document.querySelector('.tab-badge')?.innerText || '')
  await step('UC-02B', '购物车页角标与数量同步', badge !== '' && Number(badge) === cart, `badge=${badge || '无'}`)
  const c1 = await visuallyClickable(page, '.bottom-bar .btn')
  await step('UC-03', '购物车「结算」按钮可见且可点击', c1.ok, c1.why || '')

  // UC-04 点击结算 → 确认订单页可达，提交按钮可用
  if (c1.ok) {
    await page.evaluate(() => document.querySelector('.bottom-bar .btn').click())
    await sleep(2000)
    const onCheckout = page.url().includes('/checkout')
    const c2 = onCheckout ? await visuallyClickable(page, '.bottom-bar .btn') : { ok: false, why: '未跳转到确认订单页' }
    await step('UC-04', '确认订单页「提交订单」可见且可点击', c2.ok, c2.why || `url=${page.url()}`)

    // UC-05 完整下单链路：切到店自提（新会员无地址）→ 提交 → 页内支付按钮 → 支付跳转
    if (c2.ok) {
      await page.evaluate(() => {
        const tab = [...document.querySelectorAll('.seg > div')].find((t) => /自提/.test(t.innerText))
        if (tab) tab.click()
      })
      await sleep(800)
      await page.evaluate(() => {
        const btn = [...document.querySelectorAll('.bottom-bar button')].find((b) => /提交订单/.test(b.innerText))
        if (btn) btn.click()
      })
      await sleep(2500)
      const paid = await page.evaluate(() => {
        const btn = [...document.querySelectorAll('.bottom-bar button')].find((b) => /立即支付/.test(b.innerText))
        if (btn) { btn.click(); return true }
        return false
      })
      await sleep(2500)
      const onPayResult = /pay-result/.test(page.url())
      await step('UC-05', '提交订单→立即支付→跳转支付结果', paid && onPayResult, `url=${page.url()}`)
    }
  }

  // UC-06 商品详情吸底栏未被遮挡
  await page.goto('http://localhost:10086/#/product/1', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const c3 = await visuallyClickable(page, '.bottom-bar .btn')
  await step('UC-06', '商品详情吸底操作栏可见且可点击', c3.ok, c3.why || '')

  // ============ 以下为全页面覆盖（UC-07~UC-19） ============
  // 页内 API 助手：走相对路径（Vite 代理到 8080），带当前登录 token
  const pageApi = (p) =>
    page.evaluate(async (u) => {
      const r = await fetch(u, { headers: { 'X-Token': localStorage.getItem('token') } })
      return (await r.json()).data
    }, 'http://localhost:10086' + p)

  const orderRows = (await pageApi('/api/orders?page=1&size=10')) || { rows: [] }
  const anyOrder = (orderRows.rows || [])[0]
  const pickupOrder = (orderRows.rows || []).find((o) => o.status === 'PICKUP_WAIT')

  // UC-07 首页：警示条 + 分档 chips + 商品网格
  await page.goto('http://localhost:10086/#/', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1800)
  const home = await page.evaluate(() => ({
    chips: !!document.querySelector('.chips'),
    hero: !!document.querySelector('.hero'),
    warn: /长期过量咀嚼槟榔/.test(document.body.innerText),
    goods: /¥/.test(document.body.innerText)
  }))
  await step('UC-07', '首页渲染（警示条/分档 chips/商品网格）', home.chips && home.hero && home.warn && home.goods, JSON.stringify(home))

  // UC-08 全部商品页
  await page.goto('http://localhost:10086/#/category', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1800)
  const catOk = await page.evaluate(() => !!document.querySelector('.has-tabbar') && /¥/.test(document.body.innerText))
  await step('UC-08', '全部商品页按分类分节渲染', catOk)

  // UC-09 门店信息页（单店模式）
  await page.goto('http://localhost:10086/#/stores', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const storeOk = await page.evaluate(() => !!document.querySelector('.store-card') && /正隆记/.test(document.body.innerText))
  await step('UC-09', '门店信息页渲染（正隆记总号）', storeOk)

  // UC-10 会员中心（掌柜卡/四格统计/退出）
  await page.goto('http://localhost:10086/#/member', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1800)
  const memberOk = await page.evaluate(() => !!document.querySelector('.has-tabbar') && /积分/.test(document.body.innerText) && /退出/.test(document.body.innerText))
  await step('UC-10', '会员中心渲染（积分/退出入口）', memberOk)

  // UC-11 收货地址页
  await page.goto('http://localhost:10086/#/addresses', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const addrOk = await page.evaluate(() => /新增收货地址/.test(document.body.innerText))
  await step('UC-11', '收货地址页渲染（新增入口）', addrOk)

  // UC-12 订单列表 6 状态 Tab
  await page.goto('http://localhost:10086/#/orders', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1800)
  const tabCount = await page.evaluate(() => document.querySelectorAll('.otab').length)
  await step('UC-12', '订单列表 6 状态 Tab 渲染', tabCount === 6, `tabs=${tabCount}`)

  // UC-13 订单详情（时间轴 + 吸底操作）
  if (anyOrder) {
    await page.goto('http://localhost:10086/#/order/' + anyOrder.orderNo, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await sleep(1800)
    const odOk = await page.evaluate(() => !!document.querySelector('.has-bottom-bar') && /¥/.test(document.body.innerText))
    // BUG-11 回归锚点：详情页任何卡片不得渲染出字面量 undefined（收货信息曾因字段形态误用中招）
    const noUndef = await page.evaluate(() => !/undefined/.test(document.body.innerText))
    await step('UC-13', '订单详情渲染（金额明细+吸底操作栏+无 undefined 字样）', odOk && noUndef, `od=${odOk} noUndef=${noUndef}`)
  } else {
    await step('UC-13', '订单详情渲染（金额明细+吸底操作栏）', false, '无订单数据')
  }

  // UC-14 核销码页（无 orderId 引导态）
  await page.goto('http://localhost:10086/#/verify-code', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const vcGuide = await page.evaluate(() => !!document.querySelector('.hq-card') && /— — — —/.test(document.body.innerText))
  await step('UC-14', '核销码页引导态（当前门店+提示从订单进入）', vcGuide)

  // UC-15 核销码页（动态码态）
  if (pickupOrder) {
    await page.goto('http://localhost:10086/#/verify-code?orderId=' + pickupOrder.orderNo, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await sleep(2000)
    const vcDyn = await page.evaluate(() => (document.querySelector('.hq-card .code')?.innerText || '').replace(/\s/g, ''))
    await step('UC-15', '核销码页动态码展示（6 位）', /^\d{6}$/.test(vcDyn), `code=${vcDyn}`)
  } else {
    await step('UC-15', '核销码页动态码展示（6 位）', false, '无待自提订单')
  }

  // UC-16 支付结果页
  if (pickupOrder) {
    await page.goto('http://localhost:10086/#/pay-result/' + pickupOrder.orderNo, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await sleep(1800)
    const prOk = await page.evaluate(() => !!document.querySelector('.result .ok') && /下单成功/.test(document.body.innerText))
    await step('UC-16', '支付结果页渲染（下单成功引导）', prOk)
  } else {
    await step('UC-16', '支付结果页渲染（下单成功引导）', false, '无已支付订单')
  }

  // UC-17 售后申请页（说明卡 + 类型/原因下拉 + 提交按钮）
  if (anyOrder) {
    await page.goto('http://localhost:10086/#/aftersale/new/' + anyOrder.orderNo, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await sleep(1800)
    const asOk = await page.evaluate(() => ({
      cards: document.querySelectorAll('.card').length,
      reasons: document.querySelectorAll('select')[1]?.options.length || 0,
      btn: /提交申请/.test(document.body.innerText)
    }))
    await step('UC-17', '售后申请页渲染（说明/下拉/提交）', asOk.cards >= 2 && asOk.reasons >= 3 && asOk.btn, JSON.stringify(asOk))
  } else {
    await step('UC-17', '售后申请页渲染（说明/下拉/提交）', false, '无订单数据')
  }

  // UC-18 售后记录列表
  await page.goto('http://localhost:10086/#/aftersales', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const asListOk = await page.evaluate(() => /售后/.test(document.body.innerText) && !/请登录|login/i.test(document.body.innerText))
  await step('UC-18', '售后记录列表页渲染', asListOk)

  // UC-20 真实提交售后申请（选原因 → 填描述 → 勾凭证 → 提交 → 跳转详情）
  if (asOrderNo) {
    await page.goto('http://localhost:10086/#/aftersale/new/' + asOrderNo, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await sleep(1800)
    await page.evaluate(() => {
      // 原因下拉：优先选非「质量问题」选项（凭证另行真实上传，双保险）
      const sel = document.querySelectorAll('select')[1]
      const opt = [...sel.options].find((o) => o.value && !/质量/.test(o.textContent)) || [...sel.options].find((o) => o.value)
      if (opt) {
        sel.value = opt.value
        sel.dispatchEvent(new Event('change', { bubbles: true }))
      }
      const ta = document.querySelector('textarea')
      if (ta) {
        Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value').set.call(ta, '包装完好未拆封，申请退货退款（UI E2E）')
        ta.dispatchEvent(new Event('input', { bubbles: true }))
      }
    })
    // 真实上传凭证：隐藏 file input 直设文件（tests/e2e/assets/sample.png），前端压缩为 dataURL
    const proofInput = await page.$('input[type=file]')
    await proofInput.uploadFile(path.join(ROOT, 'tests/e2e/assets/sample.png'))
    await sleep(1500) // 等压缩与缩略图渲染
    const proofShown = await page.evaluate(() => !!document.querySelector('.proof-img'))
    await sleep(600)
    await page.evaluate(() => {
      const btn = [...document.querySelectorAll('button')].find((b) => /提交申请/.test(b.innerText) && !b.disabled)
      if (btn) btn.click()
    })
    await sleep(2500)
    const onDetail = /aftersale\/AS/.test(page.url())
    await step('UC-20', 'H5 真实提交售后申请（含凭证压缩上传）并跳转详情', onDetail && proofShown, `proofThumb=${proofShown} url=${page.url()}`)

    // UC-21 售后详情页流转记录（买家提交 + 系统受理）+ 凭证 base64 回显闭环
    const flow = await page.evaluate(() => ({
      items: document.querySelectorAll('.timeline-item').length,
      submitted: /买家提交售后申请/.test(document.body.innerText),
      accepted: /系统受理/.test(document.body.innerText),
      // 凭证回显锚点：详情页缩略图 src 必须为 data:image（base64 存储→回显闭环）
      proofSrc: (document.querySelector('.proof-img') || {}).src || ''
    }))
    const proofIsData = flow.proofSrc.startsWith('data:image/')
    await step('UC-21', '售后详情流转记录+凭证回显（买家提交+系统受理+base64 闭环）', flow.items >= 2 && flow.submitted && flow.accepted && proofIsData, JSON.stringify({ ...flow, proofIsData }))
  } else {
    await step('UC-20', 'H5 真实提交售后申请并跳转详情', false, '无已完成订单（数据准备失败）')
    await step('UC-21', '售后详情流转记录（买家提交+系统受理）', false, '无已完成订单（数据准备失败）')
  }

  // UC-22 退货退款寄回物流填写（FD6 补录）：自造一笔新已完成订单（避开历史限次占用）→ 接口造 WAIT_RETURN 态工单 → H5 详情页填寄回物流推进到退款中
  try {
    const adminT22 = (await api('POST', '/admin-api/login', { username: 'admin', password: 'admin123' })).data?.token
    const bt22 = (await api('POST', '/api/member/login', { phone: '13900002222', smsCode: '123456' })).data?.token
    const pool22 = []
    for (const p of (await api('GET', '/api/products?page=1&size=20')).data?.rows || []) {
      const pd = await api('GET', `/api/products/${p.id}`)
      for (const s of pd.data?.skus || []) if (s.stock > 3 && s.price < 500000) pool22.push(s)
    }
    const sku22 = pool22.sort((a, b) => a.price - b.price)[0]
    let asNo22 = ''
    if (bt22 && sku22) {
      const addrId22 = ((await api('GET', '/api/addresses', undefined, bt22)).data || [])[0]?.id
      const o22 = await api('POST', '/api/orders', { idempotentKey: 'uias22-' + Date.now(), fulfillmentType: 'EXPRESS', addressId: addrId22, items: [{ skuId: sku22.id, quantity: 1 }] }, bt22)
      if (o22.code === 200) {
        await api('POST', `/api/orders/${o22.data.orderNo}/pay`, undefined, bt22)
        await api('POST', `/admin-api/orders/${o22.data.orderNo}/ship`, { company: '顺丰速运', no: 'SF22' + Date.now() }, adminT22)
        await api('POST', `/api/orders/${o22.data.orderNo}/confirm`, undefined, bt22)
        const created22 = await api('POST', '/api/aftersales', { orderNo: o22.data.orderNo, skuId: sku22.id, type: 'RETURN_REFUND', reason: '质量问题', description: '退货物流回填用', images: ['/img/uc22.jpg'] }, bt22)
        if (created22.code === 200) asNo22 = created22.data?.afterSaleNo
      }
    }
    if (asNo22) {
      await api('POST', `/admin-api/aftersales/${asNo22}/audit`, { approve: true, opinion: '同意退货，请寄回' }, adminT22)
      await page.evaluate((t) => localStorage.setItem('token', t), token)
      await page.goto(`http://localhost:10086/#/aftersale/${asNo22}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
      await sleep(1500)
      const formShown = await page.evaluate(() => [...document.querySelectorAll('.card-title')].some((el) => /填写寄回物流/.test(el.innerText)))
      let uc22Ok = formShown
      let d22 = `form=${formShown}`
      if (formShown) {
        const inputs = await page.$$('.card input')
        await inputs[0].type('顺丰速运')
        await inputs[1].type('SF' + Date.now())
        await page.evaluate(() => [...document.querySelectorAll('button')].find((b) => /提交寄回物流/.test(b.innerText))?.click())
        await sleep(2000)
        uc22Ok = await page.evaluate(() => {
          const st = (document.querySelector('.status') || {}).innerText || ''
          return /退款中/.test(st) && /买家已寄回商品/.test(document.body.innerText) && /寄回物流/.test(document.body.innerText)
        })
        d22 += ' status=退款中 flow=买家已寄回商品'
      }
      await step('UC-22', '退货退款待寄回态填写寄回物流推进到退款中（FD6 回归）', uc22Ok, d22)
    } else {
      await step('UC-22', '退货退款待寄回态填写寄回物流', false, '无可用的已完成订单/SKU（限次或无数据）')
    }
  } catch (e2) {
    await step('UC-22', '退货退款待寄回态填写寄回物流', false, e2.message)
  }

  // UC-23 商品详情主图多图轮播（H5-13）：横向滚动切换 + 圆点同步指示
  // UC-24 商品详情图区块（H5-13）：detailImages 铺满展示
  if (mediaProdId) {
    await page.goto('http://localhost:10086/#/product/' + mediaProdId, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await sleep(1800)
    const sw = await page.evaluate(() => ({
      imgs: document.querySelectorAll('.d-swiper img').length,
      dots: document.querySelectorAll('.d-dots i').length
    }))
    await page.evaluate(() => {
      const el = document.querySelector('.d-swiper')
      if (el) el.scrollLeft = el.clientWidth // 滑到第二张
    })
    await sleep(800)
    const cur = await page.evaluate(() => {
      const dots = [...document.querySelectorAll('.d-dots i')]
      return dots.findIndex((d) => d.classList.contains('on'))
    })
    await step('UC-23', '商品详情主图多图轮播（滚动切换+圆点同步）', sw.imgs === 2 && sw.dots === 2 && cur === 1, JSON.stringify({ ...sw, cur }))
    const di = await page.evaluate(() => ({
      title: [...document.querySelectorAll('.detail-imgs-title')].some((el) => /商品详情/.test(el.innerText)),
      n: document.querySelectorAll('.d-detail-imgs img').length
    }))
    await step('UC-24', '商品详情图区块渲染（detailImages 铺满展示）', di.title && di.n === 2, JSON.stringify(di))
  } else {
    await step('UC-23', '商品详情主图多图轮播', false, '多媒体用例商品创建失败')
    await step('UC-24', '商品详情图区块渲染', false, '多媒体用例商品创建失败')
  }

  // UC-25 订单详情「订单进度」状态标签间距（回归：曾与标题贴在一起）
  if (anyOrder) {
    await page.goto('http://localhost:10086/#/order/' + anyOrder.orderNo, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await sleep(1800)
    const tagGap = await page.evaluate(() => {
      const tag = document.querySelector('.card-title .tag')
      if (!tag) return { exists: false }
      return { exists: true, ml: getComputedStyle(tag).marginLeft, visible: tag.getBoundingClientRect().height > 0 }
    })
    await step('UC-25', '订单详情「订单进度」状态标签间距（6px 左间距不被压缩）', tagGap.exists && tagGap.ml === '6px' && tagGap.visible, JSON.stringify(tagGap))
  } else {
    await step('UC-25', '订单详情状态标签间距', false, '无订单数据')
  }

  // UC-19 未登录访问受限页跳登录（携 redirect）
  await page.evaluate(() => localStorage.removeItem('token'))
  await page.goto('http://localhost:10086/#/orders', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const redirected = page.url().includes('/login')
  await page.evaluate((t) => localStorage.setItem('token', t), token)
  await step('UC-19', '未登录访问订单页跳转登录页', redirected, page.url())
} catch (e) {
  await step('UC-ERR', '脚本执行异常', false, e.message)
}

await browser.close()

const passed = results.filter((r) => r.pass).length
console.log(`\n汇总: ${passed}/${results.length} 通过`)
fs.writeFileSync(path.join(ROOT, 'tests/e2e/ui-last-run.json'), JSON.stringify({ runAt: new Date().toISOString(), total: results.length, passed, results }, null, 2))
process.exit(passed === results.length ? 0 : 1)
