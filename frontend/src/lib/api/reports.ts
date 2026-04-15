import { apiRaw } from '@/lib/api-client'

export const downloadExcel = (from: string, to: string) =>
  apiRaw(`/api/v1/reports/transactions/excel?from=${from}&to=${to}`)

export const downloadPdf = (from: string, to: string) =>
  apiRaw(`/api/v1/reports/transactions/pdf?from=${from}&to=${to}`)
