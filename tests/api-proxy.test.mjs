import { test } from 'node:test';
import assert from 'node:assert/strict';
import { onRequest } from '../functions/api/[[path]].js';

test('keeps session cookies and OAuth redirects on the public frontend', async () => {
  const original = globalThis.fetch;
  globalThis.fetch = async (target, options) => {
    assert.equal(target.href, 'https://backend.heimdall.life/api/auth/google/authorize/google');
    assert.equal(options.headers.get('cookie'), 'JSESSIONID=synthetic');
    assert.equal(options.headers.get('x-forwarded-host'), null);
    assert.equal(options.redirect, 'manual');
    return new Response(null, { status: 302, headers: { Location: 'https://backend.heimdall.life/api/auth/google/callback?code=synthetic', 'Set-Cookie': 'JSESSIONID=rotated; Path=/; Secure; HttpOnly; SameSite=Lax' } });
  };
  try {
    const response = await onRequest({ request: new Request('https://heimdall.life/api/auth/google/authorize/google', { headers: { cookie: 'JSESSIONID=synthetic', 'x-forwarded-host': 'untrusted.example' } }) });
    assert.equal(response.headers.get('location'), 'https://heimdall.life/api/auth/google/callback?code=synthetic');
    assert.match(response.headers.get('set-cookie'), /Secure; HttpOnly/);
    assert.equal(response.headers.get('cache-control'), 'no-store');
  } finally { globalThis.fetch = original; }
});

test('forwards search bodies once without retrying failed mutations', async () => {
  const original = globalThis.fetch;
  let calls = 0;
  globalThis.fetch = async (target, options) => {
    calls++;
    assert.equal(target.pathname, '/api/workspace/search/maps');
    assert.equal(await new Response(options.body).text(), 'query=restaurants');
    return new Response('unavailable', { status: 503 });
  };
  try {
    const response = await onRequest({ request: new Request('https://heimdall.life/api/workspace/search/maps', { method: 'POST', body: 'query=restaurants' }) });
    assert.equal(response.status, 503);
    assert.equal(calls, 1);
  } finally { globalThis.fetch = original; }
});
