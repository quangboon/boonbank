export type AccountStatus = "ACTIVE" | "INACTIVE" | "FROZEN" | "CLOSED";
export type AccountType = "SAVINGS" | "CHECKING" | "LOAN" | "CREDIT";
export type TransactionType = "TRANSFER" | "WITHDRAW" | "DEPOSIT";
export type TransactionStatus = "PENDING" | "COMPLETED" | "FAILED" | "REVERSED";
export type AlertSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type PeriodUnit = "WEEK" | "QUARTER" | "YEAR";

export type Customer = {
  id: string;
  customerCode: string;
  fullName: string;
  idNumber: string;
  email: string;
  phone: string;
  address?: string;
  location?: string;
  dateOfBirth?: string;
  customerTypeCode?: string;
  createdAt: string;
};

export type Account = {
  id: string;
  accountNumber: string;
  customerId: string;
  accountType: AccountType;
  status: AccountStatus;
  balance: string;
  transactionLimit: string;
  currency: string;
  openedAt: string;
};

export type AccountBalance = {
  accountNumber: string;
  balance: string;
  currency: string;
  asOf: string;
};

export type AccountStatusHistory = {
  fromStatus: AccountStatus | null;
  toStatus: AccountStatus;
  reason: string;
  createdBy: string;
  createdAt: string;
};

export type Transaction = {
  id: string;
  type: TransactionType;
  status: TransactionStatus;
  sourceAccountNumber?: string;
  destinationAccountNumber?: string;
  amount: string;
  fee: string;
  currency: string;
  description?: string;
  location?: string;
  createdAt: string;
};

export type Alert = {
  id: string;
  transactionId?: string | null;
  ruleCode: string;
  severity: AlertSeverity;
  message: string;
  resolved: boolean;
  createdAt: string;
};

export type UserRole = "ADMIN" | "CUSTOMER" | "TELLER" | "OPS" | "FRAUD";

export type User = {
  id: string;
  username: string;
  enabled: boolean;
  accountLocked: boolean;
  lastLoginAt?: string | null;
  customerId?: string | null;
  roles: UserRole[];
  createdAt: string;
};

export type BalanceTierStats = { tier: "HIGH" | "MID" | "LOW"; count: number };
export type AccountTierStats = { accountType: AccountType; count: number };
export type LocationStats = { city: string; customerCount: number };
export type TransactionPeriodStats = {
  bucket: string;
  minAmount: string;
  maxAmount: string;
  avgAmount: string;
  sumAmount: string;
  count: number;
};
