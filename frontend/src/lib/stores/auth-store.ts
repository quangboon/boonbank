'use client'

import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { api } from '@/lib/api-client'
import type { AuthResult } from '@/types'

interface AuthState {
  token: string | null
  role: string | null
  username: string | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      role: null,
      username: null,

      async login(username, password) {
        // clear stale token trước khi login
        set({ token: null, role: null, username: null })
        const data = await api<AuthResult>('/api/v1/auth/login', {
          method: 'POST',
          body: JSON.stringify({ username, password }),
        })
        set({ token: data.token, role: data.role, username })
      },

      logout() {
        set({ token: null, role: null, username: null })
      },
    }),
    { name: 'auth-store' }
  )
)
