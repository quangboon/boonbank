import axios, {
  AxiosError,
  AxiosHeaders,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from "axios";
import { toast } from "sonner";
import { tokenStore } from "@/lib/auth/token-store";
import { decodeToken } from "@/lib/auth/jwt-decode";
import {
  clearSessionCookie,
  writeSessionCookie,
} from "@/lib/auth/session-cookie";
import { ENDPOINTS } from "./endpoints";
import type { ApiResponse, ErrorResponse } from "./types";

function syncSessionCookieFromToken(accessToken: string) {
  const decoded = decodeToken(accessToken);
  if (!decoded) return;
  tokenStore.setUser(decoded);
  const role = decoded.roles.includes("ADMIN")
    ? "ADMIN"
    : decoded.roles.includes("FRAUD")
      ? "FRAUD"
      : decoded.roles.includes("CUSTOMER")
        ? "CUSTOMER"
        : (decoded.roles[0] ?? "CUSTOMER");
  const maxAge = Math.max(60, decoded.exp - Math.floor(Date.now() / 1000));
  writeSessionCookie({ role, exp: decoded.exp }, maxAge);
}

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

const AUTH_URLS = [
  ENDPOINTS.auth.login,
  ENDPOINTS.auth.refresh,
  ENDPOINTS.auth.logout,
];

function isAuthEndpoint(url?: string) {
  if (!url) return false;
  return AUTH_URLS.some((u) => url.includes(u));
}

export const api = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = tokenStore.getAccess();
  if (token) {
    const headers = AxiosHeaders.from(config.headers);
    headers.set("Authorization", `Bearer ${token}`);
    config.headers = headers;
  }
  return config;
});

let refreshPromise: Promise<string | null> | null = null;
let isRedirecting = false;

async function refreshAccessToken(): Promise<string | null> {
  if (refreshPromise) return refreshPromise;
  refreshPromise = (async () => {
    const refreshToken = tokenStore.getRefresh();
    if (!refreshToken) return null;
    try {
      const resp = await axios.post<
        ApiResponse<{ accessToken: string; refreshToken: string }>
      >(
        `${BASE_URL}${ENDPOINTS.auth.refresh}`,
        { refreshToken },
        { timeout: 10000 },
      );
      const data = resp.data?.data;
      if (!data?.accessToken) return null;
      tokenStore.setTokens(data.accessToken, data.refreshToken ?? refreshToken);
      syncSessionCookieFromToken(data.accessToken);
      return data.accessToken;
    } catch {
      return null;
    }
  })();

  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

function redirectToLogin() {
  if (isRedirecting || typeof window === "undefined") return;
  if (window.location.pathname.startsWith("/login")) return;
  isRedirecting = true;
  toast.error("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại.");
  window.location.href = `/login?next=${encodeURIComponent(
    window.location.pathname,
  )}`;
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ErrorResponse>) => {
    const original = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined;

    const status = error.response?.status;
    const url = original?.url;

    if (status === 401 && original && !original._retry && !isAuthEndpoint(url)) {
      original._retry = true;
      const newToken = await refreshAccessToken();
      if (newToken) {
        const headers = AxiosHeaders.from(original.headers);
        headers.set("Authorization", `Bearer ${newToken}`);
        original.headers = headers;
        return api(original as AxiosRequestConfig);
      }
      tokenStore.clear();
      clearSessionCookie();
      redirectToLogin();
      return Promise.reject(error);
    }

    if (error.response && status !== 401 && !isAuthEndpoint(url)) {
      const body = error.response.data;
      const msg =
        body?.detail ||
        body?.title ||
        error.message ||
        "Đã có lỗi xảy ra, vui lòng thử lại.";
      toast.error(msg);
    }
    return Promise.reject(error);
  },
);

export async function unwrap<T>(
  promise: Promise<{ data: ApiResponse<T> }>,
): Promise<T> {
  const resp = await promise;
  return resp.data.data;
}
