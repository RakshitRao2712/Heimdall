import guides from '@/seo/troubleshooting.json';
export function Troubleshooting({ path }: { path: string }) {
  const guide = guides[path as keyof typeof guides];
  if (!guide) return null;
  return <section className="setup-help" id="troubleshooting" aria-labelledby="setup-help-heading"><h2 id="setup-help-heading">Troubleshoot {guide.provider}</h2><p>Start with the message shown in Heimdall, then follow the relevant checks.</p><div className="info-grid">{guide.items.map(item => <section className="info-card" key={item.question}><h3>{item.question}</h3><ol>{item.steps.map(step => <li key={step}>{step}</li>)}</ol></section>)}</div><a href={guide.source} target="_blank" rel="noreferrer">Official {guide.provider} setup documentation ↗</a><p>Still stuck? <a href="/contact">Contact support</a> with the error text, without credentials.</p></section>;
}
