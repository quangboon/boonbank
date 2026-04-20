import { api, unwrap } from "./client";
import { ENDPOINTS } from "./endpoints";
import type { PageResponse } from "./types";

export type RecurringTransaction = {
  id: string;
  sourceAccountNumber: string;
  destinationAccountNumber: string;
  amount: string;
  cronExpression: string;
  nextRunAt: string | null;
  lastRunAt: string | null;
  enabled: boolean;
  createdAt: string;
};

export type RecurringCreateReq = {
  sourceAccountNumber: string;
  destinationAccountNumber: string;
  amount: string;
  cronExpression: string;
  enabled?: boolean;
};

export type RecurringUpdateReq = Partial<{
  amount: string;
  cronExpression: string;
  enabled: boolean;
}>;

export type RecurringSearchParams = {
  sourceAccountId?: string;
  enabled?: boolean;
  page?: number;
  size?: number;
  sort?: string;
};

export const recurringApi = {
  list: (params: RecurringSearchParams) =>
    unwrap<PageResponse<RecurringTransaction>>(
      api.get(ENDPOINTS.recurring, { params }),
    ),
  get: (id: string) =>
    unwrap<RecurringTransaction>(api.get(`${ENDPOINTS.recurring}/${id}`)),
  create: (req: RecurringCreateReq) =>
    unwrap<RecurringTransaction>(api.post(ENDPOINTS.recurring, req)),
  update: (id: string, req: RecurringUpdateReq) =>
    unwrap<RecurringTransaction>(
      api.put(`${ENDPOINTS.recurring}/${id}`, req),
    ),
  enable: (id: string) =>
    api.post(`${ENDPOINTS.recurring}/${id}/enable`, null),
  disable: (id: string) =>
    api.post(`${ENDPOINTS.recurring}/${id}/disable`, null),
  delete: (id: string) => api.delete(`${ENDPOINTS.recurring}/${id}`),
};
