import { api } from '@/lib/api-client'
import type { Customer, CustomerRequest, Page } from '@/types'

export const getCustomers = (page = 0) =>
  api<Page<Customer>>(`/api/v1/customers?page=${page}`)

export const createCustomer = (data: CustomerRequest) =>
  api<Customer>('/api/v1/customers', { method: 'POST', body: JSON.stringify(data) })

export const updateCustomer = (id: number, data: CustomerRequest) =>
  api<Customer>(`/api/v1/customers/${id}`, { method: 'PUT', body: JSON.stringify(data) })

export const deleteCustomer = (id: number) =>
  api<void>(`/api/v1/customers/${id}`, { method: 'DELETE' })
