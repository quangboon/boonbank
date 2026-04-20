import { jwtDecode } from "jwt-decode";
import type { StoredUser } from "./token-store";

type JwtClaims = {
  sub: string;
  roles?: string[];
  exp: number;
};

export function decodeToken(token: string): StoredUser | null {
  try {
    const c = jwtDecode<JwtClaims>(token);
    return {
      sub: c.sub,
      username: c.sub,
      roles: c.roles ?? [],
      exp: c.exp,
    };
  } catch {
    return null;
  }
}

export function isExpired(exp: number, skewSeconds = 30): boolean {
  const nowSec = Math.floor(Date.now() / 1000);
  return exp - skewSeconds <= nowSec;
}
