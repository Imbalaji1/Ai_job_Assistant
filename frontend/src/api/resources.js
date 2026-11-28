import { request, requestBlob, streamRequest } from './client.js'

function queryString(values) {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined && value !== null && value !== '') params.set(key, value)
  }
  const query = params.toString()
  return query ? `?${query}` : ''
}

export const dashboardApi = {
  statusCounts: () => request('/dashboard/status-counts'),
}

export const applicationsApi = {
  list: (filters = {}) => request(`/applications${queryString(filters)}`),
  get: (id) => request(`/applications/${id}`),
  create: (application) => request('/applications', jsonOptions('POST', application)),
  update: (id, application) => request(`/applications/${id}`, jsonOptions('PUT', application)),
  updateStatus: (id, status) => request(`/applications/${id}/status`, jsonOptions('PATCH', { status })),
  updateInterviewDate: (id, interviewDate) => request(`/applications/${id}/interview-date`, jsonOptions('PATCH', { interviewDate })),
  history: (id) => request(`/applications/${id}/history`),
  delete: (id) => request(`/applications/${id}`, { method: 'DELETE' }),
  resumeSubmissions: (id) => request(`/applications/${id}/resume-submissions`),
  linkResume: (id, resumeVersionId) => request(`/applications/${id}/resume-submissions`, jsonOptions('POST', { resumeVersionId })),
  createFromResume: (resumeId, application) => {
    const form = new FormData()
    form.append('application', new Blob([JSON.stringify(application)], { type: 'application/json' }))
    return request(`/resumes/${resumeId}/applications`, { method: 'POST', body: form })
  },
}

export const resumesApi = {
  list: () => request('/resumes'),
  upload: (file) => {
    const form = new FormData()
    form.append('file', file)
    return request('/resumes', { method: 'POST', body: form })
  },
  download: (id) => requestBlob(`/resumes/${id}/download`),
  delete: (id) => request(`/resumes/${id}`, { method: 'DELETE' }),
}

export const chatsApi = {
  list: () => request('/ai/chats'),
  create: (payload) => request('/ai/chats', jsonOptions('POST', payload)),
  messages: (id) => request(`/ai/chats/${id}/messages`),
  send: (id, message) => request(`/ai/chats/${id}/messages`, jsonOptions('POST', { message })),
  stream: (id, message, onToken) => streamRequest(`/ai/chats/${id}/messages/stream`, { message }, onToken),
  reviseResume: (id, instructions) => request(`/ai/chats/${id}/resume-revisions`, jsonOptions('POST', { instructions })),
  keepResume: (chatId, versionId, keep) => request(`/ai/chats/${chatId}/resume-revisions/${versionId}/keep-choice`, jsonOptions('POST', { keep })),
  delete: (id) => request(`/ai/chats/${id}`, { method: 'DELETE' }),
}

export const historyApi = {
  summary: () => request('/history/summary'),
  events: (page = 0, size = 20) => request(`/history${queryString({ page, size })}`),
}

export const profileApi = {
  get: () => request('/profile'),
  save: (profile) => request('/profile', jsonOptions('PUT', profile)),
}

function jsonOptions(method, body) {
  return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }
}
