import Icon from './Icon.jsx'

export const statuses = ['Applied', 'Interview', 'Selected', 'Rejected', 'Withdrawn']

export function PageIntro({ eyebrow, title, description, action, children }) {
  return (
    <div className="page-intro">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p className="page-description">{description}</p>}
      </div>
      <div className="page-intro-actions">{children}{action}</div>
    </div>
  )
}

export function StatusBadge({ status }) {
  return <span className={`status-badge status-${String(status || '').toLowerCase()}`}>{status || 'Unknown'}</span>
}

export function Notice({ children, tone = 'error', onDismiss }) {
  if (!children) return null
  return (
    <div className={`notice notice-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <span>{children}</span>
      {onDismiss && <button className="notice-close" onClick={onDismiss} aria-label="Dismiss message" type="button"><Icon name="close" size={15} /></button>}
    </div>
  )
}

export function EmptyState({ icon = 'sparkle', title, description, action }) {
  return (
    <div className="empty-state">
      <div className="empty-icon"><Icon name={icon} size={22} /></div>
      <h3>{title}</h3>
      {description && <p>{description}</p>}
      {action}
    </div>
  )
}

export function LoadingState({ label = 'Loading your workspace…' }) {
  return <div className="loading-state"><span className="spinner" />{label}</div>
}

export function FailureState({ title = 'Could not connect to the backend', description = 'Start Spring Boot on port 8080 and try again.', onRetry }) {
  return <div className="failure-state"><span className="failure-icon"><Icon name="refresh" size={20} /></span><h3>{title}</h3><p>{description}</p>{onRetry && <button className="button button-secondary button-small" type="button" onClick={onRetry}><Icon name="refresh" size={15} />Try again</button>}</div>
}

export function formatDate(value, options = { month: 'short', day: 'numeric', year: 'numeric' }) {
  if (!value) return '—'
  const date = new Date(`${value.slice(0, 10)}T12:00:00`)
  return Number.isNaN(date.getTime()) ? '—' : new Intl.DateTimeFormat(undefined, options).format(date)
}

export function formatMoney(value) {
  const number = Number(value)
  return Number.isFinite(number)
    ? new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 }).format(number)
    : '—'
}

export function relativeDate(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

export function initials(name = '') {
  return name.trim().split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join('') || 'U'
}
