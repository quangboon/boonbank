export const SESSION_COOKIE = "app_session";

export type Session = { role: string; exp: number };

/**
 * Parse session cookie raw value. Trả về null nếu không hợp lệ hoặc đã hết hạn.
 * Dùng chung cho middleware (proxy.ts) và server component redirect ở root page.
 */
export function parseSession(raw: string | undefined): Session | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(decodeURIComponent(raw)) as Session;
    if (typeof parsed.exp !== "number") return null;
    if (parsed.exp * 1000 < Date.now()) return null;
    return parsed;
  } catch {
    return null;
  }
}

export function sessionLandingPath(session: Session): string {
  return session.role === "ADMIN" || session.role === "FRAUD"
    ? "/admin/dashboard"
    : "/my/accounts";
}
