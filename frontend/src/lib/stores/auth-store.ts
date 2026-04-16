'use client'

import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { api } from '@/lib/api-client'
import type { AuthResult } from '@/types'

interface AuthState {
  token: string | null
  refreshToken: string | null
  role: string | null
  username: string | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      refreshToken: null,
      role: null,
      username: null,

      async login(username, password) {
        // clear stale token trước khi login
        set({ token: null, refreshToken: null, role: null, username: null })
        const data = await api<AuthResult>('/api/v1/auth/login', {
          method: 'POST',
          body: JSON.stringify({ username, password }),
        })
        set({ token: data.token, refreshToken: data.refreshToken, role: data.role, username })
      },

      logout() {
        const { refreshToken } = get()
        // clear state immediately so UI reflects logged-out
        set({ token: null, refreshToken: null, role: null, username: null })
        // fire-and-forget server-side logout
        if (refreshToken) {
          fetch(
            `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/v1/auth/logout`,
            {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ refreshToken }),
            },
          ).catch(() => {
            // ignore – user is already logged out locally
          })
        }
      },
    }),
    { name: 'auth-store' },
  ),
)
