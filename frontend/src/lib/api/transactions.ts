import { api } from '@/lib/api-client'
import type { Page, Transaction, TransactionRequest, TransactionType } from '@/types'

export const getTransactions = (page = 0) =>
  api<Page<Transaction>>(`/api/v1/transactions?page=${page}`)

export const execute = (data: TransactionRequest) =>
  api<Transaction>('/api/v1/transactions', { method: 'POST', body: JSON.stringify(data) })

export interface TxnSearchParams {
  type?: TransactionType
  amountMin?: number
  amountMax?: number
  from?: string
  to?: string
  page?: number
}

export const searchTransactions = (params: TxnSearchParams) => {
  const qs = new URLSearchParams()
  if (params.type) qs.set('type', params.type)
  if (params.amountMin != null) qs.set('amountMin', String(params.amountMin))
  if (params.amountMax != null) qs.set('amountMax', String(params.amountMax))
  if (params.from) qs.set('from', params.from)
  if (params.to) qs.set('to', params.to)
  qs.set('page', String(params.page ?? 0))
  return api<Page<Transaction>>(`/api/v1/transactions/search?${qs}`)
}
