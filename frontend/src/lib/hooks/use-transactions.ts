"use client";

import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { toast } from "sonner";
import {
  transactionsApi,
  type CashReq,
  type TransactionSearchParams,
  type TransferReq,
} from "@/lib/api/transactions";

// Error toasts are emitted by the axios response interceptor (api/client.ts).
// Mutation hooks intentionally omit onError to avoid duplicate toasts.

const TX_KEY = "transactions";

export function useTransactions(params: TransactionSearchParams) {
  return useQuery({
    queryKey: [TX_KEY, params],
    queryFn: () => transactionsApi.search(params),
    placeholderData: keepPreviousData,
  });
}

function onTxSuccess(qc: ReturnType<typeof useQueryClient>) {
  return () => {
    toast.success("Giao dịch thành công");
    qc.invalidateQueries({ queryKey: [TX_KEY] });
    qc.invalidateQueries({ queryKey: ["accounts"] });
  };
}

export function useTransfer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      req,
      key,
    }: {
      req: TransferReq;
      key: string;
    }) => transactionsApi.transfer(req, key),
    onSuccess: onTxSuccess(qc),
  });
}

export function useWithdraw() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ req, key }: { req: CashReq; key: string }) =>
      transactionsApi.withdraw(req, key),
    onSuccess: onTxSuccess(qc),
  });
}

export function useDeposit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ req, key }: { req: CashReq; key: string }) =>
      transactionsApi.deposit(req, key),
    onSuccess: onTxSuccess(qc),
  });
}
