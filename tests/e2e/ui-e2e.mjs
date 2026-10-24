// UI 级端到端验证：走真实浏览器操作路径，断言关键操作元素「可见且可点击」（未被遮挡）
// 覆盖 UC-01~UC-19（核心交易链路 + 全部页面渲染 + 401 回跳）；前置：后端 8080 + H5 10086 已启动
import { createRequire } from 'node:module'
import path from 'node:path'
import fs from 'node:fs'
import { fileURLToPath } from 'node:url'
const require = createRequire(import.meta.url)
const puppeteer = require(new URL('../../frontend-web/node_modules/puppeteer', import.meta.url).pathname.replace(/^\/([A-Za-z]):/, '$1:'))
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const OUT_DIR = path.join(ROOT, 'docs/screenshots')
fs.mkdirSync(OUT_DIR, { recursive: true })

const results = []
function check(id, name, pass, detail = '') {
  results.push({ id, name, pass, detail })
  console.log(`${pass ? 'PASS' : 'FAIL'} ${id} ${name}${detail ? ' | ' + detail : ''}`)
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
  check('UC-01', 'H5 页面真实登录成功', !!token, token ? token.slice(0, 8) : 'localStorage 无 token')

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
  check('UC-02', '详情页加入购物车（购物车数量增加）', cart > 0, `totalQuantity=${cart}`)

  // UC-03 购物车结算按钮可见且未被遮挡（回归：曾固定 bottom:0 被 TabBar 盖住）
  await page.goto('http://localhost:10086/#/cart', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1800)
  const badge = await page.evaluate(() => document.querySelector('.tab-badge')?.innerText || '')
  check('UC-02B', '购物车页角标与数量同步', badge !== '' && Number(badge) === cart, `badge=${badge || '无'}`)
  const c1 = await visuallyClickable(page, '.bottom-bar .btn')
  check('UC-03', '购物车「结算」按钮可见且可点击', c1.ok, c1.why || '')

  // UC-04 点击结算 → 确认订单页可达，提交按钮可用
  if (c1.ok) {
    await page.evaluate(() => document.querySelector('.bottom-bar .btn').click())
    await sleep(2000)
    const onCheckout = page.url().includes('/checkout')
    const c2 = onCheckout ? await visuallyClickable(page, '.bottom-bar .btn') : { ok: false, why: '未跳转到确认订单页' }
    check('UC-04', '确认订单页「提交订单」可见且可点击', c2.ok, c2.why || `url=${page.url()}`)

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
      check('UC-05', '提交订单→立即支付→跳转支付结果', paid && onPayResult, `url=${page.url()}`)
    }
  }

  // UC-06 商品详情吸底栏未被遮挡
  await page.goto('http://localhost:10086/#/product/1', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const c3 = await visuallyClickable(page, '.bottom-bar .btn')
  check('UC-06', '商品详情吸底操作栏可见且可点击', c3.ok, c3.why || '')

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
  check('UC-07', '首页渲染（警示条/分档 chips/商品网格）', home.chips && home.hero && home.warn && home.goods, JSON.stringify(home))

  // UC-08 全部商品页
  await page.goto('http://localhost:10086/#/category', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1800)
  const catOk = await page.evaluate(() => !!document.querySelector('.has-tabbar') && /¥/.test(document.body.innerText))
  check('UC-08', '全部商品页按分类分节渲染', catOk)

  // UC-09 门店信息页（单店模式）
  await page.goto('http://localhost:10086/#/stores', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const storeOk = await page.evaluate(() => !!document.querySelector('.store-card') && /正隆记/.test(document.body.innerText))
  check('UC-09', '门店信息页渲染（正隆记总号）', storeOk)

  // UC-10 会员中心（掌柜卡/四格统计/退出）
  await page.goto('http://localhost:10086/#/member', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1800)
  const memberOk = await page.evaluate(() => !!document.querySelector('.has-tabbar') && /积分/.test(document.body.innerText) && /退出/.test(document.body.innerText))
  check('UC-10', '会员中心渲染（积分/退出入口）', memberOk)

  // UC-11 收货地址页
  await page.goto('http://localhost:10086/#/addresses', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const addrOk = await page.evaluate(() => /新增收货地址/.test(document.body.innerText))
  check('UC-11', '收货地址页渲染（新增入口）', addrOk)

  // UC-12 订单列表 6 状态 Tab
  await page.goto('http://localhost:10086/#/orders', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1800)
  const tabCount = await page.evaluate(() => document.querySelectorAll('.otab').length)
  check('UC-12', '订单列表 6 状态 Tab 渲染', tabCount === 6, `tabs=${tabCount}`)

  // UC-13 订单详情（时间轴 + 吸底操作）
  if (anyOrder) {
    await page.goto('http://localhost:10086/#/order/' + anyOrder.orderNo, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await sleep(1800)
    const odOk = await page.evaluate(() => !!document.querySelector('.has-bottom-bar') && /¥/.test(document.body.innerText))
    check('UC-13', '订单详情渲染（金额明细+吸底操作栏）', odOk)
  } else {
    check('UC-13', '订单详情渲染（金额明细+吸底操作栏）', false, '无订单数据')
  }

  // UC-14 核销码页（无 orderId 引导态）
  await page.goto('http://localhost:10086/#/verify-code', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const vcGuide = await page.evaluate(() => !!document.querySelector('.hq-card') && /— — — —/.test(document.body.innerText))
  check('UC-14', '核销码页引导态（当前门店+提示从订单进入）', vcGuide)

  // UC-15 核销码页（动态码态）
  if (pickupOrder) {
    await page.goto('http://localhost:10086/#/verify-code?orderId=' + pickupOrder.orderNo, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await sleep(2000)
    const vcDyn = await page.evaluate(() => (document.querySelector('.hq-card .code')?.innerText || '').replace(/\s/g, ''))
    check('UC-15', '核销码页动态码展示（6 位）', /^\d{6}$/.test(vcDyn), `code=${vcDyn}`)
  } else {
    check('UC-15', '核销码页动态码展示（6 位）', false, '无待自提订单')
  }

  // UC-16 支付结果页
  if (pickupOrder) {
    await page.goto('http://localhost:10086/#/pay-result/' + pickupOrder.orderNo, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await sleep(1800)
    const prOk = await page.evaluate(() => !!document.querySelector('.result .ok') && /下单成功/.test(document.body.innerText))
    check('UC-16', '支付结果页渲染（下单成功引导）', prOk)
  } else {
    check('UC-16', '支付结果页渲染（下单成功引导）', false, '无已支付订单')
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
    check('UC-17', '售后申请页渲染（说明/下拉/提交）', asOk.cards >= 2 && asOk.reasons >= 3 && asOk.btn, JSON.stringify(asOk))
  } else {
    check('UC-17', '售后申请页渲染（说明/下拉/提交）', false, '无订单数据')
  }

  // UC-18 售后记录列表
  await page.goto('http://localhost:10086/#/aftersales', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const asListOk = await page.evaluate(() => /售后/.test(document.body.innerText) && !/请登录|login/i.test(document.body.innerText))
  check('UC-18', '售后记录列表页渲染', asListOk)

  // UC-19 未登录访问受限页跳登录（携 redirect）
  await page.evaluate(() => localStorage.removeItem('token'))
  await page.goto('http://localhost:10086/#/orders', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await sleep(1500)
  const redirected = page.url().includes('/login')
  await page.evaluate((t) => localStorage.setItem('token', t), token)
  check('UC-19', '未登录访问订单页跳转登录页', redirected, page.url())
} catch (e) {
  check('UC-ERR', '脚本执行异常', false, e.message)
}

await page.screenshot({ path: path.join(OUT_DIR, 'h5-cart-fixed.png') })
await browser.close()

const passed = results.filter((r) => r.pass).length
console.log(`\n汇总: ${passed}/${results.length} 通过`)
fs.writeFileSync(path.join(ROOT, 'tests/e2e/ui-last-run.json'), JSON.stringify({ runAt: new Date().toISOString(), total: results.length, passed, results }, null, 2))
process.exit(passed === results.length ? 0 : 1)
