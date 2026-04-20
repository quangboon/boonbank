const COOKIE_NAME = "app_session";

export type SessionCookie = {
  role: "ADMIN" | "CUSTOMER" | "FRAUD" | string;
  exp: number;
};

export function writeSessionCookie(session: SessionCookie, maxAgeSeconds: number) {
  if (typeof document === "undefined") return;
  const value = encodeURIComponent(JSON.stringify(session));
  const secure = location.protocol === "https:" ? "; Secure" : "";
  document.cookie = `${COOKIE_NAME}=${value}; Path=/; Max-Age=${maxAgeSeconds}; SameSite=Lax${secure}`;
}

export function clearSessionCookie() {
  if (typeof document === "undefined") return;
  document.cookie = `${COOKIE_NAME}=; Path=/; Max-Age=0; SameSite=Lax`;
}

export const SESSION_COOKIE_NAME = COOKIE_NAME;
