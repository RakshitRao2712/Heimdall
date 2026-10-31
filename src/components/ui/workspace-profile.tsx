import { DisplayNameStatus, useDisplayNameAvailability } from '@/components/ui/display-name-availability';
import { useSession, workspaceMutation, type AccountProfile } from '@/auth/session';
import { useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { BadgeCheck, ImagePlus, KeyRound, LogOut, Trash2, X } from 'lucide-react';

type Profile = { firstName: string; lastName: string; username: string; website: string; cover: string; avatar: string };
const empty: Profile = { firstName: '', lastName: '', username: '', website: '', cover: '', avatar: '' };
function readProfile(key: string): Profile {
  try {
    const value = JSON.parse(localStorage.getItem(key) || '{}');
    return Object.fromEntries(Object.keys(empty).map(name => [name, typeof value?.[name] === 'string' ? value[name] : ''])) as Profile;
  } catch { return { ...empty }; }
}

export function WorkspaceProfile({ email, keyConfigured, keyLoading, searches, loggingOut, onManageKey, onLogout }: {
  email: string; keyConfigured: boolean; keyLoading: boolean; searches: number;
  loggingOut: boolean; onManageKey: () => void; onLogout: () => void;
}) {
  const { profile, setProfile } = useSession();
  const [saving, setSaving] = useState(false);
  const storageKey = `heimdall-profile:${email}`;
  const [saved, setSaved] = useState(() => ({ ...readProfile(storageKey), ...(profile ? { firstName: profile.firstName, lastName: profile.lastName, username: profile.username, website: profile.website } : {}) }));
  const [draft, setDraft] = useState(saved);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [uploading, setUploading] = useState(false);
  const coverInput = useRef<HTMLInputElement>(null);
  const avatarInput = useRef<HTMLInputElement>(null);
  const displayName = useDisplayNameAvailability(draft.username);
  const dirty = JSON.stringify(saved) !== JSON.stringify(draft);
  const initials = (draft.firstName ? draft.firstName[0] + (draft.lastName[0] || '') : email.slice(0, 2)).toUpperCase();
  function change(field: keyof Profile, value: string) {
    setDraft(previous => ({ ...previous, [field]: value })); setMessage(''); setError('');
  }
  function upload(event: ChangeEvent<HTMLInputElement>, field: 'cover' | 'avatar') {
    const file = event.target.files?.[0]; event.target.value = '';
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 1024 * 1024) {
      setError('Choose a JPG, PNG, or WebP image smaller than 1 MB.'); return;
    }
    setUploading(true); setError('');
    const reader = new FileReader();
    reader.onload = () => { change(field, String(reader.result)); setUploading(false); };
    reader.onerror = () => { setError('The image could not be read. Try another file.'); setUploading(false); };
    reader.readAsDataURL(file);
  }
  async function save(event: FormEvent) {
    event.preventDefault(); if (saving || displayName.blocked) return;
    const next = { ...draft, firstName: draft.firstName.trim(), lastName: draft.lastName.trim(), username: draft.username.trim(), website: draft.website.trim().replace(/^https?:\/\//i, '') };
    if (next.website) {
      try { const url = new URL(`https://${next.website}`); if (!url.hostname.includes('.') || url.username || url.password) throw new Error(); }
      catch { setError('Enter a valid website, such as example.com.'); return; }
    }
    setSaving(true);
    try {
      const account = await workspaceMutation<AccountProfile>('profile', 'POST', next);
      setProfile(account); setSaved(next); setDraft(next); setError('');
      try { localStorage.setItem(storageKey, JSON.stringify(next)); setMessage('Profile saved.'); }
      catch { setMessage('Profile details saved. Images could not be saved in this browser; try smaller files.'); }
      window.dispatchEvent(new Event('heimdall-profile-updated'));
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not save your profile. Please retry.'); }
    finally { setSaving(false); }
  }
  return <section className="profile-settings" aria-label="Profile settings">
    <form onSubmit={save}>
      <div className={`profile-cover ${draft.cover ? 'has-image' : ''}`}>
        {draft.cover && <img src={draft.cover} alt="Your profile cover" />}
        <div className="profile-cover-controls">
          <button type="button" onClick={() => coverInput.current?.click()} aria-label="Upload cover image" title="Upload cover image"><ImagePlus size={21} /></button>
          {draft.cover && <button type="button" onClick={() => change('cover', '')} aria-label="Remove cover image"><X size={21} /></button>}
        </div>
      </div>
      <input hidden ref={coverInput} type="file" accept="image/jpeg,image/png,image/webp" onChange={event => upload(event, 'cover')} />
      <input hidden ref={avatarInput} type="file" accept="image/jpeg,image/png,image/webp" onChange={event => upload(event, 'avatar')} />
      <div className="profile-form-body">
        <div className="profile-avatar-row">
          <button type="button" className="profile-photo" onClick={() => avatarInput.current?.click()} aria-label="Upload profile photo">
            {draft.avatar ? <img src={draft.avatar} alt="" /> : <span>{initials}</span>}
            <span className="profile-photo-edit"><ImagePlus size={22} /></span>
          </button>
          {draft.avatar && <button className="profile-remove-photo" type="button" onClick={() => change('avatar', '')}><Trash2 size={14} aria-hidden="true" />Remove photo</button>}
        </div>
        <div className="profile-fields">
          <label>First name<input required autoComplete="given-name" maxLength={60} value={draft.firstName} onChange={e => change('firstName', e.target.value)} placeholder="Your first name" /></label>
          <label>Last name<input autoComplete="family-name" maxLength={60} value={draft.lastName} onChange={e => change('lastName', e.target.value)} placeholder="Your last name" /></label>
          <label className="profile-wide">Display name<input required autoComplete="nickname" maxLength={60} aria-invalid={displayName.status === 'taken'} aria-describedby="profile-name-status" value={draft.username} onChange={e => change('username', e.target.value)} placeholder="Choose a display name" /><DisplayNameStatus id="profile-name-status" status={displayName.status} /></label>
          <label className="profile-wide">Website<span className="profile-website"><span>https://</span><input aria-label="Website" autoComplete="url" maxLength={250} value={draft.website} onChange={e => change('website', e.target.value)} placeholder="yourwebsite.com" /></span></label>
          <label className="profile-wide">Email address<span className="profile-email"><input aria-label="Email address" type="email" value={email} readOnly /><BadgeCheck size={20} aria-label="Signed in" /></span></label>
        </div>
        <div className="profile-account-details">
          <div><span>Google Places connection</span><strong>{keyLoading ? 'Checking…' : keyConfigured ? 'Connected for this session' : 'No key connected'}</strong></div>
          <button className="workspace-button" type="button" onClick={onManageKey}><KeyRound size={14} />Manage API key</button>
        </div>
        <p className="profile-storage-note">Personal workspace · {searches} saved searches. Profile details and lead files save to your account. Cover and profile photos stay in this browser.</p>
        {error && <p role="alert" className="profile-form-error">{error}</p>}
        {message && <p role="status" className="profile-form-success">{message}</p>}
      </div>
      <div className="profile-form-footer">
        <button type="button" className="profile-signout" disabled={loggingOut} onClick={onLogout}><LogOut size={16} />{loggingOut ? 'Signing out…' : 'Sign out'}</button>
        <div><button type="button" className="profile-cancel" disabled={!dirty || uploading || saving} onClick={() => { setDraft(saved); setError(''); setMessage(''); }}>Cancel</button><button type="submit" className="profile-save" disabled={!dirty || uploading || saving || displayName.blocked}>{saving ? 'Saving…' : uploading ? 'Reading image…' : 'Save changes'}</button></div>
      </div>
    </form>
  </section>;
}
