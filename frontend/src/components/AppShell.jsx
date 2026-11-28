import { useState } from 'react'
import Icon from './Icon.jsx'
import { initials } from './UI.jsx'

const navigation = [
  { id: 'dashboard', label: 'Dashboard', icon: 'grid', section: 'Workspace' },
  { id: 'applications', label: 'Applications', icon: 'briefcase' },
  { id: 'files', label: 'Resume files', icon: 'file' },
  { id: 'assistant', label: 'AI assistant', icon: 'sparkle', section: 'Assistant' },
  { id: 'history', label: 'History', icon: 'clock' },
  { id: 'settings', label: 'Settings', icon: 'settings', section: 'Preferences' },
]

const pageCopy = {
  dashboard: ['Your job search, at a glance', 'Keep every application, interview, and next step in view.'],
  applications: ['Applications', 'Track where you applied and what needs your attention.'],
  files: ['Resume files', 'Keep your original and AI-generated resume versions together.'],
  assistant: ['AI career assistant', 'Get focused help with resumes, roles, interviews, and career growth.'],
  history: ['Activity history', 'A clear record of uploads, changes, chats, and deleted items.'],
  settings: ['Settings', 'Manage your profile and make the workspace feel like yours.'],
}

export default function AppShell({ user, page, onNavigate, theme, onToggleTheme, onSignOut, onNewApplication, children }) {
  const [mobileOpen, setMobileOpen] = useState(false)
  const [signingOut, setSigningOut] = useState(false)
  const [title, description] = pageCopy[page] || pageCopy.dashboard

  const navigate = (pageId) => {
    onNavigate(pageId)
    setMobileOpen(false)
  }

  const logout = async () => {
    setSigningOut(true)
    try { await onSignOut() } finally { setSigningOut(false) }
  }

  return (
    <div className="app-layout">
      {mobileOpen && <button className="mobile-scrim" onClick={() => setMobileOpen(false)} aria-label="Close navigation" />}
      <aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}>
        <button className="brand-lockup" type="button" onClick={() => navigate('dashboard')}>
          <span className="brand-mark"><Icon name="sparkle" size={20} /></span>
          <span><strong>AI Job Assistant</strong><small>YOUR CAREER WORKSPACE</small></span>
        </button>

        <div className="sidebar-label">MENU</div>
        <nav className="side-nav" aria-label="Main navigation">
          {navigation.map((item, index) => (
            <div key={item.id}>
              {item.section && <div className={`nav-section ${index ? 'nav-section-spaced' : ''}`}>{item.section}</div>}
              <button className={`nav-item ${page === item.id ? 'nav-item-active' : ''}`} type="button"
                onClick={() => navigate(item.id)} aria-current={page === item.id ? 'page' : undefined}>
                <Icon name={item.icon} size={18} /><span>{item.label}</span>
                {item.id === 'assistant' && <span className="nav-new-dot" />}
              </button>
            </div>
          ))}
        </nav>

        <div className="sidebar-spacer" />
        <div className="sidebar-tip">
          <div className="tip-icon"><Icon name="sparkle" size={17} /></div>
          <strong>Your next step, made clear.</strong>
          <p>Small, steady progress adds up. Keep your search moving.</p>
          <button type="button" onClick={() => navigate('assistant')}>Talk to your assistant <Icon name="arrow" size={15} /></button>
        </div>
        <div className="sidebar-account">
          <span className="avatar avatar-sidebar">{initials(user?.name)}</span>
          <span className="account-copy"><strong>{user?.name || 'Your account'}</strong><small>{user?.email || ''}</small></span>
          <button className="icon-button account-logout" type="button" onClick={logout} disabled={signingOut}
            title="Sign out" aria-label="Sign out"><Icon name="logout" size={17} /></button>
        </div>
      </aside>

      <div className="main-column">
        <header className="topbar">
          <button className="icon-button mobile-menu-button" type="button" onClick={() => setMobileOpen(true)} aria-label="Open navigation"><Icon name="menu" /></button>
          <div className="topbar-greeting"><strong>{title}</strong><span>{description}</span></div>
          <div className="topbar-actions">
            <button className="theme-toggle" onClick={onToggleTheme} type="button" aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`} title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}>
              <Icon name={theme === 'dark' ? 'sun' : 'moon'} size={17} /><span>{theme === 'dark' ? 'Light' : 'Dark'} mode</span>
            </button>
            <button className="button button-primary button-small topbar-create" type="button" onClick={onNewApplication}>
              <Icon name="plus" size={17} /><span>New application</span>
            </button>
            <span className="avatar topbar-avatar" title={user?.name}>{initials(user?.name)}</span>
          </div>
        </header>
        <main className="page-content">{children}</main>
      </div>
    </div>
  )
}
