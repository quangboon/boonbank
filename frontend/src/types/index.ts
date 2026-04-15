export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

// Auth
export interface AuthResult {
  token: string
  role: string
}

// Customer
export interface Customer {
  id: number
  name: string
  email: string
  phone: string
  address: string
  location: string
  customerTypeId: number
  customerTypeName: string
  createdAt: string
}

export interface CustomerRequest {
  name: string
  email: string
  phone: string
  address: string
  location: string
  customerTypeId: number
}

// Account
export type AccountStatus = 'ACTIVE' | 'LOCKED' | 'CLOSED'

export interface Account {
  id: number
  customerId: number
  customerName: string
  accountNumber: string
  balance: number
  transactionLimit: number
  status: AccountStatus
  openedAt: string
}

export interface AccountRequest {
  customerId: number
  transactionLimit?: number
}

export interface StatusHistory {
  id: number
  accountId: number
  oldStatus: string | null
  newStatus: string
  reason: string | null
  changedBy: string | null
  changedAt: string
}

// Transaction
export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER'

export interface Transaction {
  id: number
  fromAccountId: number | null
  toAccountId: number | null
  type: TransactionType
  amount: number
  fee: number
  location: string | null
  description: string | null
  createdAt: string
}

export interface TransactionRequest {
  type: TransactionType
  fromAccountId?: number
  toAccountId?: number
  toAccountNumber?: string
  amount: number
  location?: string
  description?: string
  idempotencyKey?: string
}

// Scheduled Transaction
export interface ScheduledTransaction {
  uuid: string
  accountId: number
  toAccountId: number | null
  type: TransactionType
  amount: number
  cronExpression: string
  description: string | null
  active: boolean
  nextRunAt: string | null
  lastRunAt: string | null
  createdAt: string
}

export interface ScheduledTransactionRequest {
  accountId: number
  toAccountId?: number
  type: TransactionType
  amount: number
  cronExpression: string
  description?: string
}

// Analytics
export interface TxnAnalytics {
  period: string
  txnCount: number
  avgAmount: number
  maxAmount: number
  minAmount: number
  totalFees: number
}

export type AnalyticsPeriod = 'WEEK' | 'MONTH' | 'YEAR'

// Statistics
export interface BalanceTier {
  tier: string
  accountCount: number
  transactionCount: number
}

export interface CustomersByLocation {
  location: string
  customerCount: number
}

// Fraud Alerts
export type FraudAlertStatus = 'PENDING' | 'REVIEWED' | 'DISMISSED'

export interface FraudAlert {
  id: number
  transactionId: number
  ruleName: string
  reason: string
  status: FraudAlertStatus
  reviewedBy: string | null
  reviewedAt: string | null
  createdAt: string
}

