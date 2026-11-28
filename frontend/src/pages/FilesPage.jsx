import { useCallback, useEffect, useRef, useState } from 'react'
import { applicationsApi, resumesApi } from '../api/resources.js'
import Icon from '../components/Icon.jsx'
import Modal from '../components/Modal.jsx'
import { EmptyState, FailureState, LoadingState, Notice, PageIntro, relativeDate } from '../components/UI.jsx'

const intents = [
  { id: 'analyze', title: 'Analyze', description: 'Save this resume and start a focused conversation with your assistant.', icon: 'sparkle' },
  { id: 'save', title: 'Save', description: 'Save it and add the company, role, and interview details to your tracker.', icon: 'file' },
  { id: 'both', title: 'Both', description: 'Track the job first, then open a resume analysis chat linked to it.', icon: 'briefcase' },
]

const localToday = () => {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export default function FilesPage({ onStartChat, onNotify }) {
  const [resumes, setResumes] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [uploadOpen, setUploadOpen] = useState(false)
  const [trackResume, setTrackResume] = useState(null)
  const [trackIntent, setTrackIntent] = useState('save')
  const [nextStepResume, setNextStepResume] = useState(null)
  const [refreshKey, setRefreshKey] = useState(0)
  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setResumes(await resumesApi.list()) }
    catch (requestError) { setError(requestError.message || 'Could not load your resume files.') }
    finally { setLoading(false) }
  }, [])
  useEffect(() => { load() }, [load, refreshKey])

  const startAnalysis = (resume, applicationId = null) => {
    onStartChat?.({
      purpose: 'RESUME_ANALYSIS', resumeVersionId: resume.id, jobApplicationId: applicationId,
      title: `Resume analysis · ${resume.originalFilename}`,
      initialPrompt: 'Please analyze this resume. Summarize the strongest qualifications, identify skills and experience you can verify from the document, point out areas I could improve, and ask me what I would like to work on next.',
    })
  }

  const download = async (resume) => {
    try {
      const blob = await resumesApi.download(resume.id)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = resume.originalFilename
      document.body.append(link); link.click(); link.remove()
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (requestError) { setError(requestError.message || 'Could not download this resume.') }
  }

  const remove = async (resume) => {
    if (!window.confirm(`Delete “${resume.originalFilename}”? Its file contents will be removed. A deletion event will remain in History.`)) return
    try { await resumesApi.delete(resume.id); setRefreshKey((value) => value + 1); onNotify?.('Resume deleted. The deletion is recorded in History.', 'success') }
    catch (requestError) { setError(requestError.message || 'Could not delete this resume.') }
  }

  const handleUploadComplete = async (resume, intent) => {
    setUploadOpen(false)
    setRefreshKey((value) => value + 1)
    onNotify?.('Resume safely saved in Files.', 'success')
    if (intent === 'analyze') startAnalysis(resume)
    else { setTrackResume(resume); setTrackIntent(intent) }
  }

  const saveJobForResume = async (form) => {
    try {
      const application = await applicationsApi.createFromResume(trackResume.id, {
        ...form,
        salaryLpa: Number(form.salaryLpa),
        interviewDate: form.interviewDate || null,
      })
      const resume = trackResume
      const intent = trackIntent
      setTrackResume(null)
      setRefreshKey((value) => value + 1)
      onNotify?.(`Application for ${application.companyName} saved with this resume.`, 'success')
      if (intent === 'both') startAnalysis(resume, application.id)
      else setNextStepResume(resume)
    } catch (requestError) { throw requestError }
  }

  const originals = resumes.filter((resume) => !resume.parentVersionId)
  const generatedCount = resumes.filter((resume) => resume.source === 'AI_GENERATED').length

  return (
    <div className="page-stack">
      <PageIntro eyebrow="YOUR DOCUMENTS" title="Resume files" description="Original uploads and improved versions, safely stored in your private workspace."
        action={<button className="button button-primary" type="button" onClick={() => setUploadOpen(true)}><Icon name="upload" size={17} />Upload resume</button>} />
      <Notice>{error}</Notice>
      <section className="file-summary-row">
        <div className="file-summary-card"><span className="stat-icon stat-blue"><Icon name="file" size={18} /></span><div><small>Saved resumes</small><strong>{resumes.length}</strong></div></div>
        <div className="file-summary-card"><span className="stat-icon stat-violet"><Icon name="sparkle" size={18} /></span><div><small>AI-generated versions</small><strong>{generatedCount}</strong></div></div>
        <div className="file-summary-hint"><Icon name="check" size={17} /><span>Your files stay in your account. Deleting a resume removes its stored file contents and leaves a History entry.</span></div>
      </section>

      <section className="panel files-panel">
        <div className="panel-heading"><div><h2>All resume versions</h2><p>{resumes.length} {resumes.length === 1 ? 'file' : 'files'} in your workspace</p></div><button className="text-button" onClick={load} type="button"><Icon name="refresh" size={15} />Refresh files</button></div>
        {loading ? <LoadingState label="Loading your files…" /> : error && resumes.length === 0 ? <FailureState title="Your resume files couldn’t load" description={error} onRetry={load} /> : resumes.length ? <div className="resume-list">
          {resumes.map((resume) => <ResumeRow key={resume.id} resume={resume} onDownload={() => download(resume)} onDelete={() => remove(resume)} onAnalyze={() => startAnalysis(resume)} onTrack={() => { setTrackResume(resume); setTrackIntent('save') }} />)}
        </div> : <EmptyState icon="file" title="Your resume library is ready" description="Upload a PDF or DOCX file. You can save it, link it to a job application, or ask the assistant to review it." action={<button className="button button-primary button-small" onClick={() => setUploadOpen(true)}><Icon name="upload" size={16} />Upload your first resume</button>} />}
      </section>

      <section className="file-note-card"><span className="note-spark"><Icon name="sparkle" size={18} /></span><div><strong>Improving a resume keeps its history together</strong><p>Your original upload stays available while you work on a new version. At the end, you can keep the original, the new version, or both. If you don’t choose, both remain saved.</p></div></section>

      {uploadOpen && <UploadResumeModal onClose={() => setUploadOpen(false)} onUploaded={handleUploadComplete} />}
      {trackResume && <ResumeApplicationForm resume={trackResume} onClose={() => setTrackResume(null)} onSave={saveJobForResume} />}
      {nextStepResume && <Modal title="Your resume and job are saved" subtitle="The resume is still available in Files. What would you like to do now?" onClose={() => setNextStepResume(null)}>
        <div className="after-save-card"><span className="after-save-icon"><Icon name="check" size={19} /></span><div><strong>{nextStepResume.originalFilename}</strong><small>Saved to Files and linked to your tracked application.</small></div></div>
        <div className="after-save-actions"><button className="button button-primary" type="button" onClick={() => { const resume = nextStepResume; setNextStepResume(null); startAnalysis(resume) }}><Icon name="message" size={16} />Chat with this resume</button><button className="button button-secondary" type="button" onClick={() => { setNextStepResume(null); onStartChat?.({ purpose: 'CAREER_DEVELOPMENT', title: 'Career development' }) }}>Finish and open a new chat<Icon name="arrow" size={15} /></button></div>
      </Modal>}
    </div>
  )
}

