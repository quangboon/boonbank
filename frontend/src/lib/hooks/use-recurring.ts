"use client";

import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { toast } from "sonner";
import {
  recurringApi,
  type RecurringCreateReq,
  type RecurringSearchParams,
  type RecurringUpdateReq,
} from "@/lib/api/recurring";

// Error toasts are emitted by the axios response interceptor (api/client.ts).
// Mutation hooks intentionally omit onError to avoid duplicate toasts.

const KEY = "recurring";

export function useRecurringList(params: RecurringSearchParams) {
  return useQuery({
    queryKey: [KEY, params],
    queryFn: () => recurringApi.list(params),
    placeholderData: keepPreviousData,
  });
}

export function useCreateRecurring() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: RecurringCreateReq) => recurringApi.create(req),
    onSuccess: () => {
      toast.success("Đã tạo giao dịch định kỳ");
      qc.invalidateQueries({ queryKey: [KEY] });
    },
  });
}

export function useUpdateRecurring() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, req }: { id: string; req: RecurringUpdateReq }) =>
      recurringApi.update(id, req),
    onSuccess: () => {
      toast.success("Đã cập nhật");
      qc.invalidateQueries({ queryKey: [KEY] });
    },
  });
}

export function useToggleRecurring() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, enabled }: { id: string; enabled: boolean }) =>
      enabled ? recurringApi.enable(id) : recurringApi.disable(id),
    onSuccess: () => {
      toast.success("Đã thay đổi trạng thái");
      qc.invalidateQueries({ queryKey: [KEY] });
    },
  });
}

export function useDeleteRecurring() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => recurringApi.delete(id),
    onSuccess: () => {
      toast.success("Đã xoá");
      qc.invalidateQueries({ queryKey: [KEY] });
    },
  });
}
