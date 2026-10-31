import GlassFolder, { hashString, type ArtPreset } from './glass-folder-cards';
import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { ChevronRight, Copy, Crown, FolderOpen, Import, LoaderCircle, LogOut, MessageSquare, Plus, RefreshCw, Search, Send, UserMinus, Users, X } from 'lucide-react';
import { useSession, workspaceMutation } from '@/auth/session';
import type { LeadFile, LibraryLead } from './lead-library';
import './teams-workspace.css';

type Actor = { email: string; name: string };
type TeamNote = { id: string; text: string; author: Actor; createdAt: string };
type SharedLead = { id: string; data: LibraryLead; done: boolean; updatedBy: Actor | null; updatedAt: string | null; version: number; notes: TeamNote[] };
type SharedFolder = { id: string; name: string; sourceFileId: string; method: string; query: string; importedBy: Actor; importedAt: string; leads: SharedLead[] };
type Team = { id: string; name: string; leaderEmail: string; inviteCode: string | null; createdAt: string; revision: number; members: { user: Actor; joinedAt: string }[]; folders: SharedFolder[] };
type Summary = { id: string; name: string; role: string; members: number; folders: number; leads: number; done: number };
type DialogMode = { type: 'create' | 'join' | 'import' | 'leave' | 'rotate' } | { type: 'remove'; member: Actor };
const path = (team: string) => `teams/${encodeURIComponent(team)}`;
const date = (value: string) => new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
function website(value: string | null) { try { const url = new URL(value || ''); return ['https:', 'http:'].includes(url.protocol) ? url.href : undefined; } catch { return undefined; } }
class TeamRequestError extends Error { constructor(message: string, readonly status: number) { super(message); } }
async function get<T>(endpoint: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/workspace/${endpoint}`, { credentials: 'same-origin', cache: 'no-store', signal });
  if (response.status === 401) { window.location.replace('/login'); throw new Error('Please sign in again.'); }
  if (!response.ok) { let message = ''; try { const body = await response.json(); message = body.detail || body.message || ''; } catch { /* Status fallback. */ } throw new TeamRequestError(message || 'Could not load your team workspace. Please retry.', response.status); }
  return response.json();
}

function TeamDialog({ mode, files, onClose, onSubmit, busy, error }: {
  mode: DialogMode; files: LeadFile[]; onClose: () => void; onSubmit: (value: string) => Promise<void>; busy: boolean; error: string;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const [value, setValue] = useState(mode.type === 'import' ? files[0]?.id || '' : '');
  useEffect(() => { dialog.current?.showModal(); return () => dialog.current?.close(); }, []);
  const title = mode.type === 'create' ? 'Create a team' : mode.type === 'join' ? 'Join a team' : mode.type === 'import' ? 'Import a lead folder' : mode.type === 'leave' ? 'Leave this team?' : mode.type === 'rotate' ? 'Replace the join code?' : `Remove ${mode.type === 'remove' ? mode.member.name : 'member'}?`;
  async function submit(event: FormEvent) { event.preventDefault(); if (!busy) await onSubmit(value.trim()); }
  return <dialog className="lead-file-dialog teams-dialog" ref={dialog} onCancel={event => { event.preventDefault(); if (!busy) onClose(); }} aria-labelledby="team-dialog-title">
    <form onSubmit={submit}><div className="lead-dialog-heading"><div><p>TEAM WORKSPACE</p><h2 id="team-dialog-title">{title}</h2></div><button type="button" className="workspace-icon-button" aria-label="Close dialog" disabled={busy} onClick={onClose}><X size={18} /></button></div>
      {mode.type === 'create' && <><p className="teams-muted">You’ll be the leader. Share your join code to bring people into your team.</p><label htmlFor="team-name">Team name</label><input autoFocus id="team-name" required maxLength={80} placeholder="e.g. Northstar growth team" value={value} onChange={event => setValue(event.target.value)} disabled={busy} /></>}
      {mode.type === 'join' && <><p className="teams-muted">Ask your team leader for a join code. Joining gives you access to that team’s shared folders and notes.</p><label htmlFor="team-code">Join code</label><input autoFocus id="team-code" required maxLength={100} autoComplete="off" spellCheck={false} placeholder="Paste the code from your leader" value={value} onChange={event => setValue(event.target.value)} disabled={busy} /></>}
      {mode.type === 'import' && <><p className="teams-muted">Import a copy from your personal Leads library. Everyone in this team can see the leads, update their status, and add notes. Your original file stays unchanged.</p><label htmlFor="team-file">Your lead folder</label><select autoFocus id="team-file" required disabled={busy || !files.length} value={value} onChange={event => setValue(event.target.value)}>{!files.length && <option value="">No files available to import</option>}{files.map(file => <option key={file.id} value={file.id}>{file.name} · {file.leads.length} leads</option>)}</select>{!files.length && <p className="teams-muted">Save a search to Leads first, or choose a file that isn’t already shared with this team.</p>}</>}
      {mode.type === 'leave' && <p className="teams-muted">You’ll lose access to this team’s folders and notes. Your contributions will remain for the team. A leader’s join code lets you rejoin later.</p>}
      {mode.type === 'remove' && <p className="teams-muted">This member will lose access immediately. Their notes and imported folders will remain. Replace the join code too if you don’t want the old code used to rejoin.</p>}
      {mode.type === 'rotate' && <p className="teams-muted">The old join code will stop working. Current members keep their access. Share the new code with anyone you want to invite.</p>}
      {error && <p className="teams-error" role="alert">{error}</p>}
      <div className="teams-dialog-actions"><button type="button" className="workspace-button" disabled={busy} onClick={onClose}>Cancel</button><button className="workspace-button teams-primary" type="submit" disabled={busy || (['create', 'join', 'import'].includes(mode.type) && !value.trim())}>{busy && <LoaderCircle size={14} className="spin" />}{mode.type === 'create' ? 'Create team' : mode.type === 'join' ? 'Join team' : mode.type === 'import' ? 'Import for everyone' : mode.type === 'rotate' ? 'Replace code' : mode.type === 'leave' ? 'Leave team' : 'Remove member'}</button></div>
    </form>
  </dialog>;
}

export function TeamsWorkspace() {
  const { user } = useSession();
  const [teams, setTeams] = useState<Summary[]>([]); const [selectedId, setSelectedId] = useState(''); const [team, setTeam] = useState<Team | null>(null);
  const [folderId, setFolderId] = useState(''); const [openedFolderId, setOpenedFolderId] = useState(''); const [leadId, setLeadId] = useState('');
  const [query, setQuery] = useState(''); const [status, setStatus] = useState('all'); const [note, setNote] = useState('');
  const [initialLoading, setInitialLoading] = useState(true); const [error, setError] = useState(''); const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(''); const [dialog, setDialog] = useState<DialogMode | null>(null); const [dialogError, setDialogError] = useState('');
  const [files, setFiles] = useState<LeadFile[]>([]); const [refresh, setRefresh] = useState(0); const [updatedAt, setUpdatedAt] = useState('');
  const notePanel = useRef<HTMLElement>(null);
  const selected = useRef(selectedId); selected.current = selectedId;
  const adopt = useCallback((next: Team) => {
    if (selected.current !== next.id) return;
    setTeam(previous => previous?.id === next.id && previous.revision > next.revision ? previous : next);
    setUpdatedAt(new Date().toLocaleTimeString());
  }, []);
  useEffect(() => {
    const controller = new AbortController(); let running = false;
    const load = async () => {
      if (running) return; running = true;
      try {
        const list = await get<Summary[]>('teams', controller.signal);
        if (controller.signal.aborted) return;
        setTeams(list); setError('');
        setSelectedId(current => list.some(item => item.id === current) ? current : list[0]?.id || '');
      } catch (e) { if (!controller.signal.aborted) setError(e instanceof Error ? e.message : 'Could not load teams.'); }
      finally { running = false; if (!controller.signal.aborted) setInitialLoading(false); }
    };
    void load(); const tick = () => { if (!document.hidden) void load(); };
    const timer = window.setInterval(tick, 10000); window.addEventListener('focus', tick);
    return () => { controller.abort(); window.clearInterval(timer); window.removeEventListener('focus', tick); };
  }, [refresh]);
  useEffect(() => {
    setTeam(null); setFolderId(''); setOpenedFolderId(''); setLeadId(''); setNote(''); setQuery(''); setStatus('all'); setUpdatedAt('');
    if (!selectedId) return;
    const controller = new AbortController(); let running = false;
    const load = async () => {
      if (running) return; running = true;
      try { const next = await get<Team>(path(selectedId), controller.signal); if (!controller.signal.aborted) { adopt(next); setError(''); } }
      catch (e) { if (!controller.signal.aborted) { setError(e instanceof Error ? e.message : 'Could not refresh this team.'); if (e instanceof TeamRequestError && [403, 404].includes(e.status)) { setTeam(null); setSelectedId(''); setRefresh(value => value + 1); } } }
      finally { running = false; }
    };
    void load(); const tick = () => { if (!document.hidden) void load(); };
    const timer = window.setInterval(tick, 5000); window.addEventListener('focus', tick);
    return () => { controller.abort(); window.clearInterval(timer); window.removeEventListener('focus', tick); };
  }, [selectedId, refresh, adopt]);
  const folder = team?.folders.find(item => item.id === folderId) || team?.folders[0];
  const filtered = folder?.leads.filter(lead => (status === 'all' || (status === 'done' ? lead.done : !lead.done)) && `${lead.data.name} ${lead.data.address || ''} ${lead.data.phones.join(' ')}`.toLowerCase().includes(query.toLowerCase())) || [];
  const activeLead = folder?.leads.find(item => item.id === leadId);
  useEffect(() => {
    if (leadId && window.innerWidth <= 1100) notePanel.current?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'nearest' });
  }, [leadId]);
  const leader = team?.leaderEmail === user?.email;
  const total = team?.folders.reduce((count, file) => count + file.leads.length, 0) || 0;
  const done = team?.folders.reduce((count, file) => count + file.leads.filter(lead => lead.done).length, 0) || 0;
  function chooseFolder(id: string, scroll = false) {
    if (busy) return;
    setFolderId(id); setOpenedFolderId(id); setLeadId(''); setNote(''); setQuery(''); setStatus('all');
    if (scroll) window.requestAnimationFrame(() => document.getElementById('team-shared-folder-detail')?.scrollIntoView({ block: 'start', behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' }));
  }
  function chooseTeam(id: string) { if (busy) return; setSelectedId(id); setError(''); setNotice(''); }
  function openDialog(mode: DialogMode) { setDialogError(''); setDialog(mode); }
  async function openImport() {
    if (!team || busy) return; setBusy('library'); setError('');
    try { const library = await get<LeadFile[]>('files'); setFiles(library.filter(file => file.leads.length && !team.folders.some(shared => shared.sourceFileId === file.id && shared.importedBy.email === user?.email))); openDialog({ type: 'import' }); }
    catch (e) { setError(e instanceof Error ? e.message : 'Cannot load your lead files.'); }
    finally { setBusy(''); }
  }
  async function submitDialog(value: string) {
    if (!dialog || busy) return; setBusy('dialog'); setDialogError('');
    try {
      let result: Team | undefined;
      if (dialog.type === 'create') result = await workspaceMutation<Team>('teams', 'POST', { name: value });
      else if (dialog.type === 'join') result = await workspaceMutation<Team>('teams/join', 'POST', { code: value });
      else if (team) {
        if (dialog.type === 'import') result = await workspaceMutation<Team>(`${path(team.id)}/folders`, 'POST', { fileId: value });
        if (dialog.type === 'rotate') result = await workspaceMutation<Team>(`${path(team.id)}/invite`, 'POST', {});
        if (dialog.type === 'remove') await workspaceMutation(`${path(team.id)}/members/remove`, 'POST', { email: dialog.member.email });
        if (dialog.type === 'leave') { await workspaceMutation(`${path(team.id)}/leave`, 'POST', {}); setSelectedId(''); setTeam(null); }
      }
      if (result) { selected.current = result.id; setSelectedId(result.id); setTeam(result); if (dialog.type === 'import') { setFolderId(result.folders[0]?.id || ''); setLeadId(''); } }
      setNotice(dialog.type === 'import' ? 'Folder imported. Your team can now work on these leads together.' : dialog.type === 'create' ? 'Team created. Share the join code with your teammates.' : dialog.type === 'join' ? 'You’ve joined the team.' : dialog.type === 'remove' ? 'Member removed.' : dialog.type === 'rotate' ? 'Join code replaced.' : 'You’ve left the team.');
      setDialog(null); setRefresh(count => count + 1);
    } catch (e) { setDialogError(e instanceof Error ? e.message : 'Could not update this team.'); }
    finally { setBusy(''); }
  }
  async function updateLead(lead: SharedLead, kind: 'status' | 'note', text?: string) {
    if (!team || !folder || busy) return;
    const teamId = team.id; const endpoint = `${path(teamId)}/folders/${folder.id}/leads/${lead.id}`;
    setBusy(`${kind}:${lead.id}`); setError(''); setNotice('');
    try {
      const next = await workspaceMutation<Team>(kind === 'note' ? `${endpoint}/notes` : endpoint, kind === 'note' ? 'POST' : 'PATCH', kind === 'note' ? { text } : { done: !lead.done, version: lead.version });
      adopt(next); if (kind === 'note') setNote('');
    } catch (e) { setError(e instanceof Error ? e.message : 'Could not save this change.'); }
    finally { setBusy(''); }
  }
  async function copyCode() {
    if (!team?.inviteCode) return;
    try { await navigator.clipboard.writeText(team.inviteCode); setNotice('Join code copied. Share it only with people you want to join this team.'); }
    catch { setError('Copy is unavailable in this browser. Select the code and copy it manually.'); }
  }
  return <section className="teams-workspace" aria-label="Teams workspace">
    <div className="teams-toolbar"><span><span className="teams-live-dot" />Shared workspace · syncs every 5 seconds</span><div><button className="workspace-button" disabled={!!busy} onClick={() => setRefresh(count => count + 1)}><RefreshCw size={14} />Refresh</button><button className="workspace-button" disabled={!!busy} onClick={() => openDialog({ type: 'join' })}>Join team</button><button className="workspace-button teams-primary" disabled={!!busy} onClick={() => openDialog({ type: 'create' })}><Plus size={15} />Create team</button></div></div>
    {error && <p className="teams-error" role="alert">{error}</p>}{notice && <p className="teams-notice" role="status">{notice}</p>}
    {initialLoading ? <p className="teams-empty"><LoaderCircle size={24} className="spin" />Loading your teams…</p> : !teams.length ? <div className="teams-empty"><Users size={40} /><h2>Better leads. Shared progress.</h2><p>Create a team or join one with a code from your leader.<br />Bring your lead folders, work through them together, and keep every conversation in context.</p><div><button className="workspace-button teams-primary" onClick={() => openDialog({ type: 'create' })}>Create your first team</button><button className="workspace-button" onClick={() => openDialog({ type: 'join' })}>I have a join code</button></div></div> : <>
      <nav className="teams-switcher" aria-label="Your teams">{teams.map(item => <button key={item.id} aria-current={selectedId === item.id ? 'page' : undefined} onClick={() => chooseTeam(item.id)} disabled={!!busy}><span className="teams-team-icon"><Users size={18} /></span><span><strong>{item.name}</strong><small>{item.role === 'leader' ? 'Leader' : 'Member'} · {item.members} members</small></span><ChevronRight size={14} /></button>)}</nav>
      {!team ? <p className="teams-empty"><LoaderCircle size={22} className="spin" />Opening shared workspace…</p> : <>
        <div className="teams-team-heading"><div><span className="teams-kicker">{leader ? <><Crown size={13} />YOU’RE THE LEADER</> : 'TEAM MEMBER'}</span><h2>{team.name}</h2><p>{updatedAt ? `Last synced ${updatedAt}` : 'Syncing…'}</p></div><div><button className="workspace-button teams-primary" disabled={!!busy} onClick={() => void openImport()}>{busy === 'library' ? <LoaderCircle size={15} className="spin" /> : <Import size={15} />}Import lead folder</button>{!leader && <button className="workspace-button" disabled={!!busy} onClick={() => openDialog({ type: 'leave' })}><LogOut size={14} />Leave team</button>}</div></div>
        <div className="teams-stats">{[['Members', team.members.length], ['Shared folders', team.folders.length], ['Total leads', total], ['Done', done]].map(([label, value]) => <div key={label}><span>{label}</span><strong>{value}</strong></div>)}</div>
        <div className={`teams-people-grid ${leader ? '' : 'member-view'}`}>
          {leader && <section className="teams-panel teams-invite"><h3><Copy size={16} />Invite your teammates</h3><p>Anyone with this code and a Heimdall account can join. Only you can view or replace it.</p><div><input aria-label="Team join code" readOnly value={team.inviteCode || ''} onFocus={event => event.currentTarget.select()} /><button className="workspace-button" onClick={() => void copyCode()}><Copy size={14} />Copy</button></div><button className="teams-text-button" disabled={!!busy} onClick={() => openDialog({ type: 'rotate' })}>Replace join code</button></section>}
          <section className="teams-panel"><h3><Users size={16} />Team members</h3><ul className="teams-members">{team.members.map(member => <li key={member.user.email}><span className="teams-member-avatar">{member.user.name.slice(0, 2).toUpperCase()}</span><div><strong>{member.user.name}{member.user.email === user?.email ? ' (you)' : ''}</strong><small>{member.user.email === team.leaderEmail ? 'Leader' : 'Member'} · {member.user.email}</small></div>{member.user.email === team.leaderEmail ? <Crown size={15} className="teams-crown" /> : leader && <button className="teams-icon-button" disabled={!!busy} onClick={() => openDialog({ type: 'remove', member: member.user })} aria-label={`Remove ${member.user.name}`}><UserMinus size={15} /></button>}</li>)}</ul></section>
        </div>
        <div className="teams-section-title"><h3>Shared lead folders</h3><span>Copies shared by your team · originals stay private</span></div>
        {!team.folders.length ? <div className="teams-panel teams-empty"><FolderOpen size={32} /><h3>A shared starting point</h3><p>Import a folder from Leads so everyone can start working on the same list.</p><button className="workspace-button" disabled={!!busy} onClick={() => void openImport()}>Import a lead folder</button></div> : <>
          <div className="lead-folder-grid teams-glass-grid">{team.folders.map((item, index) => {
            const completed = item.leads.filter(lead => lead.done).length;
            const artwork: ArtPreset[] = ['aurora', 'ember', 'dusk', 'verdant', 'glacier'];
            return <article className={`lead-folder-item teams-glass-folder ${folder?.id === item.id ? 'selected' : ''}`} key={item.id}>
              <GlassFolder width={280} label={item.name} tone="dark" art={artwork[index % artwork.length]} seed={hashString(item.id)} action="more"
                appearDelay={Math.min(index, 6) * 70} meta={`${item.leads.length} leads · ${completed} done`}
                files={item.leads.slice(0, 3).map(lead => ({ name: lead.data.name, meta: lead.data.phones.length ? 'Phone available' : 'Business lead' }))}
                open={openedFolderId === item.id}
                onOpenChange={value => { if (busy) return; if (value) chooseFolder(item.id); else setOpenedFolderId(''); }}
                onFileSelect={() => chooseFolder(item.id, true)}
                menu={[{ label: 'View shared leads', onSelect: () => chooseFolder(item.id, true) }]} />
              <div className="lead-folder-caption teams-glass-caption">
                <button onClick={() => chooseFolder(item.id, true)} disabled={!!busy} title={item.name}>{item.name}</button>
                <small>{item.leads.length} leads · {completed} done · Imported by {item.importedBy.name}</small>
                <progress max={item.leads.length} value={completed} aria-label={`${item.name}: ${completed} of ${item.leads.length} done`} />
                <div><button className="workspace-button" disabled={!!busy} onClick={() => chooseFolder(item.id, true)}><FolderOpen size={13} />Open shared folder</button>{folder?.id === item.id && <span className="teams-folder-selected">Selected</span>}</div>
              </div>
            </article>;
          })}</div>
          {folder && <section id="team-shared-folder-detail" className="teams-panel teams-lead-panel"><div className="teams-section-title"><div><h3>{folder.name}</h3><p>{folder.method === 'maps' ? 'Google Places' : 'Website Copyright'} · Imported {date(folder.importedAt)}</p></div><span>{folder.leads.filter(lead => lead.done).length} / {folder.leads.length} done</span></div>
            <div className="teams-filters"><label><Search size={15} /><input aria-label="Search team leads" placeholder="Search businesses, addresses, or phone numbers" value={query} onChange={event => setQuery(event.target.value)} /></label><select aria-label="Filter by lead status" value={status} onChange={event => setStatus(event.target.value)}><option value="all">All leads</option><option value="open">Open leads</option><option value="done">Done leads</option></select></div>
            <div className={`teams-lead-layout ${activeLead ? 'has-notes' : ''}`}><div className="teams-table-wrap"><table className="teams-table"><thead><tr><th>Status</th><th>Business</th><th>Contact</th><th>Notes</th></tr></thead><tbody>{filtered.map(lead => <tr key={lead.id} className={activeLead?.id === lead.id ? 'selected' : ''}><td><label className="teams-status"><input type="checkbox" checked={lead.done} disabled={!!busy} onChange={() => void updateLead(lead, 'status')} aria-label={`Mark ${lead.data.name} as ${lead.done ? 'open' : 'done'}`} /><span>{busy === `status:${lead.id}` ? 'Saving…' : lead.done ? 'Done' : 'Open'}</span></label>{lead.updatedBy && <small className="teams-status-by" title={lead.updatedAt ? date(lead.updatedAt) : undefined}>{lead.updatedBy.name}</small>}</td><td><button className="teams-lead-name" onClick={() => { setLeadId(lead.id); setNote(''); }}>{lead.data.name}</button><small>{lead.data.address || 'No address listed'}</small>{website(lead.data.website) && <a href={website(lead.data.website)} target="_blank" rel="noreferrer">Visit website ↗</a>}</td><td>{lead.data.phones.length ? lead.data.phones.map(phone => <a key={phone} href={`tel:${phone.replace(/[^+\d]/g, '')}`}>{phone}</a>) : <span className="teams-muted">No phone listed</span>}</td><td><button className="teams-notes-button" onClick={() => { setLeadId(lead.id); setNote(''); }} aria-label={`Notes for ${lead.data.name}: ${lead.notes.length}`}><MessageSquare size={15} />{lead.notes.length}</button></td></tr>)}</tbody></table>{!filtered.length && <p className="teams-empty">No leads match this filter.</p>}</div>
              {activeLead && <aside ref={notePanel} className="teams-notes" aria-label={`Notes for ${activeLead.data.name}`}><div className="teams-notes-heading"><div><span>LEAD CONVERSATION</span><h4>{activeLead.data.name}</h4></div><button className="teams-icon-button" aria-label="Close notes" onClick={() => { setLeadId(''); setNote(''); }}><X size={17} /></button></div><div className="teams-note-list">{activeLead.notes.length ? activeLead.notes.map(item => <article key={item.id}><header><strong title={item.author.email}>{item.author.name}</strong><span className="teams-note-email">{item.author.email}</span><time dateTime={item.createdAt}>{date(item.createdAt)}</time></header><p>{item.text}</p></article>) : <p className="teams-muted">No notes yet. Add context for everyone working on this lead.</p>}</div><form onSubmit={event => { event.preventDefault(); if (note.trim()) void updateLead(activeLead, 'note', note.trim()); }}><label htmlFor="team-lead-note">Add a note</label><textarea id="team-lead-note" maxLength={2000} required value={note} disabled={!!busy} onChange={event => setNote(event.target.value)} placeholder="Conversation, follow-up, or next step…" /><div><span>{note.length}/2000 · Visible to all members</span><button className="workspace-button teams-primary" type="submit" disabled={!!busy || !note.trim()}>{busy === `note:${activeLead.id}` ? <LoaderCircle size={14} className="spin" /> : <Send size={14} />}Post</button></div></form></aside>}
            </div>
          </section>}
        </>}
      </>}
    </>}
    {dialog && <TeamDialog mode={dialog} files={files} onClose={() => { if (!busy) setDialog(null); }} onSubmit={submitDialog} busy={busy === 'dialog'} error={dialogError} />}
  </section>;
}
