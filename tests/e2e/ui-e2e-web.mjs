// Web 管理端 UI 级端到端验证（WC-01~11）
// 前置：后端 8080 + Web dev server 5173 已启动；账号 admin/admin123
// 断言：登录 → 7 个菜单页全部渲染关键内容 → 新增商品页违禁词实时拦截 → 全程无 JS 异常
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

  check('WC-ERR', '全程无页面 JS 异常', errors.length === 0, errors.slice(0, 3).join(' | ') || 'clean')
} catch (e) {
  check('WC-FATAL', '脚本异常中断', false, e.message.slice(0, 160))
}

await browser.close()
const passed = results.filter((r) => r.pass).length
writeFileSync(new URL('./ui-web-last-run.json', import.meta.url), JSON.stringify({ runAt: new Date().toISOString(), total: results.length, passed, results }, null, 2))
console.log(`汇总: ${passed}/${results.length} 通过`)
process.exit(passed === results.length ? 0 : 1)
