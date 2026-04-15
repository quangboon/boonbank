import { api } from '@/lib/api-client'
import type { AnalyticsPeriod, TxnAnalytics } from '@/types'

export const getTxnStats = (period: AnalyticsPeriod, from?: string, to?: string) => {
  const params = new URLSearchParams({ period })
  if (from) params.set('from', from)
  if (to) params.set('to', to)
  return api<TxnAnalytics[]>(`/api/v1/analytics/transactions?${params}`)
}
