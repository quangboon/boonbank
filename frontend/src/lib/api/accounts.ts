import { api, unwrap } from "./client";
import { ENDPOINTS } from "./endpoints";
import type { PageResponse } from "./types";
import type {
  Account,
  AccountBalance,
  AccountStatus,
  AccountStatusHistory,
  AccountType,
} from "@/types/domain";

export type AccountLookup = {
  accountNumber: string;
  holderName: string;
  currency: string;
  status: AccountStatus;
};

export type AccountCreateReq = {
  customerId: string;
  accountType: AccountType;
  currency: string;
};

export type AccountUpdateReq = {
  accountType?: AccountType;
  transactionLimit?: string;
};

export type AccountSearchParams = {
  customerId?: string;
  accountType?: AccountType;
  status?: AccountStatus;
  currency?: string;
  minBalance?: string;
  maxBalance?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export const accountsApi = {
  list: (params: AccountSearchParams) =>
    unwrap<PageResponse<Account>>(api.get(ENDPOINTS.accounts, { params })),
  get: (id: string) => unwrap<Account>(api.get(`${ENDPOINTS.accounts}/${id}`)),
  balance: (accountNumber: string) =>
    unwrap<AccountBalance>(
      api.get(`${ENDPOINTS.accounts}/${accountNumber}/balance`),
    ),
  lookup: (accountNumber: string) =>
    unwrap<AccountLookup>(
      api.get(`${ENDPOINTS.accounts}/lookup`, { params: { accountNumber } }),
    ),
  create: (req: AccountCreateReq) =>
    unwrap<Account>(api.post(ENDPOINTS.accounts, req)),
  update: (id: string, req: AccountUpdateReq) =>
    unwrap<Account>(api.put(`${ENDPOINTS.accounts}/${id}`, req)),
  freeze: (id: string, reason: string) =>
    unwrap<Account>(
      api.post(`${ENDPOINTS.accounts}/${id}/freeze`, null, {
        params: { reason },
      }),
    ),
  unfreeze: (id: string, reason: string) =>
    unwrap<Account>(
      api.post(`${ENDPOINTS.accounts}/${id}/unfreeze`, null, {
        params: { reason },
      }),
    ),
  close: (id: string, reason: string) =>
    unwrap<Account>(
      api.post(`${ENDPOINTS.accounts}/${id}/close`, null, {
        params: { reason },
      }),
    ),
  statusHistory: (id: string) =>
    unwrap<AccountStatusHistory[]>(
      api.get(`${ENDPOINTS.accounts}/${id}/status-history`),
    ),
  delete: (id: string) => api.delete(`${ENDPOINTS.accounts}/${id}`),
};
