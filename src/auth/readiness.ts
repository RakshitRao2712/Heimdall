// Only these idempotent reads may be retried while the backend wakes up.
const paths = new Set(['/api/auth/me', '/api/auth/csrf', '/api/auth/google/config', '/api/workspace/profile']);
export async function readinessRequest(path: string, signal?: AbortSignal): Promise<Response> {
  if (!paths.has(path)) throw new Error('Unsupported readiness request');
  const deadline = Date.now() + 100_000;
  while (true) {
    signal?.throwIfAborted();
    const attempt = AbortSignal.any([...(signal ? [signal] : []), AbortSignal.timeout(Math.min(12_000, Math.max(1, deadline - Date.now())))]);
    try {
      const response = await fetch(path, { credentials: 'same-origin', cache: 'no-store', signal: attempt });
      if (![502, 503, 504].includes(response.status) || Date.now() >= deadline) return response;
      await response.body?.cancel();
    } catch (error) {
      signal?.throwIfAborted();
      if (Date.now() >= deadline) throw new Error(['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname)
        ? 'Cannot reach the local backend. Start it on port 8080, then try signing in again.'
        : 'Cannot connect to Heimdall right now. Please try again shortly.');
    }
    await new Promise<void>((resolve, reject) => {
      const abort = () => { clearTimeout(timer); reject(signal?.reason); };
      const timer = setTimeout(() => { signal?.removeEventListener('abort', abort); resolve(); }, Math.min(2_000, Math.max(0, deadline - Date.now())));
      signal?.addEventListener('abort', abort, { once: true });
      if (signal?.aborted) abort();
    });
  }
}
