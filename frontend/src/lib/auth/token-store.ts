const ACCESS_KEY = "app_access_token";
const REFRESH_KEY = "app_refresh_token";
const USER_KEY = "app_user";

export type StoredUser = {
  sub: string;
  username: string;
  roles: string[];
  customerId?: string;
  exp: number;
};

function safeGet(key: string): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(key);
}

function safeSet(key: string, value: string | null) {
  if (typeof window === "undefined") return;
  if (value === null) window.localStorage.removeItem(key);
  else window.localStorage.setItem(key, value);
}

export const tokenStore = {
  getAccess: () => safeGet(ACCESS_KEY),
  getRefresh: () => safeGet(REFRESH_KEY),
  setTokens: (access: string, refresh: string) => {
    safeSet(ACCESS_KEY, access);
    safeSet(REFRESH_KEY, refresh);
  },
  clear: () => {
    safeSet(ACCESS_KEY, null);
    safeSet(REFRESH_KEY, null);
    safeSet(USER_KEY, null);
  },
  getUser: (): StoredUser | null => {
    const raw = safeGet(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StoredUser;
    } catch {
      return null;
    }
  },
  setUser: (user: StoredUser | null) => {
    safeSet(USER_KEY, user ? JSON.stringify(user) : null);
  },
};
