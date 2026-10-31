import { DisplayNameStatus, useDisplayNameAvailability } from '@/components/ui/display-name-availability';
import { motion, useReducedMotion } from 'framer-motion';
import { useState, type FormEvent } from 'react';
import { ArrowRight, LoaderCircle } from 'lucide-react';
import { useSession, workspaceMutation, type AccountProfile } from '@/auth/session';

export default function Onboarding({ onComplete }: { onComplete: () => void }) {
  const { user, setProfile } = useSession();
  const reducedMotion = useReducedMotion();
  const [form, setForm] = useState(() => {
    try { const old = JSON.parse(localStorage.getItem(`heimdall-profile:${user?.email}`) || '{}'); return { firstName: old.firstName || '', lastName: old.lastName || '', username: old.username || '', website: old.website || '' }; }
    catch { return { firstName: '', lastName: '', username: '', website: '' }; }
  });
  const displayName = useDisplayNameAvailability(form.username);
  const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  function change(field: keyof typeof form, value: string) { setForm(previous => ({ ...previous, [field]: value })); setError(''); }
  async function submit(event: FormEvent) {
    event.preventDefault(); if (busy || displayName.blocked) return;
    setBusy(true); setError('');
    try {
      const profile = await workspaceMutation<AccountProfile>('profile', 'POST', form);
      onComplete(); setProfile(profile);
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not save your profile. Please retry.'); }
    finally { setBusy(false); }
  }
  return <main className="onboarding-page">
    <a href="/" className="onboarding-brand"><img src="/images/heimdall-logo.svg" alt="" />HEIMDALL</a>
    <motion.section className="onboarding-card onboarding-entrance" aria-labelledby="onboarding-title"
      initial={reducedMotion ? false : { opacity: 0, scale: 0.94, y: 28 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      transition={{ duration: reducedMotion ? 0 : 1.1, ease: [0.16, 1, 0.3, 1] }}>

      <header><p>LET’S MAKE IT YOURS</p><h1 id="onboarding-title"><span>Welcome to</span>Heimdall</h1><p>A few details, then you’re ready to discover your next lead.</p></header>
      <form onSubmit={submit}>
        <div className="profile-fields">
          <label>First name<input autoFocus required autoComplete="given-name" maxLength={60} value={form.firstName} onChange={e => change('firstName', e.target.value)} placeholder="Your first name" disabled={busy} /></label>
          <label>Last name <small>(optional)</small><input autoComplete="family-name" maxLength={60} value={form.lastName} onChange={e => change('lastName', e.target.value)} placeholder="Your last name" disabled={busy} /></label>
          <label className="profile-wide">Display name<input required autoComplete="nickname" maxLength={60} aria-invalid={displayName.status === 'taken'} aria-describedby="onboarding-name-status" value={form.username} onChange={e => change('username', e.target.value)} placeholder="How should we call you?" disabled={busy} /><DisplayNameStatus id="onboarding-name-status" status={displayName.status} /></label>
          <label className="profile-wide">Website <small>(optional)</small><span className="profile-website"><span>https://</span><input aria-label="Website (optional)" maxLength={250} value={form.website} onChange={e => change('website', e.target.value)} placeholder="yourwebsite.com" disabled={busy} /></span></label>
        </div>
        {error && <p className="profile-form-error" role="alert">{error}</p>}
        <button className="onboarding-continue" disabled={busy || displayName.blocked} type="submit">{busy ? <><LoaderCircle size={17} className="spin" />Saving your profile…</> : <>Continue to dashboard <ArrowRight size={17} /></>}</button>
        <p className="onboarding-note">Saved to your account. You can update these details in Profile anytime.</p>
      </form>
    </motion.section>
  </main>;
}
