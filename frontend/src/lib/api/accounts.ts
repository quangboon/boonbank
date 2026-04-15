import { api } from '@/lib/api-client'
import type { Account, AccountRequest, AccountStatus, Page, StatusHistory } from '@/types'

export const getAccounts = (page = 0) =>
  api<Page<Account>>(`/api/v1/accounts?page=${page}`)

export const getMyAccounts = () =>
  api<Page<Account>>('/api/v1/accounts?page=0&size=100')

export const createAccount = (data: AccountRequest) =>
  api<Account>('/api/v1/accounts', { method: 'POST', body: JSON.stringify(data) })

export const changeStatus = (id: number, status: AccountStatus, reason: string, changedBy: string) =>
  api<Account>(`/api/v1/accounts/${id}/status?status=${status}&reason=${encodeURIComponent(reason)}&changedBy=${encodeURIComponent(changedBy)}`, {
    method: 'PUT',
  })

export const lookupAccount = (accountNumber: string) =>
  api<Account>(`/api/v1/accounts/lookup?accountNumber=${accountNumber}`)

export const getStatusHistory = (id: number) =>
  api<Page<StatusHistory>>(`/api/v1/accounts/${id}/status-history?size=50&sort=changedAt,desc`)
