import ImmersiveFullscreenNav from '@/components/ui/immersive-full-screen-nav';

export const publicLinks = [
  { label: 'Pricing', href: '/pricing' },
  { label: 'How It Works', href: '/how-it-works' },
  { label: 'Google Places Setup', href: '/google-places-setup' },
];

export function SiteNav() {
  return <ImmersiveFullscreenNav
    navConfig={{ brand: 'Heimdall', brandHref: '/', overlayBg: '#101014', clipOrigin: 'left', headerClassName: 'hero-nav-header' }}
    navContent={{
      agencyName: 'Heimdall',
      tagline: 'Find local businesses. Discover website opportunities. Connect with your next customer.',
      location: 'Google Places + Copyright Search',
      links: publicLinks,
      images: [
        'https://cdn.21st.dev/assets/mirror/ba/baa678cbac1a29e773743a10ff421b226cf0e9b5d786ebf34415063d6dabc02b.jpg',
        'https://cdn.21st.dev/assets/mirror/ef/ef696854debc5d2d40cbe03ea0c3a04e29689529cf2f4bfe90c1c5978f00579a.jpg',
      ],
      socials: [{ type: 'instagram', href: '#' }, { type: 'twitter', href: '#' }, { type: 'linkedin', href: '#' }],
    }}
  />;
}