function ResumeRow({ resume, onDownload, onDelete, onAnalyze, onTrack }) {
  const isGenerated = resume.source === 'AI_GENERATED'
  const format = resume.originalFilename.toLowerCase().endsWith('.pdf') ? 'PDF' : 'DOCX'
  const sizeMb = Math.max(0.01, resume.fileSize / (1024 * 1024)).toFixed(2)
  return <article className="resume-row">
    <span className={`resume-file-mark ${format === 'PDF' ? 'resume-pdf' : 'resume-docx'}`}><Icon name="file" size={20} /><small>{format}</small></span>
    <div className="resume-main"><div className="resume-name-line"><strong title={resume.originalFilename}>{resume.originalFilename}</strong><span className={`source-pill ${isGenerated ? 'source-ai' : ''}`}>{isGenerated ? 'AI version' : 'Original upload'}</span></div>
      <div className="resume-meta"><span>{sizeMb} MB</span><i />{resume.parentVersionId && <><span>Improved from an earlier version</span><i /></>}<span>Added {relativeDate(resume.createdAt)}</span></div>
    </div>
    <div className="resume-actions"><button className="button button-secondary button-small" type="button" onClick={onAnalyze}><Icon name="sparkle" size={15} />Analyze</button><button className="icon-button" type="button" title="Download resume" aria-label={`Download ${resume.originalFilename}`} onClick={onDownload}><Icon name="download" size={17} /></button><button className="icon-button icon-danger" type="button" title="Delete resume" aria-label={`Delete ${resume.originalFilename}`} onClick={onDelete}><Icon name="trash" size={17} /></button><button className="icon-button" type="button" title="Track a job application with this resume" aria-label={`Create job application for ${resume.originalFilename}`} onClick={onTrack}><Icon name="briefcase" size={17} /></button></div>
  </article>
}

