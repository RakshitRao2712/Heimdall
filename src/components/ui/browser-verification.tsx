import { useEffect, useRef, useState } from 'react';
import { workspaceMutation } from '@/auth/session';
import { RefreshCw, ShieldCheck } from 'lucide-react';

type Verification = { waiting: boolean; challengeId?: string; expiresAt?: string; message?: string; appliedInputs?: number; localWindow?: boolean; diagnostic?: string };
export function BrowserVerification({ active, onCancel }: { active: boolean; onCancel: () => void }) {
  const [status, setStatus] = useState<Verification>({ waiting: false });
  const [error, setError] = useState('');
  const [tick, setTick] = useState(0); const [busy, setBusy] = useState(false); const [text, setText] = useState('');
  const sendQueue = useRef<Promise<void>>(Promise.resolve()); const queued = useRef(0);
  const [frameSize, setFrameSize] = useState({ width: 1280, height: 757 });
  const [inputNotice, setInputNotice] = useState('');
  useEffect(() => {
    if (!active) { setStatus({ waiting: false }); return; }
    const abort = new AbortController(); let fetching = false;
    const poll = async () => {
      if (fetching) return; fetching = true;
      try {
        const response = await fetch('/api/workspace/browser-verification', { signal: abort.signal, credentials: 'same-origin', cache: 'no-store' });
        if (!response.ok) throw new Error('Cannot load Google verification. Check your session and retry.');
        const value: Verification = await response.json();
        if (!abort.signal.aborted) { setStatus(value); setTick(previous => previous + 1); }
      } catch (failure) { if (!abort.signal.aborted) setError(failure instanceof Error ? failure.message : 'Verification unavailable.'); }
      finally { fetching = false; }
    };
    void poll(); const timer = window.setInterval(() => void poll(), 1000);
    return () => { abort.abort(); window.clearInterval(timer); };
  }, [active]);
  function send(path: 'input' | 'resume' | 'reload', data: Record<string, unknown> = {}) {
    if (!status.challengeId) return;
    if (queued.current >= 16) { setError('Please wait for the browser to catch up.'); return; }
    const challengeId = status.challengeId;
    queued.current++; setBusy(true); setError(''); setInputNotice('Sending to the browser…');
    sendQueue.current = sendQueue.current.then(async () => {
      const controller = new AbortController(); const timeout = window.setTimeout(() => controller.abort(), 10000);
      try {
        await workspaceMutation(`browser-verification/${path}`, 'POST', { challengeId, ...data }, controller.signal);
        setInputNotice(path === 'reload' ? 'Reloading Google verification…' : path === 'resume' ? 'Checking Google verification…' : 'Input sent. Wait for the browser image to update.');
      } catch (failure) { setError(controller.signal.aborted ? 'The browser control request timed out. Try again.' : failure instanceof Error ? failure.message : 'Could not update verification.'); }
      finally { clearTimeout(timeout); queued.current--; setBusy(queued.current > 0); }
    });
  }
  if (!active || !status.waiting) return null;
  const expires = status.expiresAt ? Math.max(0, Math.ceil((Date.parse(status.expiresAt) - Date.now()) / 1000)) : 0;
  return <section aria-labelledby="browser-verification-title" className="browser-verification-panel">
      <header><div><h2 id="browser-verification-title"><ShieldCheck size={22}/>Complete Google verification</h2><p>{status.message} · {Math.floor(expires / 60)}:{String(expires % 60).padStart(2, '0')} remaining</p></div></header>
      <p className="browser-verification-help">Your search is paused. Click the “I’m not a robot” checkbox in the live browser below and complete Google’s challenge, then choose Resume search.</p>
      {status.diagnostic && <p className="browser-verification-input-notice" role="status">{status.diagnostic}</p>}
      <p className="browser-verification-input-notice" role={error ? 'alert' : 'status'}>{busy ? 'Sending to the browser…' : error || inputNotice || (status.appliedInputs ? `Browser received ${status.appliedInputs} input${status.appliedInputs === 1 ? '' : 's'}.` : 'Click the Google checkbox in the image to begin verification.')}</p>
      <div className="browser-verification-viewport"><img draggable={false} width={frameSize.width} height={frameSize.height} onLoad={event => { const image = event.currentTarget; if(image.naturalWidth && image.naturalHeight) setFrameSize(previous => previous.width === image.naturalWidth && previous.height === image.naturalHeight ? previous : { width: image.naturalWidth, height: image.naturalHeight }); }} alt="Live Google verification browser. Click the image to interact with this session." src={`/api/workspace/browser-verification/frame?challengeId=${encodeURIComponent(status.challengeId || '')}&frame=${tick}`} onPointerDown={event => { if (event.button !== 0) return; event.preventDefault(); const rect = event.currentTarget.getBoundingClientRect(); send('input', { kind: 'click', x: Math.min(.999999, Math.max(0, (event.clientX - rect.left) / rect.width)), y: Math.min(.999999, Math.max(0, (event.clientY - rect.top) / rect.height)) }); }}/></div>
      <div className="browser-verification-controls"><form onSubmit={event => { event.preventDefault(); if (text) { void send('input', { kind: 'text', text }); setText(''); } }}><input aria-label="Text to type into Google verification" maxLength={200} placeholder="Text to enter in the browser…" value={text} onChange={event => setText(event.target.value)}/><button className="workspace-button" disabled={busy || !text}>Send text</button></form><div>{['TAB', 'ENTER', 'SPACE', 'BACKSPACE'].map(key => <button key={key} className="workspace-button" disabled={busy} onClick={() => void send('input', { kind: 'key', text: key })}>{key === 'BACKSPACE' ? '⌫' : key[0] + key.slice(1).toLowerCase()}</button>)}<button className="workspace-button" disabled={busy} onClick={() => void send('input', { kind: 'scroll', y: -400 })}>Scroll ↑</button><button className="workspace-button" disabled={busy} onClick={() => void send('input', { kind: 'scroll', y: 400 })}>Scroll ↓</button></div></div>
<footer><span>Private session · expires automatically</span><button className="workspace-button" disabled={busy} onClick={() => send('reload')}>Reload verification</button><button className="workspace-button" onClick={onCancel}>Cancel search</button><button className="workspace-button primary" disabled={busy} onClick={() => void send('resume')}><RefreshCw size={15}/>Resume search</button></footer>
    </section>;
}
