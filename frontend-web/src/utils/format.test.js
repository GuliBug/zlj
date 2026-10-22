import { describe, it, expect } from 'vitest'
import { fen2yuan, yuan2fen, unwrapPage } from './format'

describe('fen2yuan 金额分转元展示', () => {
  it('正常分转元', () => {
    expect(fen2yuan(1000)).toBe('¥10.00')
    expect(fen2yuan(1)).toBe('¥0.01')
    expect(fen2yuan(999999)).toBe('¥9999.99')
  })
  it('空值与非法值回退 ¥0.00', () => {
    expect(fen2yuan(null)).toBe('¥0.00')
    expect(fen2yuan(undefined)).toBe('¥0.00')
    expect(fen2yuan('')).toBe('¥0.00')
    expect(fen2yuan('abc')).toBe('¥0.00')
  })
  it('字符串数字可转换', () => {
    expect(fen2yuan('500')).toBe('¥5.00')
  })
})

describe('yuan2fen 元转分提交', () => {
  it('正常转换并四舍五入避免浮点误差', () => {
    expect(yuan2fen(10)).toBe(1000)
    expect(yuan2fen('8.5')).toBe(850)
    expect(yuan2fen(0.1 + 0.2)).toBe(30) // 0.30000000000000004 → 30
  })
})

describe('unwrapPage 分页响应兼容', () => {
  it('{total, rows} 结构', () => {
    const p = { rows: [{ id: 1 }], total: 5 }
    expect(unwrapPage(p)).toEqual({ rows: [{ id: 1 }], total: 5 })
  })
  it('rows 缺 total 时用 rows.length', () => {
    expect(unwrapPage({ rows: [1, 2, 3] }).total).toBe(3)
  })
  it('空/null 回退空页', () => {
    expect(unwrapPage(null)).toEqual({ rows: [], total: 0 })
    expect(unwrapPage({ content: [1], totalElements: 9 })).toEqual({ rows: [1], total: 9 })
  })
})
