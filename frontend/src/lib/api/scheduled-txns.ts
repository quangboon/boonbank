import { api } from '@/lib/api-client'
import type { Page, ScheduledTransaction, ScheduledTransactionRequest } from '@/types'

export const getScheduled = (page = 0) =>
  api<Page<ScheduledTransaction>>(`/api/v1/scheduled-transactions?page=${page}`)

export const createScheduled = (data: ScheduledTransactionRequest) =>
  api<ScheduledTransaction>('/api/v1/scheduled-transactions', {
    method: 'POST',
    body: JSON.stringify(data),
  })

export const toggleActive = (uuid: string, active: boolean) =>
  api<ScheduledTransaction>(`/api/v1/scheduled-transactions/${uuid}/active?active=${active}`, {
    method: 'PUT',
  })

export const deleteScheduled = (uuid: string) =>
  api<void>(`/api/v1/scheduled-transactions/${uuid}`, { method: 'DELETE' })
