import { describe, it, expect, beforeEach } from 'vitest'
import { yuan, uuid, getToken, setToken, isLoggedIn } from './api'

// node 环境无 localStorage，提供内存实现
const mem = new Map()
globalThis.localStorage = {
  getItem: (k) => (mem.has(k) ? mem.get(k) : null),
  setItem: (k, v) => mem.set(k, String(v)),
  removeItem: (k) => mem.delete(k)
}

describe('yuan 金额分转元', () => {
  it('正常转换保留两位', () => {
    expect(yuan(1000)).toBe('10.00')
    expect(yuan(1)).toBe('0.01')
    expect(yuan(0)).toBe('0.00')
  })
  it('空值回退 0.00', () => {
    expect(yuan(null)).toBe('0.00')
    expect(yuan(undefined)).toBe('0.00')
  })
})

describe('uuid 幂等键', () => {
  it('格式与唯一性', () => {
    const a = uuid()
    const b = uuid()
    expect(a).toMatch(/^[0-9a-z-]+$/)
    expect(a).not.toBe(b)
  })
})

describe('token 存取与登录态', () => {
  beforeEach(() => mem.clear())
  it('未设置时未登录', () => {
    expect(isLoggedIn()).toBe(false)
    expect(getToken()).toBe('')
  })
  it('设置后可读取，清空后登出', () => {
    setToken('MBR-test-token')
    expect(isLoggedIn()).toBe(true)
    expect(getToken()).toBe('MBR-test-token')
    setToken('')
    expect(isLoggedIn()).toBe(false)
  })
})
