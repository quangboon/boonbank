import { api } from '@/lib/api-client'
import type { FraudAlert, FraudAlertStatus, Page } from '@/types'

export const getAlerts = (status?: FraudAlertStatus, page = 0) => {
  const params = new URLSearchParams({ page: String(page) })
  if (status) params.set('status', status)
  return api<Page<FraudAlert>>(`/api/v1/fraud-alerts?${params}`)
}

export const reviewAlert = (id: number, status: 'REVIEWED' | 'DISMISSED', reviewedBy: string) =>
  api<FraudAlert>(`/api/v1/fraud-alerts/${id}/review`, {
    method: 'PUT',
    body: JSON.stringify({ status, reviewedBy }),
  })
