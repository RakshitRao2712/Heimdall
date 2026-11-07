import { useEffect, useState } from 'react';
import { GlassReveal21st } from '@/components/ui/liquid-glass-logo-reveal';

export function WorkspaceConnection({ immediate = false, overlay = false }: { immediate?: boolean; overlay?: boolean }) {
  const [visible, setVisible] = useState(immediate);
  useEffect(() => {
    if (immediate) return;
    const timer = window.setTimeout(() => setVisible(true), 8000);
    return () => window.clearTimeout(timer);
  }, [immediate]);
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    const timer = window.setTimeout(() => setSlow(true), 90000);
    return () => window.clearTimeout(timer);
  }, []);

  if (!visible) return overlay ? null : <div className="route-loading" role="status">Connecting…</div>;

  return <main className="workspace-connection">
    <GlassReveal21st className="connection-reveal" loop={false} style={{ pointerEvents: 'none' }} />
    <div className="connection-progress">
      <div className="connection-track" aria-hidden="true"><span/></div>
      <p role="status" aria-live="polite">Connecting…</p>
      {slow && <div className="connection-retry"><p>This is taking longer than expected.</p><button onClick={() => window.location.reload()}>Retry connection</button><a href="/">Back to home</a></div>}
    </div>
  </main>;
}
