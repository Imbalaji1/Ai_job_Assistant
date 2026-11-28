import { useCallback, useEffect, useState } from 'react'
import { applicationsApi, resumesApi } from '../api/resources.js'
import Icon from '../components/Icon.jsx'
import Modal from '../components/Modal.jsx'
import { EmptyState, FailureState, formatDate, formatMoney, LoadingState, Notice, PageIntro, relativeDate, StatusBadge, statuses } from '../components/UI.jsx'

const today = () => {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
const blankApplication = () => ({ companyName: '', jobRole: '', salaryLpa: '', location: '', status: 'Applied', appliedDate: today(), interviewDate: '' })

export default function ApplicationsPage({ createTrigger = 0, onOpenChat, onNotify }) {
  const [filters, setFilters] = useState({ company: '', status: '' })
  const [submittedFilters, setSubmittedFilters] = useState({ company: '', status: '' })
  const [page, setPage] = useState(0)
  const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 0 })
  const [resumes, setResumes] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [selected, setSelected] = useState(null)
  const [detailData, setDetailData] = useState({ history: [], submissions: [], loading: false, error: '' })
  const [linkResumeId, setLinkResumeId] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [response, resumeList] = await Promise.all([
        applicationsApi.list({ ...submittedFilters, page, size: 10 }),
        resumesApi.list(),
      ])
      setResult(response)
      setResumes(resumeList)
    } catch (requestError) {
      setError(requestError.message || 'Could not load applications. Check that the backend is running.')
    } finally {
      setLoading(false)
    }
  }, [submittedFilters, page])

  useEffect(() => { load() }, [load])
  useEffect(() => {
    if (createTrigger) {
      setEditing(null)
      setShowForm(true)
    }
  }, [createTrigger])

  useEffect(() => {
    if (!selected) return
    setDetailData({ history: [], submissions: [], loading: true, error: '' })
    Promise.all([applicationsApi.history(selected.id), applicationsApi.resumeSubmissions(selected.id)])
      .then(([history, submissions]) => setDetailData({ history, submissions, loading: false, error: '' }))
      .catch((requestError) => setDetailData({ history: [], submissions: [], loading: false, error: requestError.message }))
  }, [selected])

  const openCreate = () => { setEditing(null); setShowForm(true) }
  const openEdit = (application) => { setEditing(application); setShowForm(true) }

  const saveApplication = async (form) => {
    const payload = {
      ...form,
      salaryLpa: Number(form.salaryLpa),
      interviewDate: form.interviewDate || null,
    }
    try {
      if (editing) await applicationsApi.update(editing.id, payload)
      else await applicationsApi.create(payload)
      setShowForm(false)
      setEditing(null)
      await load()
      onNotify?.(editing ? 'Application details updated.' : 'Application added to your tracker.', 'success')
    } catch (requestError) {
      throw requestError
    }
  }

  const changeStatus = async (application, status) => {
    try {
      await applicationsApi.updateStatus(application.id, status)
      await load()
      if (selected?.id === application.id) setSelected((current) => ({ ...current, status }))
      onNotify?.(`Status changed to ${status}.`, 'success')
    } catch (requestError) { setError(requestError.message) }
  }

  const removeApplication = async (application) => {
    if (!window.confirm(`Delete the application at ${application.companyName}? Its application history will be removed.`)) return
    try {
      await applicationsApi.delete(application.id)
      if (selected?.id === application.id) setSelected(null)
      await load()
      onNotify?.('Application deleted.', 'success')
    } catch (requestError) { setError(requestError.message) }
  }

  const submitSearch = (event) => {
    event.preventDefault()
    setPage(0)
    setSubmittedFilters(filters)
  }

  const linkResume = async () => {
    if (!selected || !linkResumeId) return
    try {
      await applicationsApi.linkResume(selected.id, Number(linkResumeId))
      setLinkResumeId('')
      const submissions = await applicationsApi.resumeSubmissions(selected.id)
      setDetailData((current) => ({ ...current, submissions }))
      onNotify?.('Resume linked to this application.', 'success')
    } catch (requestError) { setDetailData((current) => ({ ...current, error: requestError.message })) }
  }

  return (
    <div className="page-stack">
      <PageIntro eyebrow="JOB SEARCH" title="Applications" description="One place for every role you’ve applied to and every update that follows."
        action={<button className="button button-primary" type="button" onClick={openCreate}><Icon name="plus" size={17} />Add application</button>} />
      <Notice>{error}</Notice>

      <section className="panel applications-panel">
        <form className="filter-toolbar" onSubmit={submitSearch}>
          <label className="search-field"><Icon name="search" size={17} /><input aria-label="Search company" placeholder="Search by company…" value={filters.company} onChange={(event) => setFilters((current) => ({ ...current, company: event.target.value }))} /></label>
          <label className="select-filter"><Icon name="filter" size={17} /><select aria-label="Filter by status" value={filters.status} onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}><option value="">All statuses</option>{statuses.map((status) => <option key={status}>{status}</option>)}</select></label>
          <button className="button button-secondary button-small" type="submit">Apply filters</button>
          {(submittedFilters.company || submittedFilters.status) && <button className="text-button clear-filter" type="button" onClick={() => { setFilters({ company: '', status: '' }); setSubmittedFilters({ company: '', status: '' }); setPage(0) }}>Clear</button>}
          <span className="filter-total">{result.totalElements} {result.totalElements === 1 ? 'application' : 'applications'}</span>
        </form>

        {loading ? <LoadingState label="Loading your applications…" /> : error && result.content.length === 0 ? <FailureState title="Applications couldn’t load" description={error} onRetry={load} /> : result.content.length ? <>
          <div className="table-scroll">
            <table className="data-table applications-table">
              <thead><tr><th>Company / role</th><th>Applied on</th><th>Interview date</th><th>Status</th><th>Salary</th><th aria-label="Actions" /></tr></thead>
              <tbody>{result.content.map((application) => <tr key={application.id}>
                <td><button className="table-primary-link" type="button" onClick={() => setSelected(application)}><strong>{application.companyName}</strong><small>{application.jobRole}</small></button></td>
                <td>{formatDate(application.appliedDate)}</td>
                <td><span className={application.interviewDate ? 'date-value' : 'muted-inline'}>{formatDate(application.interviewDate)}</span></td>
                <td><select className={`status-select status-select-${String(application.status).toLowerCase()}`} value={application.status} aria-label={`Change ${application.companyName} status`} onChange={(event) => changeStatus(application, event.target.value)}>{statuses.map((status) => <option key={status}>{status}</option>)}</select></td>
                <td>{formatMoney(application.salaryLpa)} <span className="muted-inline">LPA</span></td>
                <td><div className="row-actions">
                  <button className="icon-button" type="button" title="Prepare for interview" aria-label={`Prepare for interview at ${application.companyName}`} onClick={() => onOpenChat?.({ purpose: 'INTERVIEW_PREPARATION', applicationId: application.id })}><Icon name="sparkle" size={16} /></button>
                  <button className="icon-button" type="button" title="Edit application" aria-label={`Edit ${application.companyName}`} onClick={() => openEdit(application)}><Icon name="edit" size={16} /></button>
                  <button className="icon-button icon-danger" type="button" title="Delete application" aria-label={`Delete ${application.companyName}`} onClick={() => removeApplication(application)}><Icon name="trash" size={16} /></button>
                </div></td>
              </tr>)}</tbody>
            </table>
          </div>
          <div className="pagination-bar"><span>Page {result.totalPages ? page + 1 : 0} of {result.totalPages || 0}</span><div><button className="button button-secondary button-small" disabled={page <= 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>Previous</button><button className="button button-secondary button-small" disabled={!result.totalPages || page + 1 >= result.totalPages} onClick={() => setPage((current) => current + 1)}>Next</button></div></div>
        </> : <EmptyState icon="briefcase" title="No applications found" description={submittedFilters.company || submittedFilters.status ? 'Try a different search or clear the filters.' : 'Add a role when you apply. We’ll help you keep the follow-up details straight.'} action={!submittedFilters.company && !submittedFilters.status && <button className="button button-primary button-small" onClick={openCreate}><Icon name="plus" size={16} />Add your first application</button>} />}
      </section>

      {showForm && <ApplicationForm application={editing} onClose={() => { setShowForm(false); setEditing(null) }} onSave={saveApplication} />}
      {selected && <ApplicationDetails application={selected} detailData={detailData} resumes={resumes} linkResumeId={linkResumeId} setLinkResumeId={setLinkResumeId} onLinkResume={linkResume} onClose={() => setSelected(null)} onEdit={() => { setSelected(null); openEdit(selected) }} />}
    </div>
  )
}

