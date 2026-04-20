import { api, unwrap } from "./client";
import { ENDPOINTS } from "./endpoints";
import type { PageResponse } from "./types";
import type { User } from "@/types/domain";

export type UserSearchParams = {
  page?: number;
  size?: number;
  sort?: string;
};

export type ResetPasswordRes = { tempPassword: string };

export const usersApi = {
  list: (params: UserSearchParams) =>
    unwrap<PageResponse<User>>(api.get(ENDPOINTS.users, { params })),
  get: (id: string) => unwrap<User>(api.get(`${ENDPOINTS.users}/${id}`)),
  listByCustomer: (customerId: string) =>
    unwrap<User[]>(api.get(`${ENDPOINTS.users}/by-customer/${customerId}`)),
  enable: (id: string) => api.post(`${ENDPOINTS.users}/${id}/enable`),
  disable: (id: string) => api.post(`${ENDPOINTS.users}/${id}/disable`),
  resetPassword: (id: string) =>
    unwrap<ResetPasswordRes>(api.post(`${ENDPOINTS.users}/${id}/reset-password`)),
};
