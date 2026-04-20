export const ENDPOINTS = {
  auth: {
    login: "/auth/login",
    refresh: "/auth/refresh",
    logout: "/auth/logout",
  },
  users: "/users",
  customers: "/customers",
  accounts: "/accounts",
  transactions: "/transactions",
  transfer: "/transactions/transfer",
  withdraw: "/transactions/withdraw",
  deposit: "/transactions/deposit",
  recurring: "/recurring-transactions",
  alerts: "/alerts",
  alertsOpen: "/alerts/open",
  reports: {
    excel: (accountId: string) => `/reports/transactions/${accountId}.xlsx`,
    pdf: (accountId: string) => `/reports/statement/${accountId}.pdf`,
    summary: "/reports/transactions/summary",
  },
  statistics: {
    accountsByProductType: "/statistics/accounts-by-product-type",
    accountsByBalanceTier: "/statistics/accounts-by-balance-tier",
    transactionsByBalanceTier: "/statistics/transactions-by-balance-tier",
    customersByLocation: "/statistics/customers-by-location",
  },
  health: "/actuator/health",
} as const;
