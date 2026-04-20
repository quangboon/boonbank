"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
  keepPreviousData,
} from "@tanstack/react-query";
import { toast } from "sonner";
import {
  customersApi,
  type CustomerCreateReq,
  type CustomerSearchParams,
  type CustomerUpdateReq,
} from "@/lib/api/customers";

const CUSTOMERS_KEY = "customers";

export function useCustomers(params: CustomerSearchParams) {
  return useQuery({
    queryKey: [CUSTOMERS_KEY, params],
    queryFn: () => customersApi.list(params),
    placeholderData: keepPreviousData,
  });
}

export function useCustomer(id: string | undefined) {
  return useQuery({
    queryKey: [CUSTOMERS_KEY, "detail", id],
    queryFn: () => customersApi.get(id!),
    enabled: Boolean(id),
  });
}

export function useCreateCustomer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (req: CustomerCreateReq) => customersApi.create(req),
    onSuccess: () => {
      toast.success("Đã tạo khách hàng");
      qc.invalidateQueries({ queryKey: [CUSTOMERS_KEY] });
    },
  });
}

export function useUpdateCustomer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, req }: { id: string; req: CustomerUpdateReq }) =>
      customersApi.update(id, req),
    onSuccess: (_d, v) => {
      toast.success("Đã cập nhật khách hàng");
      qc.invalidateQueries({ queryKey: [CUSTOMERS_KEY] });
      qc.invalidateQueries({ queryKey: [CUSTOMERS_KEY, "detail", v.id] });
    },
  });
}

export function useDeleteCustomer() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => customersApi.delete(id),
    onSuccess: () => {
      toast.success("Đã xoá khách hàng");
      qc.invalidateQueries({ queryKey: [CUSTOMERS_KEY] });
    },
  });
}
