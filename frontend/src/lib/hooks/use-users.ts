"use client";

import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { toast } from "sonner";
import { usersApi, type UserSearchParams } from "@/lib/api/users";

const USERS_KEY = "users";

export function useUsers(params: UserSearchParams) {
  return useQuery({
    queryKey: [USERS_KEY, params],
    queryFn: () => usersApi.list(params),
    placeholderData: keepPreviousData,
  });
}

export function useUsersByCustomer(customerId: string | undefined) {
  return useQuery({
    queryKey: [USERS_KEY, "by-customer", customerId],
    queryFn: () => usersApi.listByCustomer(customerId!),
    enabled: Boolean(customerId),
  });
}

export function useEnableUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => usersApi.enable(id),
    onSuccess: () => {
      toast.success("Đã bật user");
      qc.invalidateQueries({ queryKey: [USERS_KEY] });
    },
  });
}

export function useDisableUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => usersApi.disable(id),
    onSuccess: () => {
      toast.success("Đã tắt user");
      qc.invalidateQueries({ queryKey: [USERS_KEY] });
    },
  });
}

export function useResetUserPassword() {
  return useMutation({
    mutationFn: (id: string) => usersApi.resetPassword(id),
    onSuccess: () => {
      toast.success("Đã đặt lại mật khẩu");
    },
  });
}
