import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { applicationsApi, chatsApi, resumesApi } from '../api/resources.js'
import Icon from '../components/Icon.jsx'
import Modal from '../components/Modal.jsx'
import { EmptyState, LoadingState, Notice, PageIntro, relativeDate } from '../components/UI.jsx'

const purposeOptions = [
  { id: 'CAREER_DEVELOPMENT', label: 'Career development', description: 'Talk through job roles, skills, and next steps.', icon: 'sparkle' },
  { id: 'RESUME_ANALYSIS', label: 'Resume analysis', description: 'Understand your strengths and opportunities from a saved resume.', icon: 'file' },
  { id: 'RESUME_IMPROVEMENT', label: 'Resume improvement', description: 'Work on a more focused version of your resume.', icon: 'edit' },
  { id: 'INTERVIEW_PREPARATION', label: 'Interview preparation', description: 'Practice for an interview linked to an application.', icon: 'calendar' },
]

export default function AssistantPage({ activeChatId, onActiveChatChange, launchChat, onLaunchConsumed, onNotify }) {
  const [conversations, setConversations] = useState([])
  const [messages, setMessages] = useState([])
  const [resumes, setResumes] = useState([])
  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(true)
  const [messagesLoading, setMessagesLoading] = useState(false)
  const [sending, setSending] = useState(false)
  const [draft, setDraft] = useState('')
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [newChatOpen, setNewChatOpen] = useState(false)
  const [revisionOpen, setRevisionOpen] = useState(false)
  const [revisionInstructions, setRevisionInstructions] = useState('')
  const [revisionBusy, setRevisionBusy] = useState(false)
  const [pendingRetention, setPendingRetention] = useState(null)
  const [chatBusy, setChatBusy] = useState(false)
  const bottomRef = useRef(null)
  const handledLaunchRef = useRef(null)
  const skipNextMessagesLoad = useRef(null)

  const loadWorkspace = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const [chatList, resumeList, applicationPage] = await Promise.all([
        chatsApi.list(), resumesApi.list(), applicationsApi.list({ page: 0, size: 100 }),
      ])
      setConversations(chatList)
      setResumes(resumeList)
      setApplications(applicationPage.content || [])
    } catch (requestError) {
      setError(requestError.message || 'Could not connect to the AI assistant.')
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { loadWorkspace() }, [loadWorkspace])

  useEffect(() => {
    if (loading || launchChat || activeChatId || !conversations.length || handledLaunchRef.current) return
    onActiveChatChange(conversations[0].id)
  }, [loading, launchChat, conversations, activeChatId, onActiveChatChange])

  useEffect(() => {
    if (!activeChatId) { setMessages([]); setPendingRetention(null); return }
    if (skipNextMessagesLoad.current === activeChatId) {
      skipNextMessagesLoad.current = null
      return
    }
    let cancelled = false
    setMessagesLoading(true); setError(''); setPendingRetention(null)
    chatsApi.messages(activeChatId)
      .then((items) => { if (!cancelled) setMessages(items) })
      .catch((requestError) => { if (!cancelled) setError(requestError.message || 'Could not load this conversation.') })
      .finally(() => { if (!cancelled) setMessagesLoading(false) })
    return () => { cancelled = true }
  }, [activeChatId])

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' }) }, [messages])

  const activeConversation = conversations.find((conversation) => String(conversation.id) === String(activeChatId))
  const linkedResume = resumes.find((resume) => resume.id === activeConversation?.resumeVersionId)
  const linkedApplication = applications.find((application) => application.id === activeConversation?.jobApplicationId)

  const sendToConversation = useCallback(async (conversationId, text, resetCurrent = true) => {
    const messageText = text.trim()
    if (!messageText || sending) return
    const userMessage = { id: `local-user-${Date.now()}`, role: 'USER', content: messageText, createdAt: new Date().toISOString() }
    const assistantId = `local-assistant-${Date.now()}`
    if (resetCurrent) {
      setDraft('')
      setError('')
      setMessages((current) => [...current, userMessage, { id: assistantId, role: 'ASSISTANT', content: '', createdAt: new Date().toISOString(), streaming: true }])
    }
    setSending(true)
    try {
      await chatsApi.stream(conversationId, messageText, (_token, fullText) => {
        if (resetCurrent) setMessages((current) => current.map((item) => item.id === assistantId ? { ...item, content: fullText } : item))
      })
      const [updatedMessages, updatedChats] = await Promise.all([chatsApi.messages(conversationId), chatsApi.list()])
      if (resetCurrent) setMessages(updatedMessages)
      setConversations(updatedChats)
    } catch (requestError) {
      if (resetCurrent) {
        setMessages((current) => current.filter((item) => item.id !== assistantId))
        try { setMessages(await chatsApi.messages(conversationId)) } catch { /* Keep the visible conversation when the backend is unavailable. */ }
      }
      setError(requestError.message || 'The assistant could not complete this response. Check that Ollama is running.')
    } finally { setSending(false) }
  }, [sending])

  const startConversation = useCallback(async (details) => {
    setChatBusy(true); setError(''); setInfo('')
    try {
      const conversation = await chatsApi.create({
        purpose: details.purpose,
        title: details.title?.trim() || null,
        resumeVersionId: details.resumeVersionId ? Number(details.resumeVersionId) : null,
        jobApplicationId: details.jobApplicationId ? Number(details.jobApplicationId) : null,
      })
      setConversations((current) => [conversation, ...current.filter((item) => item.id !== conversation.id)])
      setMessages([])
      setNewChatOpen(false)
      skipNextMessagesLoad.current = details.initialPrompt ? conversation.id : null
      onActiveChatChange(conversation.id)
      if (details.initialPrompt) {
        await sendToConversation(conversation.id, details.initialPrompt, true)
      }
      return conversation
    } catch (requestError) {
      setError(requestError.message || 'Could not start a new conversation.')
      throw requestError
    } finally { setChatBusy(false) }
  }, [onActiveChatChange, sendToConversation])

  useEffect(() => {
    if (!launchChat || !launchChat.id || handledLaunchRef.current === launchChat.id || loading) return
    handledLaunchRef.current = launchChat.id
    onLaunchConsumed?.(launchChat.id)
    startConversation(launchChat).catch(() => {})
  }, [launchChat, loading, onLaunchConsumed, startConversation])

  const submitMessage = (event) => {
    event.preventDefault()
    if (activeChatId) sendToConversation(activeChatId, draft)
  }

  const deleteConversation = async () => {
    if (!activeConversation || !window.confirm(`Delete “${activeConversation.title}” and its saved messages?`)) return
    try {
      await chatsApi.delete(activeConversation.id)
      const remaining = conversations.filter((conversation) => conversation.id !== activeConversation.id)
      setConversations(remaining); setMessages([]); setPendingRetention(null)
      onActiveChatChange(remaining[0]?.id || null)
      onNotify?.('Conversation deleted. The deletion is recorded in History.', 'success')
    } catch (requestError) { setError(requestError.message || 'Could not delete the conversation.') }
  }

  const generateRevision = async (event) => {
    event.preventDefault()
    if (!activeConversation) return
    setRevisionBusy(true); setError('')
    try {
      const response = await chatsApi.reviseResume(activeConversation.id, revisionInstructions.trim() || null)
      setPendingRetention({ version: response.generatedResume, question: response.question, choices: response.choices })
      setRevisionOpen(false); setRevisionInstructions('')
      setMessages(await chatsApi.messages(activeConversation.id))
      await loadWorkspace()
    } catch (requestError) { setError(requestError.message || 'Could not create a revised resume.') }
    finally { setRevisionBusy(false) }
  }

  const chooseRetention = async (keep) => {
    if (!activeConversation || !pendingRetention) return
    setChatBusy(true); setError('')
    try {
      await chatsApi.keepResume(activeConversation.id, pendingRetention.version.id, keep)
      setPendingRetention(null)
      await loadWorkspace()
      onNotify?.(keep === 'BOTH' ? 'Both resume versions are saved.' : `The ${keep === 'ORIGINAL' ? 'original' : 'new'} resume is kept.`, 'success')
    } catch (requestError) { setError(requestError.message || 'Could not save your resume choice.') }
    finally { setChatBusy(false) }
  }

  if (loading) return <div className="page-stack"><PageIntro eyebrow="YOUR ASSISTANT" title="AI career assistant" description="A focused space for your job search conversations." /><LoadingState label="Connecting to your assistant…" /></div>

  return (
    <div className="page-stack assistant-page">
      <PageIntro eyebrow="YOUR CAREER COMPANION" title="AI career assistant" description="A private conversation for resumes, interviews, and your next career move."
        action={<button className="button button-primary" type="button" onClick={() => setNewChatOpen(true)}><Icon name="plus" size={17} />New conversation</button>} />
      <Notice>{error}</Notice>
      <section className="assistant-workspace">
        <aside className="conversation-sidebar">
          <div className="conversation-sidebar-head"><div><span>YOUR CHATS</span><strong>{conversations.length}</strong></div><button className="icon-button" type="button" onClick={() => setNewChatOpen(true)} aria-label="Start a conversation"><Icon name="plus" size={17} /></button></div>
          {conversations.length ? <div className="conversation-list">{conversations.map((conversation) => <button key={conversation.id} type="button" className={`conversation-item ${String(activeChatId) === String(conversation.id) ? 'conversation-active' : ''}`} onClick={() => onActiveChatChange(conversation.id)}>
            <span className="conversation-icon"><Icon name={conversation.purpose?.includes('RESUME') ? 'file' : conversation.purpose === 'INTERVIEW_PREPARATION' ? 'calendar' : 'message'} size={16} /></span>
            <span className="conversation-copy"><strong>{conversation.title}</strong><small>{purposeLabel(conversation.purpose)}</small><small>{relativeDate(conversation.updatedAt)}</small></span>
          </button>)}</div> : <div className="conversation-empty"><p>Your conversations will be saved here.</p><button className="text-button" onClick={() => setNewChatOpen(true)}>Start your first chat <Icon name="arrow" size={14} /></button></div>}
          <div className="conversation-local-note"><span className="local-dot" />Your career data stays in your account</div>
        </aside>

        <section className="chat-window">
          {activeConversation ? <>
            <header className="chat-header">
              <span className="chat-avatar"><Icon name="sparkle" size={19} /></span>
              <div className="chat-header-copy"><strong>{activeConversation.title}</strong><span>{purposeLabel(activeConversation.purpose)}{linkedApplication ? ` · ${linkedApplication.companyName}` : ''}{linkedResume ? ` · ${linkedResume.originalFilename}` : ''}</span></div>
              {linkedResume && <button className="button button-secondary button-small revise-button" type="button" onClick={() => setRevisionOpen(true)}><Icon name="edit" size={15} />Improve resume</button>}
              <button className="icon-button icon-danger" type="button" onClick={deleteConversation} title="Delete conversation" aria-label="Delete conversation"><Icon name="trash" size={17} /></button>
            </header>
            <div className="chat-messages">
              {messagesLoading ? <LoadingState label="Opening conversation…" /> : messages.length ? <>
                {messages.map((message) => <ChatMessage key={message.id} message={message} />)}
                {sending && !messages.some((message) => message.streaming) && <div className="assistant-thinking"><span className="thinking-dots"><i /><i /><i /></span><span>Thinking…</span></div>}
              </> : <div className="chat-welcome"><span className="welcome-sparkle"><Icon name="sparkle" size={25} /></span><h2>What would you like to work on?</h2><p>Your assistant can help you understand a resume, prepare for an interview, or plan your next career move.</p><div className="suggestion-chips">{suggestionsFor(activeConversation.purpose).map((suggestion) => <button type="button" key={suggestion} onClick={() => setDraft(suggestion)}>{suggestion}<Icon name="arrow" size={13} /></button>)}</div></div>}
              {pendingRetention && <div className="retention-card"><span className="retention-icon"><Icon name="file" size={18} /></span><div className="retention-copy"><strong>{pendingRetention.question}</strong><span>Both versions are saved while you decide. If you leave this unanswered, both will stay in Files.</span><div className="retention-actions">{pendingRetention.choices.map((choice) => <button type="button" className={`button ${choice === 'BOTH' ? 'button-primary' : 'button-secondary'} button-small`} key={choice} disabled={chatBusy} onClick={() => chooseRetention(choice)}>{choiceLabel(choice)}</button>)}</div></div></div>}
              <div ref={bottomRef} />
            </div>
            <div className="chat-composer-wrap">
              <form className="chat-composer" onSubmit={submitMessage}>
                <textarea value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="Ask about your resume, a role, or a career next step…" maxLength={12000} rows={2} disabled={sending} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); submitMessage(event) } }} />
                <div className="composer-footer"><span>Focused on your job search · Enter to send · Shift + Enter for a new line</span><button className="send-button" type="submit" disabled={sending || !draft.trim()} aria-label="Send message"><Icon name={sending ? 'refresh' : 'send'} size={17} /></button></div>
              </form>
              <p className="chat-disclaimer">AI can make mistakes. Check important application and resume details before using them.</p>
            </div>
          </> : <div className="chat-no-selection"><EmptyState icon="message" title="Your next conversation starts here" description="Choose a saved chat or start a fresh conversation about your job search." action={<button className="button button-primary button-small" onClick={() => setNewChatOpen(true)}><Icon name="plus" size={16} />Start a conversation</button>} /></div>}
        </section>
      </section>

      {newChatOpen && <NewConversationModal resumes={resumes} applications={applications} busy={chatBusy} onClose={() => setNewChatOpen(false)} onCreate={startConversation} />}
      {revisionOpen && <Modal title="Improve this resume" subtitle="The assistant will use the saved resume and your recent chat as context." onClose={() => setRevisionOpen(false)}>
        <form className="form-grid" onSubmit={generateRevision}><label className="field field-span-2">What should the assistant focus on?<textarea className="form-textarea" rows={5} maxLength={4000} value={revisionInstructions} onChange={(event) => setRevisionInstructions(event.target.value)} placeholder="For example: make the experience section clearer for junior Java developer roles." /></label><p className="field-hint field-span-2">The assistant is instructed to preserve facts from your resume and not invent experience or skills.</p><div className="modal-actions field-span-2"><button className="button button-secondary" type="button" onClick={() => setRevisionOpen(false)}>Cancel</button><button className="button button-primary" disabled={revisionBusy}>{revisionBusy ? 'Creating version…' : 'Create revised resume'}</button></div></form>
      </Modal>}
    </div>
  )
}

