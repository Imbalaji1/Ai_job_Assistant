import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { login, request } from './client.js'

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

let storageSpy

describe('authenticated API requests', () => {
  beforeEach(() => {
    storageSpy = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
    }
    vi.stubGlobal('sessionStorage', storageSpy)
    vi.stubGlobal('window', { dispatchEvent: vi.fn() })
    vi.stubGlobal('CustomEvent', class CustomEvent {
      constructor(type, options) { this.type = type; this.detail = options?.detail }
    })
  })

  afterEach(() => vi.unstubAllGlobals())

  it('silently refreshes an expired token and retries the original request', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({
        accessToken: 'first-token', expiresInSeconds: 1800,
        user: { id: 7, name: 'Nila', email: 'nila@example.com' },
      }))
      .mockResolvedValueOnce(jsonResponse({ message: 'expired' }, 401))
      .mockResolvedValueOnce(jsonResponse({
        accessToken: 'renewed-token', expiresInSeconds: 1800,
        user: { id: 7, name: 'Nila', email: 'nila@example.com' },
      }))
      .mockResolvedValueOnce(jsonResponse({ totalApplications: 3 }))
    vi.stubGlobal('fetch', fetchMock)

    await login('nila@example.com', 'a-long-password')
    const result = await request('/dashboard/status-counts')

    expect(result.totalApplications).toBe(3)
    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      '/api/auth/login',
      '/api/dashboard/status-counts',
      '/api/auth/refresh',
      '/api/dashboard/status-counts',
    ])
    expect(fetchMock.mock.calls[1][1].headers.get('Authorization'))
      .toBe('Bearer first-token')
    expect(fetchMock.mock.calls[2][1].credentials).toBe('include')
    expect(fetchMock.mock.calls[0][1].headers['X-Requested-With']).toBe('AIJobAssistant')
    expect(fetchMock.mock.calls[2][1].headers['X-Requested-With']).toBe('AIJobAssistant')
    expect(fetchMock.mock.calls[3][1].headers.get('Authorization'))
      .toBe('Bearer renewed-token')
    expect(storageSpy.getItem).not.toHaveBeenCalled()
    expect(storageSpy.setItem).not.toHaveBeenCalled()
    expect(storageSpy.removeItem).not.toHaveBeenCalled()
  })
})
