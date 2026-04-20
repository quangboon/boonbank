import { api, unwrap } from "./client";
import { ENDPOINTS } from "./endpoints";
import type { PageResponse } from "./types";
import type {
  Transaction,
  TransactionStatus,
  TransactionType,
} from "@/types/domain";

export type TransferReq = {
  sourceAccountNumber: string;
  destinationAccountNumber: string;
  amount: string;
  currency: string;
  location?: string;
  description?: string;
};

export type CashReq = {
  accountNumber: string;
  amount: string;
  location?: string;
  description?: string;
};

export type TransactionSearchParams = {
  accountId?: string;
  type?: TransactionType;
  status?: TransactionStatus;
  minAmount?: string;
  maxAmount?: string;
  from?: string;
  to?: string;
  location?: string;
  page?: number;
  size?: number;
  sort?: string;
};

const IDEMPOTENCY_HEADER = "Idempotency-Key";

function idempotentHeader(key: string) {
  return { [IDEMPOTENCY_HEADER]: key };
}

export const transactionsApi = {
  search: (params: TransactionSearchParams) =>
    unwrap<PageResponse<Transaction>>(
      api.get(ENDPOINTS.transactions, { params }),
    ),
  transfer: (req: TransferReq, idempotencyKey: string) =>
    unwrap<Transaction>(
      api.post(ENDPOINTS.transfer, req, {
        headers: idempotentHeader(idempotencyKey),
      }),
    ),
  withdraw: (req: CashReq, idempotencyKey: string) =>
    unwrap<Transaction>(
      api.post(ENDPOINTS.withdraw, req, {
        headers: idempotentHeader(idempotencyKey),
      }),
    ),
  deposit: (req: CashReq, idempotencyKey: string) =>
    unwrap<Transaction>(
      api.post(ENDPOINTS.deposit, req, {
        headers: idempotentHeader(idempotencyKey),
      }),
    ),
};