function ChatMessage({ message }) {
  const isAssistant = message.role === 'ASSISTANT'
  return <article className={`chat-message ${isAssistant ? 'message-assistant' : 'message-user'}`}>
    {isAssistant && <span className="message-avatar"><Icon name="sparkle" size={15} /></span>}
    <div className="message-content"><div className="message-meta"><strong>{isAssistant ? 'Assistant' : 'You'}</strong><time>{relativeDate(message.createdAt)}</time></div><div className="message-text">{message.content || (message.streaming ? <span className="stream-cursor" /> : '')}</div></div>
  </article>
}

function NewConversationModal({ resumes, applications, busy, onClose, onCreate }) {
  const [purpose, setPurpose] = useState('CAREER_DEVELOPMENT')
  const [resumeVersionId, setResumeVersionId] = useState('')
  const [jobApplicationId, setJobApplicationId] = useState('')
  const [title, setTitle] = useState('')
  const [error, setError] = useState('')
  const requiresResume = purpose === 'RESUME_ANALYSIS' || purpose === 'RESUME_IMPROVEMENT'
  const requiresApplication = purpose === 'INTERVIEW_PREPARATION'
  const submit = async (event) => {
    event.preventDefault(); setError('')
    if (requiresResume && !resumeVersionId) { setError('Choose a saved resume for this conversation.'); return }
    if (requiresApplication && !jobApplicationId) { setError('Choose an application to prepare for.'); return }
    try { await onCreate({ purpose, title, resumeVersionId, jobApplicationId }) } catch (requestError) { setError(requestError.message || 'Could not start the conversation.') }
  }
  return <Modal title="Start a conversation" subtitle="Pick the kind of career help you need today." onClose={onClose} size="large">
    <form onSubmit={submit}>
      <Notice>{error}</Notice>
      <div className="purpose-grid">{purposeOptions.map((option) => <button key={option.id} type="button" className={`purpose-card ${purpose === option.id ? 'purpose-selected' : ''}`} onClick={() => setPurpose(option.id)} aria-pressed={purpose === option.id}><span className="purpose-icon"><Icon name={option.icon} size={18} /></span><strong>{option.label}</strong><small>{option.description}</small></button>)}</div>
      <div className="form-grid new-chat-fields">
        {requiresResume && <label className="field field-span-2">Resume to use<select value={resumeVersionId} onChange={(event) => setResumeVersionId(event.target.value)}><option value="">Choose a saved resume…</option>{resumes.map((resume) => <option value={resume.id} key={resume.id}>{resume.originalFilename}{resume.source === 'AI_GENERATED' ? ' · AI version' : ''}</option>)}</select></label>}
        {requiresApplication && <label className="field field-span-2">Application to prepare for<select value={jobApplicationId} onChange={(event) => setJobApplicationId(event.target.value)}><option value="">Choose an application…</option>{applications.map((application) => <option value={application.id} key={application.id}>{application.companyName} · {application.jobRole}</option>)}</select></label>}
        {!requiresResume && !requiresApplication && <label className="field field-span-2">Add resume context <span className="field-optional">optional</span><select value={resumeVersionId} onChange={(event) => setResumeVersionId(event.target.value)}><option value="">No resume attached</option>{resumes.map((resume) => <option value={resume.id} key={resume.id}>{resume.originalFilename}</option>)}</select></label>}
        <label className="field field-span-2">Conversation name <span className="field-optional">optional</span><input value={title} maxLength={160} onChange={(event) => setTitle(event.target.value)} placeholder="We’ll create a name if you leave this blank" /></label>
      </div>
      <div className="modal-actions"><button type="button" className="button button-secondary" onClick={onClose}>Cancel</button><button className="button button-primary" disabled={busy}>{busy ? 'Starting…' : 'Start conversation'}<Icon name="arrow" size={16} /></button></div>
    </form>
  </Modal>
}

function purposeLabel(purpose) {
  return purposeOptions.find((option) => option.id === purpose)?.label || 'Career conversation'
}

function choiceLabel(choice) {
  return ({ ORIGINAL: 'Keep original', GENERATED: 'Keep new version', BOTH: 'Keep both' })[choice] || choice
}

function suggestionsFor(purpose) {
  if (purpose === 'RESUME_ANALYSIS') return ['Summarize my strongest skills', 'What could I improve?', 'Which roles fit this resume?']
  if (purpose === 'RESUME_IMPROVEMENT') return ['Improve my summary section', 'Make my experience clearer', 'Tailor my resume to a role']
  if (purpose === 'INTERVIEW_PREPARATION') return ['Ask me common interview questions', 'Help me introduce myself', 'What should I ask the interviewer?']
  return ['Help me explore a job role', 'What skills should I develop?', 'Help me plan my next step']
}
