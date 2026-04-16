import { api } from '@/lib/api-client'
import type { Customer, CustomerRequest, Page } from '@/types'

export const getCustomers = (page = 0) =>
  api<Page<Customer>>(`/api/v1/customers?page=${page}`)

export interface CustomerSearchParams {
  name?: string
  email?: string
  phone?: string
  location?: string
  page?: number
}

export const searchCustomers = (params: CustomerSearchParams) => {
  const q = new URLSearchParams()
  if (params.name) q.set('name', params.name)
  if (params.email) q.set('email', params.email)
  if (params.phone) q.set('phone', params.phone)
  if (params.location) q.set('location', params.location)
  q.set('page', String(params.page ?? 0))
  return api<Page<Customer>>(`/api/v1/customers/search?${q.toString()}`)
}

export const createCustomer = (data: CustomerRequest) =>
  api<Customer>('/api/v1/customers', { method: 'POST', body: JSON.stringify(data) })

export const updateCustomer = (id: number, data: CustomerRequest) =>
  api<Customer>(`/api/v1/customers/${id}`, { method: 'PUT', body: JSON.stringify(data) })

export const deleteCustomer = (id: number) =>
  api<void>(`/api/v1/customers/${id}`, { method: 'DELETE' })
