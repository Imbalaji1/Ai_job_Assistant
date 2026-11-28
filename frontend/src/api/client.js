const AUTH_REQUEST_HEADER = 'X-Requested-With'
const AUTH_REQUEST_VALUE = 'AIJobAssistant'

// Bearer tokens stay in JavaScript memory. After a reload, the HttpOnly refresh
// cookie restores the session without exposing the access token to web storage.
let accessToken = null
let expiresAt = 0
let refreshInProgress = null

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export function getAccessTokenExpiry() {
  return expiresAt
}

export function getCurrentAccessToken() {
  return accessToken
}

export async function login(email, password) {
  return authenticate('/auth/login', { email, password })
}

export async function register(name, email, password) {
  return authenticate('/auth/register', { name, email, password })
}

export async function refreshAccessToken() {
  if (refreshInProgress) return refreshInProgress

  refreshInProgress = (async () => {
    let response
    try {
      response = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { [AUTH_REQUEST_HEADER]: AUTH_REQUEST_VALUE },
        credentials: 'include',
      })
    } catch (error) {
      throw new ApiError('Could not reach the sign-in service. Check your connection.', 0)
    }

    if (!response.ok) {
      const error = await readApiError(response)
      if (response.status === 401) {
        clearAccessToken()
        publish('aija:session-ended')
      }
      throw error
    }

    const session = await response.json()
    saveAccessToken(session)
    return session
  })().finally(() => {
    refreshInProgress = null
  })

  return refreshInProgress
}

export async function request(path, options = {}) {
  const perform = () => {
    const headers = new Headers(options.headers || {})
    if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
    return fetchWithConnectionMessage(`/api${path}`, {
      ...options,
      headers,
      credentials: 'include',
    })
  }

  let response = await perform()
  if (response.status === 401 && !options.skipRefresh) {
    await refreshAccessToken()
    response = await perform()
  }

  if (!response.ok) throw await readApiError(response)
  if (response.status === 204) return null
  return response.json()
}

export async function requestBlob(path, options = {}) {
  const perform = () => {
    const headers = new Headers(options.headers || {})
    if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
    return fetchWithConnectionMessage(`/api${path}`, {
      ...options,
      headers,
      credentials: 'include',
    })
  }

  let response = await perform()
  if (response.status === 401 && !options.skipRefresh) {
    await refreshAccessToken()
    response = await perform()
  }
  if (!response.ok) throw await readApiError(response)
  return response.blob()
}

export async function streamRequest(path, body, onToken) {
  const options = {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  }
  const perform = () => {
    const headers = new Headers(options.headers)
    if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
    return fetchWithConnectionMessage(`/api${path}`, {
      ...options,
      headers,
      credentials: 'include',
    })
  }

  let response = await perform()
  if (response.status === 401) {
    await refreshAccessToken()
    response = await perform()
  }
  if (!response.ok) throw await readApiError(response)
  if (!response.body) throw new ApiError('The chat stream was not available.', response.status)

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let answer = ''

  const consumeBlock = (block) => {
    let eventName = 'message'
    const data = []
    for (const line of block.split(/\r?\n/)) {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
    }
    if (eventName === 'token') {
      const token = data.join('\n')
      answer += token
      onToken?.(token, answer)
    }
    if (eventName === 'error') {
      throw new ApiError(data.join('\n') || 'The AI response stream failed.', 500)
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value, { stream: !done })
    let separator = buffer.match(/\r?\n\r?\n/)
    while (separator) {
      const index = separator.index
      consumeBlock(buffer.slice(0, index))
      buffer = buffer.slice(index + separator[0].length)
      separator = buffer.match(/\r?\n\r?\n/)
    }
    if (done) break
  }
  if (buffer.trim()) consumeBlock(buffer)
  return answer
}

export async function getCurrentUser() {
  return request('/auth/me')
}

export async function logout() {
  try {
    await fetch('/api/auth/logout', {
      method: 'POST',
      headers: { [AUTH_REQUEST_HEADER]: AUTH_REQUEST_VALUE },
      credentials: 'include',
    })
  } catch {
    // The local browser session should still end if the server is unavailable.
  } finally {
    clearAccessToken()
    publish('aija:session-ended')
  }
}

async function authenticate(path, body) {
  const response = await fetchWithConnectionMessage(`/api${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [AUTH_REQUEST_HEADER]: AUTH_REQUEST_VALUE,
    },
    credentials: 'include',
    body: JSON.stringify(body),
  })
  if (!response.ok) throw await readApiError(response)
  const session = await response.json()
  saveAccessToken(session)
  return session
}

function saveAccessToken(session) {
  accessToken = session.accessToken
  expiresAt = Date.now() + session.expiresInSeconds * 1000
  publish('aija:token-updated', { expiresAt })
}

function clearAccessToken() {
  accessToken = null
  expiresAt = 0
}

async function readApiError(response) {
  try {
    const body = await response.json()
    return new ApiError(body.message || `Request failed (${response.status}).`, response.status)
  } catch {
    return new ApiError(`Request failed (${response.status}).`, response.status)
  }
}

async function fetchWithConnectionMessage(url, options) {
  try {
    return await fetch(url, options)
  } catch {
    throw new ApiError(
      'Cannot reach the backend. Make sure Spring Boot is running on port 8080.',
      0,
    )
  }
}

function publish(name, detail) {
  if (typeof window !== 'undefined' && typeof CustomEvent !== 'undefined') {
    window.dispatchEvent(new CustomEvent(name, { detail }))
  }
}
