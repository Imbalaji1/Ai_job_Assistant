import { useState } from 'react'
import Icon from './Icon.jsx'
import { Notice } from './UI.jsx'

export default function AuthScreen({ theme, onToggleTheme, onSignIn, onSignUp }) {
  const [registerMode, setRegisterMode] = useState(false)
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const update = (event) => { setForm((current) => ({ ...current, [event.target.name]: event.target.value })); setError('') }
  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setError('')
    try {
      if (registerMode) await onSignUp(form.name, form.email, form.password)
      else await onSignIn(form.email, form.password)
    } catch (requestError) { setError(requestError.message || 'Could not connect to your account.') }
    finally { setBusy(false) }
  }
  const switchMode = () => { setRegisterMode((mode) => !mode); setError('') }

  return <main className="auth-screen">
    <button className="auth-theme-toggle" type="button" onClick={onToggleTheme}><Icon name={theme === 'dark' ? 'sun' : 'moon'} size={16} />{theme === 'dark' ? 'Light mode' : 'Dark mode'}</button>
    <section className="auth-layout">
      <div className="auth-story">
        <div className="auth-story-glow" />
        <div className="brand-lockup auth-brand"><span className="brand-mark"><Icon name="sparkle" size={20} /></span><span><strong>AI Job Assistant</strong><small>YOUR CAREER WORKSPACE</small></span></div>
        <div className="auth-story-copy"><span className="auth-pill"><i />A clearer way to move forward</span><h1>Your next opportunity deserves a little more <em>focus.</em></h1><p>Keep your job search organized, your resume versions close, and your progress easy to see.</p></div>
        <div className="auth-feature-stack">
          <div className="auth-feature"><span><Icon name="briefcase" size={17} /></span><div><strong>Every application, remembered</strong><small>Track status, interviews, and follow-ups.</small></div></div>
          <div className="auth-feature"><span><Icon name="file" size={17} /></span><div><strong>Your resume, in good hands</strong><small>Keep original and updated versions together.</small></div></div>
          <div className="auth-feature"><span><Icon name="sparkle" size={17} /></span><div><strong>Career help when you need it</strong><small>Ask your local AI assistant focused questions.</small></div></div>
        </div>
        <div className="auth-art" aria-hidden="true"><span className="art-orbit orbit-one" /><span className="art-orbit orbit-two" /><span className="art-star star-one">✦</span><span className="art-star star-two">✧</span><div className="art-card"><span className="art-card-icon"><Icon name="check" size={19} /></span><span><strong>One step at a time</strong><small>Your search is taking shape</small></span><Icon name="arrow" size={17} /></div></div>
        <p className="auth-story-foot">A private space for your career journey</p>
      </div>

      <div className="auth-panel">
        <div className="auth-panel-inner">
          <span className="auth-mobile-brand"><span className="brand-mark"><Icon name="sparkle" size={18} /></span>AI Job Assistant</span>
          <p className="auth-eyebrow">{registerMode ? 'LET’S GET STARTED' : 'WELCOME BACK'}</p>
          <h2>{registerMode ? 'Create your account' : 'Good to see you again.'}</h2>
          <p className="auth-panel-description">{registerMode ? 'Set up your private workspace in a moment.' : 'Sign in and pick up right where you left off.'}</p>
          <form className="auth-form" onSubmit={submit}>
            <Notice>{error}</Notice>
            {registerMode && <label className="field">Your name<input name="name" value={form.name} onChange={update} autoComplete="name" maxLength={120} placeholder="How should we call you?" required /></label>}
            <label className="field">Email address<input name="email" type="email" value={form.email} onChange={update} autoComplete="email" maxLength={254} placeholder="you@example.com" required /></label>
            <label className="field">Password<input name="password" type="password" value={form.password} onChange={update} autoComplete={registerMode ? 'new-password' : 'current-password'} minLength={registerMode ? 12 : 1} maxLength={72} placeholder={registerMode ? 'At least 12 characters' : 'Enter your password'} required />{registerMode && <small className="field-hint">Use at least 12 characters.</small>}</label>
            <button className="button button-primary auth-submit" disabled={busy}>{busy ? <><span className="spinner spinner-light" />Please wait…</> : <>{registerMode ? 'Create account' : 'Sign in'}<Icon name="arrow" size={17} /></>}</button>
          </form>
          <p className="auth-switch">{registerMode ? 'Already have an account?' : 'New to AI Job Assistant?'} <button type="button" onClick={switchMode}>{registerMode ? 'Sign in' : 'Create account'}</button></p>
          <div className="auth-security"><Icon name="check" size={15} /><span>Your data is tied to your account. Your password is never stored in this browser.</span></div>
        </div>
        <div className="auth-panel-footer"><span>AI JOB ASSISTANT</span><span>Local AI · Private workspace</span></div>
      </div>
    </section>
  </main>
}
