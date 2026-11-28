import { useCallback, useEffect, useState } from 'react'
import { useAuth } from './auth/AuthProvider.jsx'
import AuthScreen from './components/AuthScreen.jsx'
import AppShell from './components/AppShell.jsx'
import Icon from './components/Icon.jsx'
import AssistantPage from './pages/AssistantPage.jsx'
import ApplicationsPage from './pages/ApplicationsPage.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import FilesPage from './pages/FilesPage.jsx'
import HistoryPage from './pages/HistoryPage.jsx'
import SettingsPage from './pages/SettingsPage.jsx'

function initialTheme() {
  try { return localStorage.getItem('aija.theme') === 'dark' ? 'dark' : 'light' }
  catch { return 'light' }
}

export default function App() {
  const { user, loading, signIn, signUp, signOut, updateUser } = useAuth()
  const [theme, setTheme] = useState(initialTheme)
  const [page, setPage] = useState('dashboard')
  const [createTrigger, setCreateTrigger] = useState(0)
  const [activeChatId, setActiveChatId] = useState(null)
  const [launchChat, setLaunchChat] = useState(null)
  const [toast, setToast] = useState(null)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.documentElement.style.colorScheme = theme
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#11151d' : '#f5f7fb')
    try { localStorage.setItem('aija.theme', theme) } catch { /* The current page still uses the selected theme. */ }
  }, [theme])

  useEffect(() => {
    if (!user) {
      setPage('dashboard')
      setActiveChatId(null)
      setLaunchChat(null)
    }
  }, [user])

  useEffect(() => {
    if (!toast) return undefined
    const timer = window.setTimeout(() => setToast(null), 4200)
    return () => window.clearTimeout(timer)
  }, [toast])

  const notify = useCallback((message, tone = 'info') => setToast({ message, tone, id: Date.now() }), [])
  const toggleTheme = useCallback(() => setTheme((current) => current === 'dark' ? 'light' : 'dark'), [])
  const openNewApplication = useCallback(() => {
    setPage('applications')
    setCreateTrigger((current) => current + 1)
  }, [])
  const openAssistant = useCallback((options) => {
    setPage('assistant')
    if (options?.chatId) {
      setActiveChatId(options.chatId)
      setLaunchChat(null)
      return
    }
    if (options) {
      setActiveChatId(null)
      setLaunchChat({ ...options, id: Date.now() })
    }
  }, [])
  const consumeLaunch = useCallback((id) => {
    setLaunchChat((current) => current?.id === id ? null : current)
  }, [])

  if (loading) return <main className="startup-screen"><span className="brand-mark"><Icon name="sparkle" size={20} /></span><span className="spinner" /><p>Preparing your private workspace…</p></main>
  if (!user) return <AuthScreen theme={theme} onToggleTheme={toggleTheme} onSignIn={signIn} onSignUp={signUp} />

  let content
  if (page === 'applications') content = <ApplicationsPage createTrigger={createTrigger} onOpenChat={openAssistant} onNotify={notify} />
  else if (page === 'files') content = <FilesPage onStartChat={openAssistant} onNotify={notify} />
  else if (page === 'assistant') content = <AssistantPage activeChatId={activeChatId} onActiveChatChange={setActiveChatId} launchChat={launchChat} onLaunchConsumed={consumeLaunch} onNotify={notify} />
  else if (page === 'history') content = <HistoryPage onOpenChat={openAssistant} />
  else if (page === 'settings') content = <SettingsPage theme={theme} onThemeChange={setTheme} onProfileUpdated={updateUser} onNotify={notify} />
  else content = <DashboardPage onNavigate={setPage} onCreateApplication={openNewApplication} onOpenChat={openAssistant} />

  return <>
    <AppShell user={user} page={page} onNavigate={setPage} theme={theme} onToggleTheme={toggleTheme} onSignOut={signOut} onNewApplication={openNewApplication}>
      {content}
    </AppShell>
    {toast && <div className={`toast toast-${toast.tone}`} role="status"><span className="toast-check"><Icon name={toast.tone === 'success' ? 'check' : 'sparkle'} size={16} /></span><span>{toast.message}</span><button className="toast-close" type="button" onClick={() => setToast(null)} aria-label="Dismiss notification"><Icon name="close" size={15} /></button></div>}
  </>
}
