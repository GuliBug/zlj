import { createRequire } from 'node:module'
const require = createRequire(import.meta.url)
const puppeteer = require('D:/projects/qingguoji-mall-main/frontend-web/node_modules/puppeteer')

const shots = 'D:/projects/zhenglongji-mall/docs/screenshots'
const browser = await puppeteer.launch({
  headless: 'new',
  executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  args: ['--no-sandbox', '--disable-gpu']
})
const errors = []

async function fill(page, values) {
  await page.evaluate((vals) => {
    const inputs = [...document.querySelectorAll('input')]
    vals.forEach((v, i) => {
      const el = inputs[i]
      if (!el) return
      const setter = Object.getOwnPropertyDescriptor(el.constructor.prototype, 'value').set
      setter.call(el, v)
      el.dispatchEvent(new Event('input', { bubbles: true }))
    })
    const btn = [...document.querySelectorAll('button, .btn, [class*="btn"]')].find((b) => /登录|登 录/.test(b.innerText))
    if (btn) btn.click()
  }, values)
}

const h5 = await browser.newPage()
await h5.setViewport({ width: 390, height: 844, isMobile: true })
h5.on('pageerror', (e) => errors.push('H5 ' + e.message))
await h5.goto('http://localhost:10086/', { waitUntil: 'domcontentloaded', timeout: 60000 })
await new Promise((r) => setTimeout(r, 1500))
await h5.screenshot({ path: shots + '/h5-home.png' })
console.log('H5 home text:', (await h5.evaluate(() => document.body.innerText)).slice(0, 150).replace(/\n/g, ' | '))

await h5.goto('http://localhost:10086/#/login', { waitUntil: 'domcontentloaded', timeout: 60000 })
await new Promise((r) => setTimeout(r, 800))
await fill(h5, ['13900001234', '123456'])
await new Promise((r) => setTimeout(r, 2500))
await h5.screenshot({ path: shots + '/h5-after-login.png' })
console.log('H5 url after login:', h5.url())

const web = await browser.newPage()
await web.setViewport({ width: 1440, height: 900 })
web.on('pageerror', (e) => errors.push('WEB ' + e.message))
await web.goto('http://127.0.0.1:5173/#/login', { waitUntil: 'domcontentloaded', timeout: 60000 })
await new Promise((r) => setTimeout(r, 800))
await fill(web, ['admin', 'admin123'])
await new Promise((r) => setTimeout(r, 3000))
await web.screenshot({ path: shots + '/web-dashboard.png' })
console.log('WEB url:', web.url())
console.log('WEB text:', (await web.evaluate(() => document.body.innerText)).slice(0, 200).replace(/\n/g, ' | '))
await web.goto('http://127.0.0.1:5173/#/products', { waitUntil: 'domcontentloaded', timeout: 60000 })
await new Promise((r) => setTimeout(r, 2000))
await web.screenshot({ path: shots + '/web-products.png' })

console.log('page errors:', errors.length ? errors : 'none')
await browser.close()
