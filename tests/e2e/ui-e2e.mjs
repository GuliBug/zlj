// UI 级端到端验证：走真实浏览器操作路径，断言关键操作元素「可见且可点击」（未被遮挡）
// 覆盖 UC-01~UC-05；前置：后端 8080 + H5 10086 已启动
import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const puppeteer = require(new URL('../../frontend-web/node_modules/puppeteer', import.meta.url).pathname.replace(/^\/([A-Za-z]):/, '$1:'))

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
} catch (e) {
  check('UC-ERR', '脚本执行异常', false, e.message)
}

await page.screenshot({ path: 'D:/projects/zhenglongji-mall/docs/screenshots/h5-cart-fixed.png' })
await browser.close()

const passed = results.filter((r) => r.pass).length
console.log(`\n汇总: ${passed}/${results.length} 通过`)
const fs = await import('node:fs')
fs.writeFileSync('D:/projects/zhenglongji-mall/tests/e2e/ui-last-run.json', JSON.stringify({ runAt: new Date().toISOString(), total: results.length, passed, results }, null, 2))
process.exit(passed === results.length ? 0 : 1)
