import brand from '@/seo/brand.json';
import { MethodComparison } from '@/components/method-comparison';
import { PageFAQ } from '@/components/page-faq';
import { useHomepageFade } from '@/components/ui/homepage-fade';
import { LifetimeOffer } from '@/components/lifetime-offer';
import { useSession } from "@/auth/session";
import { ShinyButton } from "@/components/ui/shiny-button";
import { HeimdallFooter } from '@/components/heimdall-footer';
import InkOrbitFeatures from '@/components/ui/ink-orbit-features';
import { Features } from '@/components/blocks/features-8';
import { SiteNav } from '@/components/site-nav';
import { ProductVideo } from '@/components/product-video';
import { TiltedGridHero } from '@/components/ui/tilted-grid-hero';

const IMAGES = [
  { src: '/images/discovery/restaurant.webp', alt: 'Independent restaurant interior' },
  { src: '/images/discovery/dining.webp', alt: 'Restaurant dining room ready for guests' },
  { src: '/images/discovery/cafe.webp', alt: 'Neighborhood café interior' },
  { src: '/images/discovery/coffee.webp', alt: 'Freshly prepared café coffee' },
  { src: '/images/discovery/counter.webp', alt: 'Local café service counter' },
  { src: '/images/discovery/bakery.webp', alt: 'Neighborhood bakery' },
  { src: '/images/discovery/dining.webp', alt: 'Restaurant tables and seating' },
  { src: '/images/discovery/restaurant.webp', alt: 'Local restaurant space' },
  { src: '/images/discovery/cafe.webp', alt: 'Independent coffee shop' },
  { src: '/images/discovery/bakery.webp', alt: 'Local bakery business' },
];

export default function App() {
  const { user, loading } = useSession();
  useHomepageFade();
  return (
    <>
    <main className="page homepage-surface relative z-10 bg-background">
      <section id="home" className="first-hero">
      <SiteNav homepage />
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
      <ProductVideo />
      <Features />
      <div id="workflow" className="w-full"><InkOrbitFeatures theme="dark" /></div>
      <MethodComparison />
      <LifetimeOffer />
      <PageFAQ path="/" />
    </main>
    <HeimdallFooter />
    </>
  );
}
