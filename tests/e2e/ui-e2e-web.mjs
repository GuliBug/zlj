// Web 管理端 UI 级端到端验证（WC-01~19）
// 前置：后端 8080 + Web dev server 5173 已启动；账号 admin/admin123
// 断言：登录 → 7 个菜单页全部渲染关键内容 → 新增商品页违禁词实时拦截 → 编辑页/6 个操作弹窗/核销面板 → 全程无 JS 异常
import { createRequire } from 'node:module'
import { writeFileSync } from 'node:fs'
const require = createRequire(import.meta.url)
const puppeteer = require(new URL('../../frontend-web/node_modules/puppeteer', import.meta.url).pathname.replace(/^\/([A-Za-z]):/, '$1:'))

const results = []
function check(id, name, pass, detail = '') {
  results.push({ id, name, pass, detail })
  console.log(`${pass ? 'PASS' : 'FAIL'} ${id} ${name}${detail ? ' | ' + detail : ''}`)
}
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const BASE = 'http://127.0.0.1:5173'

const browser = await puppeteer.launch({
  headless: 'new',
  executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  args: ['--no-sandbox', '--disable-gpu']
})
const page = await browser.newPage()
await page.setViewport({ width: 1440, height: 900 })
const errors = []
page.on('pageerror', (e) => errors.push(e.message.slice(0, 120)))

