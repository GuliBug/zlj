import { defineStore } from 'pinia'
import { getToken, setToken, removeToken } from '@/utils/auth'

const USER_KEY = 'zl_admin_user'

function loadUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY)) || {}
  } catch {
    return {}
  }
}

export default defineStore('adminUser', {
  state: () => ({
    token: getToken() || '',
    username: loadUser().username || '',
    nickname: loadUser().nickname || ''
  }),
  actions: {
    setUser(token, username, nickname) {
      this.token = token
      this.username = username
      this.nickname = nickname
      setToken(token)
      localStorage.setItem(USER_KEY, JSON.stringify({ username, nickname }))
    },
    logout() {
      this.token = ''
      this.username = ''
      this.nickname = ''
      removeToken()
      localStorage.removeItem(USER_KEY)
    }
  }
})
