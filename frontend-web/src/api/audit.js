import request from '@/utils/request'
import { unwrapPage } from '@/utils/format'

// AC-16 审计与告警
export function listAuditLogs(query) {
  return request({ url: '/audit-logs', method: 'get', params: query }).then((res) => unwrapPage(res.data))
}

export function listAlerts(query) {
  return request({ url: '/alerts', method: 'get', params: query }).then((res) => unwrapPage(res.data))
}
