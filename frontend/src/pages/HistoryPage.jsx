import { useCallback, useEffect, useState } from 'react'
import { chatsApi, historyApi } from '../api/resources.js'
import Icon from '../components/Icon.jsx'
import { EmptyState, FailureState, LoadingState, Notice, PageIntro, relativeDate } from '../components/UI.jsx'

export default function HistoryPage({ onOpenChat }) {
  const [summary, setSummary] = useState(null)
  const [events, setEvents] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 })
  const [conversations, setConversations] = useState([])
  const [tab, setTab] = useState('activity')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const [nextSummary, nextEvents, nextChats] = await Promise.all([
        historyApi.summary(), historyApi.events(page, 20), chatsApi.list(),
      ])
      setSummary(nextSummary); setEvents(nextEvents); setConversations(nextChats)
    } catch (requestError) { setError(requestError.message || 'Could not load your History.') }
    finally { setLoading(false) }
  }, [page])
  useEffect(() => { load() }, [load])

  const historyCards = [
    { label: 'Resume uploads', value: summary?.resumeUploads || 0, icon: 'upload', tone: 'blue' },
    { label: 'AI resume versions', value: summary?.aiGeneratedResumeVersions || 0, icon: 'sparkle', tone: 'violet' },
    { label: 'Resumes deleted', value: summary?.resumeDeletions || 0, icon: 'trash', tone: 'rose' },
    { label: 'Applications added', value: summary?.applicationCreations || 0, icon: 'briefcase', tone: 'green' },
    { label: 'Applications deleted', value: summary?.applicationDeletions || 0, icon: 'close', tone: 'amber' },
    { label: 'AI chats started', value: summary?.aiChatsStarted || 0, icon: 'message', tone: 'blue' },
  ]

  return (
    <div className="page-stack">
      <PageIntro eyebrow="YOUR ACTIVITY" title="History" description="Uploads, changes, conversations, and deletions. Your activity stays connected to your account." action={<button className="button button-secondary button-small" type="button" onClick={load}><Icon name="refresh" size={15} />Refresh</button>} />
      <Notice>{error}</Notice>
      <section className="history-summary-grid">{historyCards.map((card) => <div className="history-summary-card" key={card.label}><span className={`stat-icon stat-${card.tone}`}><Icon name={card.icon} size={17} /></span><small>{card.label}</small><strong>{card.value}</strong></div>)}</section>

      <section className="panel history-panel">
        <div className="panel-heading history-panel-heading"><div><h2>Your record</h2><p>See what happened or return to a saved conversation.</p></div>
          <div className="segmented-tabs" role="tablist" aria-label="History views"><button role="tab" aria-selected={tab === 'activity'} className={tab === 'activity' ? 'segment-active' : ''} onClick={() => setTab('activity')} type="button">Activity</button><button role="tab" aria-selected={tab === 'chats'} className={tab === 'chats' ? 'segment-active' : ''} onClick={() => setTab('chats')} type="button">Conversations <span>{conversations.length}</span></button></div>
        </div>
        {loading ? <LoadingState label="Loading your activity…" /> : error && !summary ? <FailureState title="Your History couldn’t load" description={error} onRetry={load} /> : tab === 'activity' ? <>
          {events.content.length ? <div className="activity-timeline">{events.content.map((event) => {
            const chatRelated = event.relatedEntityType === 'AI_CHAT' && !event.eventType.includes('DELETED')
            const linkedChat = chatRelated && conversations.find((conversation) => conversation.id === event.relatedEntityId)
            const presentation = eventPresentation(event.eventType)
            return <article className="activity-event" key={event.id}>
              <span className={`event-icon event-${presentation.tone}`}><Icon name={presentation.icon} size={17} /></span>
              <div className="event-copy"><div><strong>{event.summary}</strong><small>{event.eventType.replaceAll('_', ' ').toLowerCase()}</small></div><time>{relativeDate(event.createdAt)}</time></div>
              {linkedChat && <button className="text-button event-open-chat" type="button" onClick={() => onOpenChat?.({ chatId: linkedChat.id })}>Open chat <Icon name="arrow" size={14} /></button>}
            </article>
          })}</div> : <EmptyState icon="clock" title="Your activity will show up here" description="As you save resumes, manage applications, and chat with your assistant, the timeline will grow." />}
          {events.totalPages > 1 && <div className="pagination-bar"><span>{events.totalElements} activity items · Page {page + 1} of {events.totalPages}</span><div><button className="button button-secondary button-small" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>Previous</button><button className="button button-secondary button-small" disabled={page + 1 >= events.totalPages} onClick={() => setPage((current) => current + 1)}>Next</button></div></div>}
        </> : conversations.length ? <div className="saved-chat-list">{conversations.map((conversation) => <button className="saved-chat-row" type="button" key={conversation.id} onClick={() => onOpenChat?.({ chatId: conversation.id })}><span className="saved-chat-icon"><Icon name={conversation.purpose?.includes('RESUME') ? 'file' : conversation.purpose === 'INTERVIEW_PREPARATION' ? 'calendar' : 'message'} size={18} /></span><span className="saved-chat-copy"><strong>{conversation.title}</strong><small>{conversation.purpose?.replaceAll('_', ' ').toLowerCase()} · Updated {relativeDate(conversation.updatedAt)}</small></span><Icon name="chevron" size={17} /></button>)}</div> : <EmptyState icon="message" title="No saved conversations yet" description="Chats with your assistant will be listed here so you can return to them." />}
      </section>
      <div className="history-privacy-note"><Icon name="check" size={16} /><span>Deleting a resume removes its stored file contents. The activity record stays here, and its chat is kept unless you delete that conversation separately.</span></div>
    </div>
  )
}

function eventPresentation(type = '') {
  if (type.includes('RESUME')) return { icon: type.includes('DELETED') ? 'trash' : 'file', tone: type.includes('DELETED') ? 'rose' : 'violet' }
  if (type.includes('APPLICATION')) return { icon: type.includes('DELETED') ? 'trash' : 'briefcase', tone: type.includes('DELETED') ? 'rose' : 'blue' }
  if (type.includes('AI_CHAT')) return { icon: type.includes('DELETED') ? 'trash' : 'message', tone: type.includes('DELETED') ? 'rose' : 'green' }
  return { icon: 'clock', tone: 'blue' }
}
