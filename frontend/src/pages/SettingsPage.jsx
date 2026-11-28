import { useCallback, useEffect, useState } from 'react'
import { profileApi } from '../api/resources.js'
import Icon from '../components/Icon.jsx'
import { FailureState, LoadingState, Notice, PageIntro } from '../components/UI.jsx'

export default function SettingsPage({ theme, onThemeChange, onProfileUpdated, onNotify }) {
  const [form, setForm] = useState({ name: '', email: '', phoneNumber: '' })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { const profile = await profileApi.get(); setForm({ name: profile.name || '', email: profile.email || '', phoneNumber: profile.phoneNumber || '' }) }
    catch (requestError) { setError(requestError.message || 'Could not load your profile.') }
    finally { setLoading(false) }
  }, [])
  useEffect(() => { load() }, [load])

  const update = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setError('')
    try {
      const profile = await profileApi.save({ name: form.name.trim(), email: form.email.trim(), phoneNumber: form.phoneNumber.trim() || null })
      setForm({ name: profile.name, email: profile.email, phoneNumber: profile.phoneNumber || '' })
      onProfileUpdated?.({ name: profile.name, email: profile.email })
      onNotify?.('Your profile has been updated.', 'success')
    } catch (requestError) { setError(requestError.message || 'Could not save your profile.') }
    finally { setSaving(false) }
  }

  return <div className="page-stack settings-page">
    <PageIntro eyebrow="PERSONALIZE" title="Settings" description="Your account details and the way your workspace looks." />
    <Notice>{error}</Notice>
    {loading ? <LoadingState label="Loading your settings…" /> : error && !form.name && !form.email ? <FailureState title="Your settings couldn’t load" description={error} onRetry={load} /> : <div className="settings-grid">
      <section className="panel settings-card">
        <div className="settings-card-heading"><span className="settings-heading-icon"><Icon name="settings" size={19} /></span><div><h2>Profile information</h2><p>These details are used across your account.</p></div></div>
        <form className="settings-form" onSubmit={submit}>
          <label className="field">Full name<input name="name" value={form.name} onChange={update} maxLength={120} required autoComplete="name" /></label>
          <label className="field">Sign-in email<input name="email" type="email" value={form.email} onChange={update} maxLength={254} required autoComplete="email" /><small className="field-hint">This also changes the email you use to sign in.</small></label>
          <label className="field">Phone number <span className="field-optional">optional</span><input name="phoneNumber" type="tel" value={form.phoneNumber} onChange={update} maxLength={32} autoComplete="tel" placeholder="Add a phone number if you’d like" /></label>
          <div className="settings-save-row"><span><Icon name="check" size={15} />Your information belongs to your account.</span><button className="button button-primary" disabled={saving}>{saving ? 'Saving…' : 'Save profile'}</button></div>
        </form>
      </section>

      <section className="panel settings-card appearance-card">
        <div className="settings-card-heading"><span className="settings-heading-icon settings-heading-purple"><Icon name="sun" size={19} /></span><div><h2>Appearance</h2><p>Choose the theme that’s easiest on your eyes.</p></div></div>
        <div className="theme-options" role="radiogroup" aria-label="Choose appearance theme">
          <button className={`theme-option ${theme === 'light' ? 'theme-option-active' : ''}`} type="button" role="radio" aria-checked={theme === 'light'} onClick={() => onThemeChange('light')}><span className="theme-preview theme-preview-light"><i /><i /><i /></span><strong><Icon name="sun" size={16} />Light mode</strong><small>Bright and clear</small><span className="theme-radio" /></button>
          <button className={`theme-option ${theme === 'dark' ? 'theme-option-active' : ''}`} type="button" role="radio" aria-checked={theme === 'dark'} onClick={() => onThemeChange('dark')}><span className="theme-preview theme-preview-dark"><i /><i /><i /></span><strong><Icon name="moon" size={16} />Dark mode</strong><small>Comfortable in low light</small><span className="theme-radio" /></button>
        </div>
        <div className="appearance-note"><Icon name="check" size={16} /><span>Your choice is saved in this browser and will be used next time you open the app.</span></div>
      </section>

      <section className="panel privacy-card"><span className="privacy-icon"><Icon name="check" size={18} /></span><div><h2>Your private job search</h2><p>Your applications, resume files, history, and chats are separated by your account. Your resume conversations use the local Ollama service configured for this project.</p></div></section>
    </div>}
  </div>
}
