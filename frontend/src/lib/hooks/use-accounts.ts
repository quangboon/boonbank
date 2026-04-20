"use client";

import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { toast } from "sonner";
import {
  accountsApi,
  type AccountCreateReq,
  type AccountSearchParams,
  type AccountUpdateReq,
} from "@/lib/api/accounts";

// Error toasts are emitted by the axios response interceptor (api/client.ts).
// Mutation hooks intentionally omit onError to avoid duplicate toasts.

const ACCOUNTS_KEY = "accounts";

export function useAccounts(params: AccountSearchParams) {
  return useQuery({
    queryKey: [ACCOUNTS_KEY, params],
    queryFn: () => accountsApi.list(params),
    placeholderData: keepPreviousData,
  });
}

export function useAccount(id: string | undefined) {
  return useQuery({
    queryKey: [ACCOUNTS_KEY, "detail", id],
    queryFn: () => accountsApi.get(id!),
    enabled: Boolean(id),
  });
}

export function useAccountStatusHistory(id: string | undefined) {
  return useQuery({
    queryKey: [ACCOUNTS_KEY, "history", id],
    queryFn: () => accountsApi.statusHistory(id!),
    enabled: Boolean(id),
  });
}

/**
 * Lookup chủ TK theo số TK. Chỉ fetch khi length >= 10 (đủ dài để không spam).
 * Caller nên truyền value đã debounce (useDebounce).
 */
export function useAccountLookup(accountNumber: string) {
  return useQuery({
    queryKey: [ACCOUNTS_KEY, "lookup", accountNumber],
    queryFn: () => accountsApi.lookup(accountNumber),
    enabled: accountNumber.length >= 10,
    staleTime: 5 * 60 * 1000, // tên chủ TK ít đổi — giữ fresh 5 phút client-side
    retry: false, // 404 không retry
  });
}

export function useCreateAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: AccountCreateReq) => accountsApi.create(req),
    onSuccess: () => {
      toast.success("Đã mở tài khoản");
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY] });
    },
  });
}

export function useUpdateAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, req }: { id: string; req: AccountUpdateReq }) =>
      accountsApi.update(id, req),
    onSuccess: (_d, v) => {
      toast.success("Đã cập nhật tài khoản");
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY] });
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY, "detail", v.id] });
    },
  });
}

export function useFreezeAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      accountsApi.freeze(id, reason),
    onSuccess: (_d, v) => {
      toast.success("Đã khoá tài khoản");
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY] });
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY, "detail", v.id] });
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY, "history", v.id] });
    },
  });
}

export function useUnfreezeAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      accountsApi.unfreeze(id, reason),
    onSuccess: (_d, v) => {
      toast.success("Đã mở khoá tài khoản");
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY] });
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY, "detail", v.id] });
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY, "history", v.id] });
    },
  });
}

export function useCloseAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      accountsApi.close(id, reason),
    onSuccess: (_d, v) => {
      toast.success("Đã đóng tài khoản");
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY] });
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY, "detail", v.id] });
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY, "history", v.id] });
    },
  });
}

export function useDeleteAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => accountsApi.delete(id),
    onSuccess: () => {
      toast.success("Đã xoá tài khoản");
      qc.invalidateQueries({ queryKey: [ACCOUNTS_KEY] });
    },
  });
}