function ApplicationForm({ application, onClose, onSave }) {
  const [form, setForm] = useState(application ? {
    companyName: application.companyName, jobRole: application.jobRole, salaryLpa: application.salaryLpa,
    location: application.location, status: application.status, appliedDate: application.appliedDate,
    interviewDate: application.interviewDate || '',
  } : blankApplication())
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const setField = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setError('')
    try { await onSave(form) } catch (requestError) { setError(requestError.message || 'Could not save the application.') } finally { setBusy(false) }
  }
  return <Modal title={application ? 'Edit application' : 'Add an application'} subtitle="Add the details you’ll want to remember later." onClose={onClose}>
    <form className="form-grid" onSubmit={submit}>
      <Notice>{error}</Notice>
      <label className="field field-span-2">Company name<input name="companyName" value={form.companyName} onChange={setField} maxLength={120} required placeholder="e.g. Northstar Labs" /></label>
      <label className="field">Job role<input name="jobRole" value={form.jobRole} onChange={setField} maxLength={120} required placeholder="e.g. Software Engineer" /></label>
      <label className="field">Location<input name="location" value={form.location} onChange={setField} maxLength={160} required placeholder="e.g. Bengaluru / Remote" /></label>
      <label className="field">Salary (LPA)<input name="salaryLpa" type="number" min="0.01" step="0.01" value={form.salaryLpa} onChange={setField} required placeholder="8.5" /></label>
      <label className="field">Current status<select name="status" value={form.status} onChange={setField}>{statuses.map((status) => <option key={status}>{status}</option>)}</select></label>
      <label className="field">Applied date<input name="appliedDate" type="date" value={form.appliedDate} onChange={setField} required /></label>
      <label className="field">Interview date <span className="field-optional">optional</span><input name="interviewDate" type="date" value={form.interviewDate} onChange={setField} /></label>
      <div className="modal-actions field-span-2"><button className="button button-secondary" type="button" onClick={onClose}>Cancel</button><button className="button button-primary" disabled={busy}>{busy ? 'Saving…' : application ? 'Save changes' : 'Add application'}</button></div>
    </form>
  </Modal>
}

