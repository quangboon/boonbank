const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

function getToken(): string | null {
  try {
    const raw = localStorage.getItem('auth-store')
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return parsed?.state?.token ?? null
  } catch {
    return null
  }
}

function getRefreshToken(): string | null {
  try {
    const raw = localStorage.getItem('auth-store')
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return parsed?.state?.refreshToken ?? null
  } catch {
    return null
  }
}

function updateStoredTokens(token: string, refreshToken: string) {
  try {
    const raw = localStorage.getItem('auth-store')
    if (!raw) return
    const parsed = JSON.parse(raw)
    parsed.state.token = token
    parsed.state.refreshToken = refreshToken
    localStorage.setItem('auth-store', JSON.stringify(parsed))
  } catch {
    // ignore
  }
}

function clearStoredAuth() {
  try {
    const raw = localStorage.getItem('auth-store')
    if (!raw) return
    const parsed = JSON.parse(raw)
    parsed.state.token = null
    parsed.state.refreshToken = null
    parsed.state.role = null
    parsed.state.username = null
    localStorage.setItem('auth-store', JSON.stringify(parsed))
  } catch {
    // ignore
  }
}

function redirect401() {
  if (typeof window !== 'undefined') {
    clearStoredAuth()
    window.location.href = '/login'
  }
}

// --- Refresh token queue ---
// Ensures only one refresh request runs at a time.
// Concurrent 401s will all wait on the same promise.
let refreshPromise: Promise<boolean> | null = null

async function doRefreshToken(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false

  try {
    const res = await fetch(`${BASE_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })

    if (!res.ok) return false

    const json = await res.json()
    if (!json.success || !json.data) return false

    updateStoredTokens(json.data.token, json.data.refreshToken)
    return true
  } catch {
    return false
  }
}

function refreshTokenOnce(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = doRefreshToken().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

// --- API functions ---

export async function api<T>(
  path: string,
  opts: RequestInit = {},
  _isRetry = false,
): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(opts.headers as Record<string, string>),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${BASE_URL}${path}`, { ...opts, headers })

  if (res.status === 401) {
    // Don't attempt refresh if this is already a retry or a refresh/login request
    if (_isRetry || path.includes('/auth/refresh') || path.includes('/auth/login')) {
      redirect401()
      throw new Error('Unauthorized')
    }

    const refreshed = await refreshTokenOnce()
    if (refreshed) {
      return api<T>(path, opts, true)
    }

    redirect401()
    throw new Error('Unauthorized')
  }

  if (res.status === 204) return undefined as T

  const json = await res.json()
  if (!json.success) {
    throw new Error(json.message ?? 'Request failed')
  }

  return json.data as T
}

export async function apiRaw(
  path: string,
  opts: RequestInit = {},
  _isRetry = false,
): Promise<Blob> {
  const token = getToken()
  const headers: Record<string, string> = {
    ...(opts.headers as Record<string, string>),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${BASE_URL}${path}`, { ...opts, headers })

  if (res.status === 401) {
    if (_isRetry || path.includes('/auth/refresh') || path.includes('/auth/login')) {
      redirect401()
      throw new Error('Unauthorized')
    }

    const refreshed = await refreshTokenOnce()
    if (refreshed) {
      return apiRaw(path, opts, true)
    }

    redirect401()
    throw new Error('Unauthorized')
  }

  if (!res.ok) {
    throw new Error(`Download failed: ${res.statusText}`)
  }

  return res.blob()
}
