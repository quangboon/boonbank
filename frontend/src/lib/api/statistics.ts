import { api } from '@/lib/api-client'
import type { BalanceTier, CustomersByLocation } from '@/types'

export const getBalanceTiers = () =>
  api<BalanceTier[]>('/api/v1/statistics/balance-tiers')

export const getCustomersByLocation = () =>
  api<CustomersByLocation[]>('/api/v1/statistics/customers-by-location')
