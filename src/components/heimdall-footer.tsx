import PushpinTearFooter from '@/components/ui/pushpin-tear-footer';
import { linkedinUrl, supportEmail } from '@/components/policy-links';

export function HeimdallFooter() {
  return <div className="heimdall-tear-footer" id="footer">
    <PushpinTearFooter
      brand="heimdall."
      name="Heimdall"
      logoHref="/"
      logo={<img src="/images/heimdall-logo.svg" alt="Heimdall" width={44} height={48} />}
      sheet="#101010"
      sheetTexture="/images/heimdall-paper.webp"
      sheetInk="#eeeeee"
      paper="#f1ede4"
      paperInk="#1b1712"
      seed={23}
      email={supportEmail}
      newsletter={null}
      status={{ label: 'free access · your API keys', href: '/pricing' }}
      mark="DISCOVER / CONNECT"
      timeZone="Asia/Kolkata"
      timeLabel="IST"
      socials={[
        { icon: 'mail', label: 'Email Heimdall', href: `mailto:${supportEmail}` },
        { icon: 'linkedin', label: 'Heimdall on LinkedIn', href: linkedinUrl },
      ]}
      columns={[
        { title: 'discover', links: [
          { label: 'Google Places', href: '/google-places' },
          { label: 'Foursquare', href: '/foursquare' },
          { label: 'How it works', href: '/how-it-works' },
          { label: 'Guides', href: '/guides' },
        ], cta: { label: 'start.', href: '/dashboard' } },
        { title: 'Heimdall', links: [
          { label: 'About', href: '/about' },
          { label: 'Free access', href: '/pricing' },
          { label: 'Contact', href: '/contact' },
          { label: 'Refund policy', href: '/refund' },
        ], cta: { label: 'say hi.', href: `mailto:${supportEmail}` } },
      ]}
      sideLinks={[
        { icon: 'shield', label: 'privacy', href: '/privacy' },
        { icon: 'doc', label: 'terms', href: '/terms' },
      ]}
      chat={{ label: 'email us', href: `mailto:${supportEmail}` }}
      copyright="© 2026 Heimdall"
      credit="Your next customer, waiting to be found."
      handle={{ label: 'heimdall.life', href: '/' }}
    />
  </div>;
}
