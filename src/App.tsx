import { DiscoveryLinks } from './pages/DiscoveryPage';
import brand from '@/seo/brand.json';
import { MethodComparison } from '@/components/method-comparison';
import { PageFAQ } from '@/components/page-faq';
import { useHomepageFade } from '@/components/ui/homepage-fade';
import { LifetimeOffer } from '@/components/lifetime-offer';
import { useSession } from "@/auth/session";
import { ShinyButton } from "@/components/ui/shiny-button";
import { CinematicFooter } from '@/components/ui/motion-footer';
import InkOrbitFeatures from '@/components/ui/ink-orbit-features';
import { Features } from '@/components/blocks/features-8';
import { SiteNav } from '@/components/site-nav';
import CurtainSplitHero from '@/components/ui/perspective-tunnel-hero';
import { TiltedGridHero } from '@/components/ui/tilted-grid-hero';

const IMAGES = [
  { src: `/images/discovery/cafe.webp`, alt: 'Independent café interior' },
  { src: `/images/discovery/dining.webp`, alt: 'Restaurant dining area' },
  { src: `/images/discovery/studio.webp`, alt: 'Creative studio workspace' },
  { src: `/images/discovery/coffee.webp`, alt: 'Coffee prepared at a local business' },
  { src: `/images/discovery/restaurant.webp`, alt: 'Restaurant interior' },
  { src: `/images/discovery/flowers.webp`, alt: 'Flower shop display' },
  { src: `/images/discovery/market.webp`, alt: 'Local market storefront' },
  { src: `/images/discovery/counter.webp`, alt: 'Café service counter' },
  { src: `/images/discovery/bakery.webp`, alt: 'Neighborhood bakery' },
  { src: `/images/discovery/cafe.webp`, alt: 'Independent café' },
];

export default function App() {
  const { user, loading } = useSession();
  useHomepageFade();
  return (
    <>
    <main className="page relative z-10 bg-background">
      <section id="home" className="first-hero">
      <SiteNav />
      <TiltedGridHero images={IMAGES} className="hero">
        <div className="hero-content">
          <div className="hero-title"><h1 className="hero-font-pair"><span className="hero-script">Your next</span><span className="hero-anton">customer,</span><span className="hero-heading-tail">waiting to be found.</span></h1></div>
          <div className="hero-description">
            <p>{brand.description}</p>
            <div className="hero-actions">
            <ShinyButton onClick={() => window.location.assign(user ? '/dashboard' : '/login')}>{loading ? 'Get started' : user ? 'Dashboard' : 'Get started'}</ShinyButton>
              <a className="hero-how-link" href="/how-it-works">How it works?</a>
            </div>
          </div>
        </div>
      </TiltedGridHero>
      </section>
      <section id="how-it-works" className="tunnel-demo" aria-label="How Heimdall works">
        <div className="tunnel-demo-inner"><CurtainSplitHero /></div>
      </section>
      <Features />
      <div id="workflow" className="w-full"><InkOrbitFeatures theme="dark" /></div>
      <MethodComparison />
      <LifetimeOffer />
      <PageFAQ path="/" /><section className="info-note"><h2>Find your discovery workflow</h2><DiscoveryLinks/></section>
    </main>
    <CinematicFooter />
    </>
  );
}
