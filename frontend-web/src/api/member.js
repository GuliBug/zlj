import request from '@/utils/request'
import { unwrapPage } from '@/utils/format'

// AC-15 会员
export function listMembers(query) {
  return request({ url: '/members', method: 'get', params: query }).then((res) => unwrapPage(res.data))
}

export function adjustPoints(id, data) {
  return request({ url: '/members/' + id + '/points', method: 'post', data })
}

export function getLevelRules() {
  return request({ url: '/level-rules', method: 'get' })
}

export function updateLevelRules(data) {
  return request({ url: '/level-rules', method: 'put', data })
}
