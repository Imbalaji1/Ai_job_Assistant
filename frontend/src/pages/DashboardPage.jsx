import { useCallback, useEffect, useMemo, useState } from 'react'
import { applicationsApi, dashboardApi, historyApi, resumesApi } from '../api/resources.js'
import Icon from '../components/Icon.jsx'
import { EmptyState, FailureState, formatDate, formatMoney, LoadingState, Notice, PageIntro, StatusBadge } from '../components/UI.jsx'

const chartColors = {
  Applied: '#7188e8',
  Interview: '#e8a84b',
  Selected: '#35b896',
  Rejected: '#e16e7c',
  Withdrawn: '#a0aab8',
}

const localDateKey = () => {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export default function DashboardPage({ onNavigate, onCreateApplication, onOpenChat }) {
  const [data, setData] = useState({ counts: null, applications: [], resumes: [], history: null })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [counts, applications, resumes, history] = await Promise.all([
        dashboardApi.statusCounts(),
        applicationsApi.list({ page: 0, size: 100 }),
        resumesApi.list(),
        historyApi.summary(),
      ])
      setData({ counts, applications: applications.content || [], resumes, history })
    } catch (requestError) {
      setError(requestError.message || 'Could not load your dashboard.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const applications = useMemo(() => [...data.applications].sort((left, right) =>
    String(right.appliedDate).localeCompare(String(left.appliedDate))), [data.applications])
  const upcoming = useMemo(() => data.applications
    .filter((item) => item.interviewDate && item.interviewDate >= localDateKey() && item.status !== 'Rejected' && item.status !== 'Withdrawn')
    .sort((left, right) => String(left.interviewDate).localeCompare(String(right.interviewDate)))
    .slice(0, 3), [data.applications])

  const counts = data.counts?.countsByStatus || {}
  const total = data.counts?.totalApplications ?? 0
  const statusList = ['Applied', 'Interview', 'Selected', 'Rejected', 'Withdrawn']
  const segments = []
  let cursor = 0
  for (const status of statusList) {
    const count = Number(counts[status] || 0)
    if (!total || !count) continue
    const next = cursor + (count / total) * 100
    segments.push(`${chartColors[status]} ${cursor}% ${next}%`)
    cursor = next
  }
  const donutStyle = { background: segments.length ? `conic-gradient(${segments.join(', ')})` : 'conic-gradient(var(--line-strong) 0 100%)' }

  const monthBars = useMemo(() => {
    const now = new Date()
    const months = []
    for (let offset = 5; offset >= 0; offset--) {
      const monthDate = new Date(now.getFullYear(), now.getMonth() - offset, 1)
      const key = `${monthDate.getFullYear()}-${String(monthDate.getMonth() + 1).padStart(2, '0')}`
      const count = data.applications.filter((application) => String(application.appliedDate || '').startsWith(key)).length
      months.push({ key, label: new Intl.DateTimeFormat(undefined, { month: 'short' }).format(monthDate), count })
    }
    return months
  }, [data.applications])
  const maxMonth = Math.max(1, ...monthBars.map((month) => month.count))

  return (
    <div className="page-stack">
      <PageIntro eyebrow="YOUR OVERVIEW" title="Good things are in motion." description="A calmer view of every application and what comes next." />
      <Notice>{error}</Notice>
      {loading ? <LoadingState label="Gathering your job search details…" /> : error && !data.counts ? <FailureState title="Your dashboard couldn’t load" description={error} onRetry={load} /> : <>
        <section className="stats-grid" aria-label="Job search summary">
          <StatCard label="Applications sent" value={total} icon="briefcase" tone="blue" note="Across your current search" />
          <StatCard label="Interviews" value={counts.Interview || 0} icon="calendar" tone="amber" note="Conversations in progress" />
          <StatCard label="Offers received" value={counts.Selected || 0} icon="check" tone="green" note="A wonderful step forward" />
          <StatCard label="Resume versions" value={data.resumes.length} icon="file" tone="violet" note="Saved securely in your files" />
        </section>

        <section className="dashboard-grid">
          <article className="panel applications-chart-panel">
            <div className="panel-heading">
              <div><h2>Application snapshot</h2><p>See how your search is progressing</p></div>
              <button type="button" className="text-button" onClick={() => onNavigate('applications')}>View applications <Icon name="arrow" size={15} /></button>
            </div>
            <div className="chart-content">
              <div className="donut-wrap">
                <div className="donut-chart" style={donutStyle}><span><strong>{total}</strong><small>total</small></span></div>
              </div>
              <div className="chart-legend">
                {statusList.map((status) => <div className="legend-row" key={status}>
                  <span className="legend-label"><i style={{ background: chartColors[status] }} />{status}</span>
                  <strong>{counts[status] || 0}</strong>
                </div>)}
              </div>
            </div>
          </article>

          <article className="panel activity-chart-panel">
            <div className="panel-heading"><div><h2>Your momentum</h2><p>Applications added in the past six months</p></div><span className="panel-icon"><Icon name="grid" size={17} /></span></div>
            <div className="bar-chart" aria-label="Applications added in the past six months">
              {monthBars.map((month) => <div className="bar-column" key={month.key}>
                <span className="bar-count">{month.count || ''}</span>
                <div className="bar-track"><span style={{ height: `${Math.max(month.count ? 12 : 3, (month.count / maxMonth) * 100)}%` }} /></div>
                <small>{month.label}</small>
              </div>)}
            </div>
            <div className="chart-summary"><span className="chart-summary-dot" />{total ? `${total} thoughtful steps in your search so far` : 'Your first application will appear here'}</div>
          </article>
        </section>

        <section className="dashboard-grid dashboard-bottom-grid">
          <article className="panel table-panel">
            <div className="panel-heading">
              <div><h2>Recent applications</h2><p>Your latest job search activity</p></div>
              <button type="button" className="text-button" onClick={() => onNavigate('applications')}>See all <Icon name="arrow" size={15} /></button>
            </div>
            {applications.length ? <div className="table-scroll">
              <table className="data-table compact-table">
                <thead><tr><th>Company & role</th><th>Applied</th><th>Status</th><th>Salary</th></tr></thead>
                <tbody>{applications.slice(0, 5).map((application) => <tr key={application.id}>
                  <td><strong>{application.companyName}</strong><small>{application.jobRole}</small></td>
                  <td>{formatDate(application.appliedDate, { month: 'short', day: 'numeric' })}</td>
                  <td><StatusBadge status={application.status} /></td>
                  <td>{formatMoney(application.salaryLpa)} <span className="muted-inline">LPA</span></td>
                </tr>)}</tbody>
              </table>
            </div> : <EmptyState icon="briefcase" title="Your search starts here" description="Add the first role you apply for and keep the details in one place." action={<button className="button button-primary button-small" onClick={onCreateApplication}><Icon name="plus" size={16} />Add application</button>} />}
          </article>

          <article className="panel interview-panel">
            <div className="panel-heading"><div><h2>Coming up</h2><p>Interview dates on your calendar</p></div><span className="panel-icon panel-icon-amber"><Icon name="calendar" size={17} /></span></div>
            {upcoming.length ? <div className="upcoming-list">
              {upcoming.map((item) => <div className="upcoming-item" key={item.id}>
                <div className="upcoming-date"><strong>{new Date(`${item.interviewDate}T12:00:00`).getDate()}</strong><small>{new Intl.DateTimeFormat(undefined, { month: 'short' }).format(new Date(`${item.interviewDate}T12:00:00`))}</small></div>
                <div className="upcoming-copy"><strong>{item.companyName}</strong><span>{item.jobRole}</span></div>
                <Icon name="chevron" size={16} />
              </div>)}
              <button className="text-button upcoming-link" type="button" onClick={() => onNavigate('applications')}>Review your applications <Icon name="arrow" size={15} /></button>
            </div> : <EmptyState icon="calendar" title="Room for what’s next" description="Add an interview date to an application and we’ll keep it in view." action={<button className="text-button" onClick={() => onNavigate('applications')}>Go to applications <Icon name="arrow" size={15} /></button>} />}
          </article>
        </section>

        <section className="assistant-promo">
          <div className="promo-illustration"><span className="promo-spark promo-spark-one">✦</span><Icon name="sparkle" size={27} /><span className="promo-spark promo-spark-two">✧</span></div>
          <div><p className="eyebrow">A LITTLE HELP, WHEN YOU NEED IT</p><h2>Your next career conversation starts here.</h2><p>Review a resume, get ready for an interview, or talk through your next move with your local AI assistant.</p></div>
          <button className="button button-primary" type="button" onClick={() => onOpenChat()}><Icon name="message" size={17} />Open AI assistant <Icon name="arrow" size={15} /></button>
        </section>
      </>}
    </div>
  )
}

function StatCard({ label, value, icon, tone, note }) {
  return <article className="stat-card"><span className={`stat-icon stat-${tone}`}><Icon name={icon} size={19} /></span><span className="stat-label">{label}</span><strong className="stat-value">{value}</strong><small>{note}</small></article>
}
