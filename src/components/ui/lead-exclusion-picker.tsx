import { useEffect, useState } from 'react';
import { FolderOpen, RefreshCw } from 'lucide-react';

type FileOption = { id: string; name: string; method: string; leadCount: number };
export function LeadExclusionPicker({ selected, onChange, disabled }: {
  selected: string[]; onChange: (ids: string[]) => void; disabled: boolean;
}) {
  const [files, setFiles] = useState<FileOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [filter, setFilter] = useState('');
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError('');
    void fetch('/api/workspace/files/options', { credentials: 'same-origin', cache: 'no-store', signal: controller.signal })
      .then(async response => {
        if (!response.ok) throw new Error('Could not load saved files. Retry to choose files.');
        const data: unknown = await response.json();
        if (!Array.isArray(data) || !data.every(file => typeof file.id === 'string' && typeof file.name === 'string' && typeof file.leadCount === 'number')) throw new Error('Could not read saved files. Please retry.');
        if (!controller.signal.aborted) setFiles(data);
      }).catch(reason => { if (!controller.signal.aborted) setError(reason.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [revision]);
  const matching = files.filter(file => file.name.toLowerCase().includes(filter.toLowerCase()));
  return <details className="lead-exclusion-picker">
    <summary><FolderOpen size={17}/><span>Skip leads from saved files</span><span className="workspace-chip">{selected.length ? `${selected.length} selected` : 'Optional'}</span></summary>
    <div className="lead-exclusion-body">
      <p>Select files containing businesses you already have. Matching businesses won’t count toward your new lead target.</p>
      <div className="lead-exclusion-toolbar">
        <input aria-label="Find files to exclude" placeholder="Find a saved file…" value={filter} disabled={disabled} onChange={event => setFilter(event.target.value)}/>
        <button type="button" className="workspace-button" disabled={disabled || loading} onClick={() => setRevision(value => value + 1)} aria-label="Refresh files to exclude"><RefreshCw size={14}/></button>
        {!!selected.length && <button type="button" className="workspace-button" disabled={disabled} onClick={() => onChange([])}>Clear</button>}
      </div>
      {loading ? <p role="status">Loading saved files…</p> : error ? <p role="alert">{error}</p> : !files.length ? <p>Save a search to your lead files first, then select it here on your next search.</p> : <div className="lead-exclusion-options">{matching.map(file => <label key={file.id}>
        <input type="checkbox" checked={selected.includes(file.id)} disabled={disabled} onChange={event => onChange(event.target.checked ? [...selected, file.id] : selected.filter(id => id !== file.id))}/>
        <span>{file.name}<small>{file.leadCount} leads · {file.method === 'maps' ? 'Google Places' : 'Website search'}</small></span>
      </label>)}{!matching.length && <p>No saved files match this name.</p>}</div>}
      <p className="lead-exclusion-note">New matches depend on provider coverage. A search may return fewer leads after skipping saved businesses.</p>
    </div>
  </details>;
}
