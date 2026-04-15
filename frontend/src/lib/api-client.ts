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

function redirect401() {
  if (typeof window !== 'undefined') {
    window.location.href = '/login'
  }
}

export async function api<T>(path: string, opts: RequestInit = {}): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(opts.headers as Record<string, string>),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${BASE_URL}${path}`, { ...opts, headers })

  if (res.status === 401) {
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

export async function apiRaw(path: string, opts: RequestInit = {}): Promise<Blob> {
  const token = getToken()
  const headers: Record<string, string> = {
    ...(opts.headers as Record<string, string>),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${BASE_URL}${path}`, { ...opts, headers })

  if (res.status === 401) {
    redirect401()
    throw new Error('Unauthorized')
  }

  if (!res.ok) {
    throw new Error(`Download failed: ${res.statusText}`)
  }

  return res.blob()
}
