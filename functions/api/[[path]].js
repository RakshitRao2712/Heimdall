// Keep browser sessions on the frontend origin. Provider credentials stay on Render.
const backendOrigin = 'https://heimdall-staging-wskj.onrender.com';
const wakePaths = new Set(['/api/auth/me', '/api/auth/csrf', '/api/auth/google/config', '/api/workspace/profile']);

export async function onRequest({ request }) {
  const incoming = new URL(request.url);
  const target = new URL(backendOrigin);
  target.pathname = incoming.pathname;
  target.search = incoming.search;
  const headers = new Headers(request.headers);
  for (const name of ['host', 'forwarded', 'x-forwarded-for', 'x-forwarded-host', 'x-forwarded-proto']) headers.delete(name);
  const options = {
    method: request.method,
    headers,
    redirect: 'manual',
    body: ['GET', 'HEAD'].includes(request.method) ? undefined : request.body,
  };
  try {
    let response = await fetch(target, options);
    // Only retry safe readiness reads, never account changes, payments or searches.
    const deadline = Date.now() + 85000;
    while (request.method === 'GET' && wakePaths.has(incoming.pathname) && [502, 503, 504].includes(response.status) && Date.now() < deadline) {
      await response.body?.cancel();
      await new Promise(resolve => setTimeout(resolve, 3000));
      response = await fetch(target, options);
    }
    const result = new Response(response.body, response);
    result.headers.set('Cache-Control', 'no-store');
    result.headers.set('X-Robots-Tag', 'noindex, noarchive');
    const location = result.headers.get('Location');
    if (location) {
      const redirect = new URL(location, target);
      if (redirect.origin === backendOrigin) {
        redirect.protocol = incoming.protocol;
        redirect.host = incoming.host;
        result.headers.set('Location', redirect.href);
      }
    }
    return result;
  } catch {
    return Response.json({ detail: 'Cannot connect to your workspace. Please retry shortly.' }, {
      status: 503,
      headers: { 'Cache-Control': 'no-store', 'Retry-After': '10', 'X-Robots-Tag': 'noindex, noarchive' },
    });
  }
}
