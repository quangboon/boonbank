"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { tokenStore, type StoredUser } from "@/lib/auth/token-store";
import { decodeToken, isExpired } from "@/lib/auth/jwt-decode";
import {
  clearSessionCookie,
  writeSessionCookie,
} from "@/lib/auth/session-cookie";
import { authApi, type TokenPair } from "@/lib/api/auth";

type AuthContextValue = {
  user: StoredUser | null;
  isAuthed: boolean;
  isAdmin: boolean;
  isCustomer: boolean;
  signIn: (username: string, password: string) => Promise<StoredUser>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function primaryRole(user: StoredUser | null): string | null {
  if (!user) return null;
  if (user.roles.includes("ADMIN")) return "ADMIN";
  if (user.roles.includes("FRAUD")) return "FRAUD";
  if (user.roles.includes("CUSTOMER")) return "CUSTOMER";
  return user.roles[0] ?? null;
}

function syncCookie(user: StoredUser | null) {
  if (!user) {
    clearSessionCookie();
    return;
  }
  const role = primaryRole(user) ?? "CUSTOMER";
  const maxAge = Math.max(60, user.exp - Math.floor(Date.now() / 1000));
  writeSessionCookie({ role, exp: user.exp }, maxAge);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<StoredUser | null>(null);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    const stored = tokenStore.getUser();
    const access = tokenStore.getAccess();
    if (stored && access && !isExpired(stored.exp)) {
      setUser(stored);
      syncCookie(stored);
    } else {
      tokenStore.clear();
      clearSessionCookie();
    }
    setHydrated(true);

    const onStorage = (e: StorageEvent) => {
      if (e.key === "app_access_token" && e.newValue === null) {
        setUser(null);
        clearSessionCookie();
      }
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const applyTokens = useCallback((tokens: TokenPair): StoredUser => {
    tokenStore.setTokens(tokens.accessToken, tokens.refreshToken);
    const decoded = decodeToken(tokens.accessToken);
    if (!decoded) {
      tokenStore.clear();
      clearSessionCookie();
      throw new Error("Token giải mã thất bại");
    }
    tokenStore.setUser(decoded);
    syncCookie(decoded);
    setUser(decoded);
    return decoded;
  }, []);

  const signIn = useCallback(
    async (username: string, password: string) => {
      const tokens = await authApi.login({ username, password });
      return applyTokens(tokens);
    },
    [applyTokens],
  );

  const signOut = useCallback(async () => {
    void authApi.logout();
    tokenStore.clear();
    clearSessionCookie();
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthed: hydrated && user !== null,
      isAdmin: user?.roles.includes("ADMIN") ?? false,
      isCustomer: user?.roles.includes("CUSTOMER") ?? false,
      signIn,
      signOut,
    }),
    [user, hydrated, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
