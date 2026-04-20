"use client";

import { useQuery } from "@tanstack/react-query";
import { statisticsApi } from "@/lib/api/statistics";
import type { PeriodUnit } from "@/types/domain";

export function useAccountsByProductType() {
  return useQuery({
    queryKey: ["statistics", "accounts-by-product-type"],
    queryFn: () => statisticsApi.accountsByProductType(),
  });
}

export function useAccountsByBalanceTier() {
  return useQuery({
    queryKey: ["statistics", "accounts-by-balance-tier"],
    queryFn: () => statisticsApi.accountsByBalanceTier(),
  });
}

export function useCustomersByLocation() {
  return useQuery({
    queryKey: ["statistics", "customers-by-location"],
    queryFn: () => statisticsApi.customersByLocation(),
  });
}

export function useTransactionsSummary(params: {
  period: PeriodUnit;
  from: string;
  to: string;
  accountId?: string;
  enabled?: boolean;
}) {
  const { enabled = true, ...rest } = params;
  return useQuery({
    queryKey: ["statistics", "transactions-summary", rest],
    queryFn: () => statisticsApi.transactionsSummary(rest),
    enabled: enabled && Boolean(rest.from && rest.to),
  });
}
