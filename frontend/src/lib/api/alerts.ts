import { api, unwrap } from "./client";
import { ENDPOINTS } from "./endpoints";
import type { PageResponse } from "./types";
import type { Alert, AlertSeverity } from "@/types/domain";

export type AlertsSearchParams = {
  severity?: AlertSeverity;
  resolved?: boolean;
  page?: number;
  size?: number;
  sort?: string;
};

export const alertsApi = {
  search: (params: AlertsSearchParams) =>
    unwrap<PageResponse<Alert>>(api.get(ENDPOINTS.alerts, { params })),
  open: () => unwrap<Alert[]>(api.get(ENDPOINTS.alertsOpen)),
};
