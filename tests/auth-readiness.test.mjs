import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readinessRequest } from '../src/auth/readiness.ts';

test('recovers after a sleeping backend responds unavailable', async () => {
  const original = globalThis.fetch;
  let calls = 0;
  globalThis.fetch = async () => ++calls === 1 ? new Response('', { status: 503 }) : Response.json({ enabled: true });
  try {
    assert.deepEqual(await (await readinessRequest('/api/auth/google/config')).json(), { enabled: true });
    assert.equal(calls, 2);
  } finally { globalThis.fetch = original; }
});
test('does not retry rejected sessions or account mutations', async () => {
  const original = globalThis.fetch;
  let calls = 0;
  globalThis.fetch = async () => { calls++; return new Response(null, { status: 401 }); };
  try {
    assert.equal((await readinessRequest('/api/auth/me')).status, 401);
    await assert.rejects(readinessRequest('/api/auth/login'), /Unsupported/);
    assert.equal(calls, 1);
  } finally { globalThis.fetch = original; }
});
test('cancels retries when the login page unmounts', async () => {
  const original = globalThis.fetch;
  const controller = new AbortController();
  let calls = 0;
  globalThis.fetch = async () => { calls++; controller.abort(); throw new TypeError('offline'); };
  try {
    await assert.rejects(readinessRequest('/api/auth/google/config', controller.signal), { name: 'AbortError' });
    assert.equal(calls, 1);
  } finally { globalThis.fetch = original; }
});