// —— 接口级数据准备：制造 待发货订单 与 待审核售后工单，供 UI 交互用例使用 ——
const B = 'http://localhost:8080'
async function api(method, path, body, token) {
  const r = await fetch(B + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { 'X-Token': token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  return r.json()
}
let prepNote = ''
try {
  const adminTokenPrep = (await api('POST', '/admin-api/login', { username: 'admin', password: 'admin123' })).data?.token
  const phone = '137' + String(Date.now()).slice(-8)
  const bt = (await api('POST', '/api/member/login', { phone, smsCode: '123456' })).data?.token
  const prods = (await api('GET', '/api/products?page=1&size=20')).data?.rows || []
  const skuPool = []
  for (const p of prods) {
    const d = await api('GET', `/api/products/${p.id}`)
    for (const s of d.data?.skus || []) if (s.stock > 3 && s.price < 500000) skuPool.push(s)
  }
  const sku = skuPool.sort((a, b) => a.price - b.price)[0]
  if (bt && sku) {
    await api('POST', '/api/addresses', { receiver: '预备', phone, region: '湖南省湘潭市', detail: 'UI 预备地址 1 号' }, bt)
    const addrId = ((await api('GET', '/api/addresses', undefined, bt)).data || [])[0]?.id
    const mk = (key) =>
      api('POST', '/api/orders', { idempotentKey: 'uiweb-' + Date.now() + '-' + key, fulfillmentType: 'EXPRESS', addressId: addrId, items: [{ skuId: sku.id, quantity: 1 }] }, bt)
    const o1 = await mk('a')
    if (o1.code === 200) await api('POST', `/api/orders/${o1.data.orderNo}/pay`, undefined, bt) // → PAID：发货弹窗用例
    const o2 = await mk('b')
    if (o2.code === 200) {
      await api('POST', `/api/orders/${o2.data.orderNo}/pay`, undefined, bt)
      await api('POST', `/admin-api/orders/${o2.data.orderNo}/ship`, { company: '顺丰速运', no: 'SFUI' + Date.now() }, adminTokenPrep)
      await api('POST', `/api/orders/${o2.data.orderNo}/confirm`, undefined, bt) // → COMPLETED
      const as1 = await api('POST', '/api/aftersales', { orderNo: o2.data.orderNo, skuId: sku.id, type: 'REFUND_ONLY', reason: '质量问题', images: ['/img/ui.jpg'] }, bt)
      prepNote = as1.code === 200 ? 'ok' : '售后申请失败：' + as1.msg
    }
  } else {
    prepNote = '无可用 SKU 或登录失败，交互用例按无数据降级'
  }
} catch (e) {
  prepNote = '数据准备异常：' + e.message
}

async function open(path, tries = 2) {
  for (let i = 0; i < tries; i++) {
    try {
      await page.goto(BASE + path, { waitUntil: 'domcontentloaded', timeout: 60000 })
      await sleep(2500)
      return
    } catch (e) {
      if (i === tries - 1) throw e
      await sleep(2000) // Vite 按需编译可能卡顿，重试一次
    }
  }
}

// 页面冒烟：断言关键元素渲染
async function visitPage(id, name, path, mustContain) {
  await open(path)
  const found = await page.evaluate((sel) => {
    for (const s of sel) {
      if (document.querySelector(s)) return s
    }
    return null
  }, mustContain)
  check(id, name, !!found, found ? `命中 ${found}` : '未找到关键元素，url=' + page.url())
}

try {
  // WC-01 管理端登录（token 键 zl_admin_token；用户名默认预填 admin，仅输密码）
  await open('/login')
  await page.evaluate(() => {
    const u = [...document.querySelectorAll('.el-input__inner')].find((i) => (i.placeholder || '') === '账号')
    if (u) {
      u.value = ''
      u.dispatchEvent(new Event('input', { bubbles: true }))
    }
  })
  await page.type('input[placeholder="账号"]', 'admin', { delay: 20 })
  await page.type('input[placeholder="密码"]', 'admin123', { delay: 20 })
  await page.evaluate(() => {
    const btn = [...document.querySelectorAll('button')].find((b) => /登\s*录/.test(b.innerText))
    if (btn) btn.click()
  })
  await sleep(2500)
  const token = await page.evaluate(() => localStorage.getItem('zl_admin_token'))
  check('WC-01', '管理端页面真实登录', !!token, token ? String(token).slice(0, 10) : '无 token')

  // WC-02 工作台：指标卡 + 今日概览
  await visitPage('WC-02', '工作台渲染（指标卡/概览）', '/dashboard', ['.stat-card', '.el-card', '[class*="stat"]', '[class*="overview"]', '.el-table'])

  // WC-03 商品列表
  await visitPage('WC-03', '商品列表渲染（表格）', '/products', ['.el-table', 'table'])

  // WC-04 新增商品页（三步步骤条 + 违禁词实时校验）
  await open('/products/new')
  const hasSteps = await page.evaluate(() => !!document.querySelector('.el-steps, [class*="step"]'))
  const nameSel = await page.evaluate(() => {
    const inputs = [...document.querySelectorAll('.el-input__inner')]
    return inputs.map((i) => i.placeholder)
  })
  // 找到名称输入框（第一个文本输入）
  const nameHandle = await page.evaluateHandle(() => [...document.querySelectorAll('.el-input__inner')].find((i) => /名称/.test(i.placeholder || '')) || document.querySelector('.el-input__inner'))
  await nameHandle.asElement().type('提神醒脑神器', { delay: 30 })
  await sleep(1800) // oninput 防抖 + 接口返回
  const bannedShown = await page.evaluate(() => /提神|违禁|禁用|命中/.test(document.body.innerText))
  check('WC-04', '新增商品页违禁词实时拦截', hasSteps && bannedShown, `steps=${hasSteps} placeholders=${JSON.stringify(nameSel.slice(0, 3))} banned=${bannedShown}`)

  // WC-05 库存三页签
  await open('/stock')
  const tabs = await page.evaluate(() => [...document.querySelectorAll('.el-tabs__item')].map((t) => t.innerText.trim()))
  check('WC-05', '库存管理三页签（总览/流水/盘点）', tabs.length >= 3, tabs.join('/'))

  // WC-06 门店管理（含核销记录面板）
  await visitPage('WC-06', '门店管理渲染', '/stores', ['.el-table', 'table', '[class*="store"]'])

  // WC-07 订单管理
  await visitPage('WC-07', '订单管理渲染（表格+筛选）', '/orders', ['.el-table', 'table'])

  // WC-08 售后管理
  await visitPage('WC-08', '售后管理渲染', '/aftersales', ['.el-table', 'table'])

  // WC-09 会员管理
  await visitPage('WC-09', '会员管理渲染（等级卡+列表）', '/members', ['.el-table', '[class*="level"]', 'table'])

  // WC-10 审计与告警
  await visitPage('WC-10', '审计与告警页', '/audit-alerts', ['.el-tabs__item', '.el-table', 'table'])

  // 通用：打开列表页 → 点按钮 → 断言弹窗标题 → 关闭
  async function openDialog(id, name, path, btnText, titleRe, extraNote) {
    await open(path)
    const clicked = await page.evaluate((txt) => {
      const b = [...document.querySelectorAll('button')].find((x) => x.offsetParent !== null && x.innerText.trim() === txt)
      if (b) {
        b.click()
        return true
      }
      return false
    }, btnText)
    await sleep(1000)
    const title = await page.evaluate(() => {
      const d = [...document.querySelectorAll('.el-dialog')].find((x) => x.offsetParent !== null)
      return d ? d.querySelector('.el-dialog__title')?.innerText || '' : ''
    })
    check(id, name, clicked && titleRe.test(title), (title || '弹窗未打开') + (extraNote ? ' | ' + extraNote : ''))
    await page.evaluate(() => {
      const b = [...document.querySelectorAll('.el-dialog button')].find((x) => x.offsetParent !== null && /取消|关闭/.test(x.innerText.trim()))
      if (b) b.click()
    })
    await sleep(600)
  }

  // WC-11 商品编辑页（三步步骤条 + 表单回显）
  await open('/products')
  const pid = await page.evaluate(async () => {
    const t = localStorage.getItem('zl_admin_token')
    const r = await fetch('/admin-api/products?page=1&size=1', { headers: { 'X-Token': t } })
    return (await r.json()).data?.rows?.[0]?.id
  })
  await open(`/products/${pid}/edit`)
  const editOk = await page.evaluate(() => ({
    steps: !!document.querySelector('.el-steps,[class*="step"]'),
    inputs: document.querySelectorAll('.el-input__inner,.el-textarea__inner').length
  }))
  check('WC-11', '商品编辑页（步骤条+表单回显）', editOk.steps && editOk.inputs > 0, JSON.stringify(editOk))

  // WC-12 订单发货弹窗
  await openDialog('WC-12', '订单发货弹窗（仅待发货可发）', '/orders', '发货', /订单发货/, prepNote)

  // WC-13 售后审核弹窗
  await openDialog('WC-13', '售后审核弹窗（意见必填）', '/aftersales', '审核', /售后审核/, prepNote)

  // WC-14 会员积分调整弹窗
  await openDialog('WC-14', '会员积分调整弹窗', '/members', '调整积分', /调整积分/)

  // WC-15 等级规则设置弹窗
  await openDialog('WC-15', '等级规则设置弹窗（留审计）', '/members', '等级规则设置', /等级规则/)

  // WC-16 库存入库弹窗
  await openDialog('WC-16', '库存入库弹窗（批次号必填）', '/stock', '入库', /商品入库/)

  // WC-17 库存冻结弹窗
  await openDialog('WC-17', '库存冻结弹窗（填原因）', '/stock', '冻结', /冻结库存/)

  // WC-18 门店核销操作面板
  await open('/stores')
  const verifyPanel = await page.evaluate(
    () => /核销操作/.test(document.body.innerText) && [...document.querySelectorAll('button')].some((b) => b.innerText.trim() === '核销')
  )
  check('WC-18', '门店核销操作面板（订单号+码+核销按钮）', verifyPanel)

  // WC-19 商品列表操作入口（导出 CSV）
  await open('/products')
  const prodBtns = await page.evaluate(() => [...document.querySelectorAll('button')].map((b) => b.innerText.trim()))
  check('WC-19', '商品列表操作入口（导出）', prodBtns.includes('导出'))

  check('WC-ERR', '全程无页面 JS 异常', errors.length === 0, errors.slice(0, 3).join(' | ') || 'clean')
} catch (e) {
  check('WC-FATAL', '脚本异常中断', false, e.message.slice(0, 160))
}

await browser.close()
const passed = results.filter((r) => r.pass).length
writeFileSync(new URL('./ui-web-last-run.json', import.meta.url), JSON.stringify({ runAt: new Date().toISOString(), total: results.length, passed, results }, null, 2))
console.log(`汇总: ${passed}/${results.length} 通过`)
process.exit(passed === results.length ? 0 : 1)
