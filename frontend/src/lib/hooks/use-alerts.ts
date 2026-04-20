"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { alertsApi, type AlertsSearchParams } from "@/lib/api/alerts";

export function useAlerts(params: AlertsSearchParams) {
  return useQuery({
    queryKey: ["alerts", params],
    queryFn: () => alertsApi.search(params),
    placeholderData: keepPreviousData,
  });
}

export function useOpenAlerts() {
  return useQuery({
    queryKey: ["alerts", "open"],
    queryFn: () => alertsApi.open(),
    refetchInterval: 60_000,
  });
}
