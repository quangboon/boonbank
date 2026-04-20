import { api, unwrap } from "./client";
import { ENDPOINTS } from "./endpoints";
import type { PageResponse } from "./types";
import type { Customer } from "@/types/domain";

export type CustomerCreateReq = {
  fullName: string;
  idNumber: string;
  email: string;
  phone: string;
  address?: string;
  location?: string;
  dateOfBirth?: string;
  customerTypeCode?: string;
};

export type CustomerUpdateReq = Partial<
  Pick<
    CustomerCreateReq,
    "fullName" | "email" | "phone" | "address" | "location" | "customerTypeCode"
  >
>;

export type CustomerSearchParams = {
  keyword?: string;
  location?: string;
  customerTypeCode?: string;
  page?: number;
  size?: number;
  sort?: string;
};

export type CustomerCreateRes = {
  customer: Customer;
  credentials: { username: string; tempPassword: string };
};

export const customersApi = {
  list: (params: CustomerSearchParams) =>
    unwrap<PageResponse<Customer>>(api.get(ENDPOINTS.customers, { params })),
  get: (id: string) => unwrap<Customer>(api.get(`${ENDPOINTS.customers}/${id}`)),
  create: (req: CustomerCreateReq) =>
    unwrap<CustomerCreateRes>(api.post(ENDPOINTS.customers, req)),
  update: (id: string, req: CustomerUpdateReq) =>
    unwrap<Customer>(api.put(`${ENDPOINTS.customers}/${id}`, req)),
  delete: (id: string) => api.delete(`${ENDPOINTS.customers}/${id}`),
};
