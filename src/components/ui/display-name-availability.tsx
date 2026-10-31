import { useEffect, useState } from 'react';

type Status = 'idle' | 'checking' | 'available' | 'taken' | 'error';
export function useDisplayNameAvailability(name: string) {
  const value = name.trim();
  const [result, setResult] = useState<{ value: string; status: Status }>({ value: '', status: 'idle' });
  useEffect(() => {
    if (!value) { setResult({ value, status: 'idle' }); return; }
    const controller = new AbortController();
    setResult({ value, status: 'checking' });
    const timer = window.setTimeout(async () => {
      try {
        const response = await fetch(`/api/workspace/profile/availability?username=${encodeURIComponent(value)}`, { credentials: 'same-origin', cache: 'no-store', signal: controller.signal });
        if (!response.ok) throw new Error();
        const data = await response.json(); if (typeof data.available !== 'boolean') throw new Error();
        if (!controller.signal.aborted) setResult({ value, status: data.available ? 'available' : 'taken' });
      } catch { if (!controller.signal.aborted) setResult({ value, status: 'error' }); }
    }, 350);
    return () => { controller.abort(); window.clearTimeout(timer); };
  }, [value]);
  const status: Status = value === result.value ? result.status : value ? 'checking' : 'idle';
  return { status, blocked: status === 'taken' || status === 'checking' };
}
export function DisplayNameStatus({ status, id }: { status: Status; id: string }) {
  const text = status === 'checking' ? 'Checking availability…' : status === 'available' ? 'This display name is available.' : status === 'taken' ? 'This display name is taken. Choose another.' : status === 'error' ? 'Availability check unavailable. Your name will be checked when you save.' : 'Choose a unique display name.';
  return <span id={id} className={`display-name-status display-name-${status}`} role="status">{text}</span>;
}