function ApplicationDetails({ application, detailData, resumes, linkResumeId, setLinkResumeId, onLinkResume, onClose, onEdit }) {
  const linkedIds = new Set(detailData.submissions.map((submission) => submission.resumeVersion?.id))
  const linkable = resumes.filter((resume) => !linkedIds.has(resume.id))
  return <Modal title={application.companyName} subtitle={application.jobRole} onClose={onClose} size="large">
    <div className="application-detail-grid">
      <div className="detail-overview">
        <div className="detail-facts"><div><small>Status</small><StatusBadge status={application.status} /></div><div><small>Location</small><strong>{application.location}</strong></div><div><small>Salary</small><strong>{formatMoney(application.salaryLpa)} LPA</strong></div><div><small>Applied</small><strong>{formatDate(application.appliedDate)}</strong></div><div><small>Interview</small><strong>{formatDate(application.interviewDate)}</strong></div></div>
        <button className="button button-secondary button-small" type="button" onClick={onEdit}><Icon name="edit" size={15} />Edit application</button>
        <div className="detail-section"><h3>Resume versions sent</h3>{detailData.loading ? <LoadingState label="Loading resumes…" /> : detailData.submissions.length ? <div className="linked-resume-list">{detailData.submissions.map((submission) => <div className="linked-resume" key={submission.id}><span className="file-type-icon"><Icon name="file" size={17} /></span><span><strong>{submission.resumeVersion?.originalFilename || 'Resume file'}</strong><small>Linked {relativeDate(submission.submittedAt)}</small></span></div>)}</div> : <p className="subtle-text">No resume version is linked to this application yet.</p>}
          {linkable.length > 0 && <div className="inline-link-form"><select aria-label="Choose a resume to link" value={linkResumeId} onChange={(event) => setLinkResumeId(event.target.value)}><option value="">Choose a saved resume…</option>{linkable.map((resume) => <option value={resume.id} key={resume.id}>{resume.originalFilename}</option>)}</select><button className="button button-secondary button-small" disabled={!linkResumeId} onClick={onLinkResume}>Link resume</button></div>}
        </div>
      </div>
      <div className="detail-timeline"><h3>Status history</h3>{detailData.loading ? <LoadingState label="Loading history…" /> : detailData.error ? <Notice>{detailData.error}</Notice> : detailData.history.length ? <div className="timeline-list">{detailData.history.map((entry) => <div className="timeline-entry" key={entry.id}><span className="timeline-dot" /><div><strong>{entry.previousStatus || 'Application created'} <span>→</span> {entry.newStatus}</strong><small>{relativeDate(entry.changedAt)}</small></div></div>)}</div> : <EmptyState icon="clock" title="No status changes yet" description="Changes to this application will be recorded here." />}</div>
    </div>
  </Modal>
}
