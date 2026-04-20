import { api, unwrap } from "./client";
import { ENDPOINTS } from "./endpoints";

export type TokenPair = {
  accessToken: string;
  refreshToken: string;
};

export type LoginReq = {
  username: string;
  password: string;
};

export const authApi = {
  login: (req: LoginReq) =>
    unwrap<TokenPair>(api.post(ENDPOINTS.auth.login, req)),

  refresh: (refreshToken: string) =>
    unwrap<TokenPair>(api.post(ENDPOINTS.auth.refresh, { refreshToken })),

  logout: () => api.post(ENDPOINTS.auth.logout).catch(() => undefined),
};
