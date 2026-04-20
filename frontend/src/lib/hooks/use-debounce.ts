"use client";

import { useEffect, useState } from "react";

/**
 * Debounce giá trị input — chỉ update sau khi ngừng thay đổi `delayMs`.
 * Dùng cho lookup on-type tránh spam API.
 */
export function useDebounce<T>(value: T, delayMs = 500): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(t);
  }, [value, delayMs]);
  return debounced;
}
