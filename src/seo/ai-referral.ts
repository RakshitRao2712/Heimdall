const sources = ['chatgpt.com', 'chat.openai.com', 'perplexity.ai', 'claude.ai', 'copilot.microsoft.com', 'gemini.google.com', 'you.com', 'poe.com'];
/** Only a recognized public AI service origin, never a conversation path or query. */
export function safeAiReferrer(referrer: string): string {
  try {
    const url = new URL(referrer);
    if (url.protocol !== 'https:' || url.username || url.password || url.port) return '';
    const host = url.hostname.toLowerCase();
    const source = sources.find(domain => host === domain || host === `www.${domain}`);
    return source ? `https://${source}/` : '';
  } catch { return ''; }
}
