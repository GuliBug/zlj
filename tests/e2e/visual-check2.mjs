import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const puppeteer = require(new URL('../../frontend-web/node_modules/puppeteer', import.meta.url).pathname.replace(/^\/([A-Za-z]):/, '$1:'))

const shots = 'D:/projects/zhenglongji-mall/docs/screenshots'
const browser = await puppeteer.launch({
  headless: 'new',
  executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  args: ['--no-sandbox', '--disable-gpu']
})

const h5 = await browser.newPage()
await h5.setViewport({ width: 390, height: 844, isMobile: true })
await h5.goto('http://localhost:10086/', { waitUntil: 'domcontentloaded', timeout: 60000 })
await new Promise((r) => setTimeout(r, 2000))
await h5.screenshot({ path: shots + '/h5-home.png' })

// 详情页 + SKU 弹层
await h5.evaluate(() => {
  const card = [...document.querySelectorAll('.p-card, .product-card, [class*="card"]')].find((c) => /三号究|六号究|头道究/.test(c.innerText))
  if (card) card.click()
})
await new Promise((r) => setTimeout(r, 1800))
await h5.screenshot({ path: shots + '/h5-product.png' })

// Web 商品管理页（通过侧边栏点击跳转，避免 hash goto 卡住）
const web = await browser.newPage()
await web.setViewport({ width: 1440, height: 900 })
await web.goto('http://localhost:5173/#/login', { waitUntil: 'domcontentloaded', timeout: 60000 })
await new Promise((r) => setTimeout(r, 1000))
await web.evaluate(() => {
  const inputs = [...document.querySelectorAll('input')]
  const set = (el, v) => {
    const setter = Object.getOwnPropertyDescriptor(el.constructor.prototype, 'value').set
    setter.call(el, v)
    el.dispatchEvent(new Event('input', { bubbles: true }))
  }
  set(inputs[0], 'admin')
  set(inputs[1], 'admin123')
  const btn = [...document.querySelectorAll('button')].find((b) => /登录|登 录/.test(b.innerText))
  btn && btn.click()
})
await new Promise((r) => setTimeout(r, 2500))
// 登录后首先落在工作台
await web.screenshot({ path: shots + '/web-dashboard.png' })
const menuClicked = await web.evaluate(() => {
  const item = [...document.querySelectorAll('li, .el-menu-item, [class*="menu"] *')].find((m) => (m.innerText || '').trim() === '商品列表')
  if (item) {
    item.click()
    return true
  }
  return false
})
await new Promise((r) => setTimeout(r, 2000))
console.log('menuClicked:', menuClicked, 'url:', web.url())
await web.screenshot({ path: shots + '/web-products.png' })

await browser.close()
console.log('done')
