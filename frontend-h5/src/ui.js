/** 轻量 Toast / Confirm / Modal（无 UI 库，手写实现） */

let toastTimer = null
export function showToast(msg, duration = 2000) {
  let el = document.getElementById('zl-toast')
  if (!el) {
    el = document.createElement('div')
    el.id = 'zl-toast'
    el.style.cssText = 'position:fixed;left:50%;bottom:120px;transform:translateX(-50%);' +
      'max-width:80%;background:rgba(30,15,8,.82);color:#FFF6E3;padding:10px 18px;border-radius:8px;' +
      'font-size:13px;line-height:1.5;z-index:9999;text-align:center;pointer-events:none;' +
      'transition:opacity .25s;opacity:0;'
    document.body.appendChild(el)
  }
  el.textContent = msg
  el.style.opacity = '1'
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { el.style.opacity = '0' }, duration)
}

/** 二次确认，返回 Promise<boolean> */
export function showConfirm(title, content = '') {
  return new Promise(resolve => {
    const mask = document.createElement('div')
    mask.style.cssText = 'position:fixed;inset:0;background:rgba(40,20,10,.55);z-index:9998;' +
      'display:flex;align-items:center;justify-content:center;'
    const box = document.createElement('div')
    box.style.cssText = 'width:78%;max-width:320px;background:#FFFDF8;border-radius:12px;padding:22px 18px 16px;text-align:center;'
    box.innerHTML =
      `<div style="font-size:16px;font-weight:600;color:#3B2B20">${title}</div>` +
      (content ? `<div style="font-size:13px;color:#9A8B7A;margin-top:8px;line-height:1.6">${content}</div>` : '') +
      `<div style="display:flex;gap:10px;margin-top:18px">
         <button data-a="no" style="flex:1;height:38px;border-radius:19px;border:1px solid #C9BCA6;background:#fff;color:#6B5B48;font-size:14px">取消</button>
         <button data-a="yes" style="flex:1;height:38px;border-radius:19px;border:none;background:linear-gradient(135deg,#B01F24,#8E181C);color:#FFF6E3;font-size:14px">确定</button>
       </div>`
    mask.appendChild(box)
    document.body.appendChild(mask)
    mask.addEventListener('click', e => {
      const a = e.target.dataset && e.target.dataset.a
      if (a === 'yes') { mask.remove(); resolve(true) }
      else if (a === 'no' || e.target === mask) { mask.remove(); resolve(false) }
    })
  })
}

/** 错误弹窗（如库存不足），仅一个确定按钮 */
export function showAlert(title, content = '') {
  return new Promise(resolve => {
    const mask = document.createElement('div')
    mask.style.cssText = 'position:fixed;inset:0;background:rgba(40,20,10,.55);z-index:9998;' +
      'display:flex;align-items:center;justify-content:center;'
    const box = document.createElement('div')
    box.style.cssText = 'width:78%;max-width:320px;background:#FFFDF8;border-radius:12px;padding:22px 18px 16px;text-align:center;'
    box.innerHTML =
      `<div style="font-size:16px;font-weight:600;color:#B01F24">${title}</div>` +
      `<div style="font-size:13px;color:#6B5B48;margin-top:8px;line-height:1.6;white-space:pre-wrap">${content}</div>` +
      `<button style="width:100%;height:38px;border-radius:19px;border:none;background:linear-gradient(135deg,#B01F24,#8E181C);color:#FFF6E3;font-size:14px;margin-top:18px">我知道了</button>`
    mask.appendChild(box)
    document.body.appendChild(mask)
    mask.addEventListener('click', e => {
      if (e.target === box.querySelector('button') || e.target === mask) { mask.remove(); resolve() }
    })
  })
}