function UploadResumeModal({ onClose, onUploaded }) {
  const [file, setFile] = useState(null)
  const [intent, setIntent] = useState('analyze')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const inputRef = useRef(null)
  const chooseFile = (next) => {
    setError('')
    if (!next) return
    if (!/\.(pdf|docx)$/i.test(next.name)) { setFile(null); setError('Choose a PDF or DOCX resume.'); return }
    if (next.size > 10 * 1024 * 1024) { setFile(null); setError('Resume files must be 10 MB or smaller.'); return }
    setFile(next)
  }
  const upload = async (event) => {
    event.preventDefault()
    if (!file) { setError('Choose the resume file you want to upload.'); return }
    setBusy(true); setError('')
    try {
      const saved = await resumesApi.upload(file)
      await onUploaded(saved, intent)
    } catch (requestError) { setError(requestError.message || 'Upload failed. Check that the backend is running.') }
    finally { setBusy(false) }
  }
  return <Modal title="Add a resume" subtitle="Choose what you’d like to do after it’s saved." onClose={onClose} size="large">
    <form onSubmit={upload}>
      <Notice>{error}</Notice>
      <div className="upload-dropzone" onClick={() => inputRef.current?.click()} onDragOver={(event) => event.preventDefault()} onDrop={(event) => { event.preventDefault(); chooseFile(event.dataTransfer.files?.[0]) }} role="button" tabIndex={0} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') inputRef.current?.click() }}>
        <input ref={inputRef} type="file" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" hidden onChange={(event) => chooseFile(event.target.files?.[0])} />
        <span className="upload-icon"><Icon name="upload" size={21} /></span>
        <strong>{file ? file.name : 'Drop your resume here or browse files'}</strong>
        <small>{file ? `${(file.size / 1024 / 1024).toFixed(2)} MB · PDF or DOCX` : 'PDF or DOCX · up to 10 MB'}</small>
        {file && <button className="text-button" type="button" onClick={(event) => { event.stopPropagation(); setFile(null); if (inputRef.current) inputRef.current.value = '' }}>Choose another file</button>}
      </div>
      <p className="field-label upload-choice-label">What would you like to do?</p>
      <div className="upload-intents">{intents.map((option) => <button className={`intent-option ${intent === option.id ? 'intent-selected' : ''}`} type="button" key={option.id} onClick={() => setIntent(option.id)} aria-pressed={intent === option.id}>
        <span className="intent-icon"><Icon name={option.icon} size={18} /></span><span><strong>{option.title}</strong><small>{option.description}</small></span><span className="radio-indicator" />
      </button>)}</div>
      <div className="modal-actions"><button className="button button-secondary" type="button" onClick={onClose}>Cancel</button><button className="button button-primary" disabled={busy || !file}><Icon name="upload" size={16} />{busy ? 'Uploading…' : 'Save resume and continue'}</button></div>
    </form>
  </Modal>
}

function ResumeApplicationForm({ resume, onClose, onSave }) {
  const [form, setForm] = useState({ companyName: '', jobRole: '', salaryLpa: '', location: '', status: 'Applied', appliedDate: localToday(), interviewDate: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const update = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  const submit = async (event) => { event.preventDefault(); setBusy(true); setError(''); try { await onSave(form) } catch (requestError) { setError(requestError.message || 'Could not save the job application.') } finally { setBusy(false) } }
  return <Modal title="Add job details" subtitle={`Your resume “${resume.originalFilename}” is already saved in Files.`} onClose={onClose}>
    <form className="form-grid" onSubmit={submit}>
      <Notice>{error}</Notice>
      <label className="field field-span-2">Company name<input name="companyName" value={form.companyName} onChange={update} maxLength={120} required placeholder="e.g. Northstar Labs" /></label>
      <label className="field">Job role<input name="jobRole" value={form.jobRole} onChange={update} maxLength={120} required placeholder="e.g. Software Engineer" /></label>
      <label className="field">Location<input name="location" value={form.location} onChange={update} maxLength={160} required placeholder="e.g. Bengaluru / Remote" /></label>
      <label className="field">Salary (LPA)<input name="salaryLpa" type="number" min="0.01" step="0.01" value={form.salaryLpa} onChange={update} required placeholder="8.5" /></label>
      <label className="field">Status<select name="status" value={form.status} onChange={update}><option>Applied</option><option>Interview</option><option>Selected</option><option>Rejected</option><option>Withdrawn</option></select></label>
      <label className="field">Applied date<input name="appliedDate" type="date" value={form.appliedDate} onChange={update} required /></label>
      <label className="field">Interview date <span className="field-optional">optional</span><input name="interviewDate" type="date" value={form.interviewDate} onChange={update} /></label>
      <div className="modal-actions field-span-2"><button className="button button-secondary" type="button" onClick={onClose}>Later</button><button className="button button-primary" disabled={busy}>{busy ? 'Saving…' : 'Save job details'}</button></div>
    </form>
  </Modal>
}
