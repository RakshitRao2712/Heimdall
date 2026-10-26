import { Features } from '@/components/blocks/features-8';
import ImmersiveFullscreenNav from '@/components/ui/immersive-full-screen-nav';
import CurtainSplitHero from '@/components/ui/perspective-tunnel-hero';
import { TiltedGridHero } from '@/components/ui/tilted-grid-hero';

const CDN = 'https://pub-940ccf6255b54fa799a9b01050e6c227.r2.dev';
const IMAGES = [
  { src: `${CDN}/stock-images/767d99bb371a54d0d36751e8cecae43c.jpg`, alt: 'Diver silhouetted inside a sunset seascape shaped like a profile' },
  { src: `${CDN}/gradients/hero_gradient/hero-gradients-01.png`, alt: 'Soft multi-tone gradient wash' },
  { src: `${CDN}/stock-images/821d815affa6496c39cbdeeec7a84603.jpg`, alt: 'Double-exposure portrait blended with a city skyline at dusk' },
  { src: `${CDN}/gradients/moon/moon-grade-03.png`, alt: 'Moon-toned gradient' },
  { src: `${CDN}/stock-images/937438c560ada1c83317f2c11b3454b0.jpg`, alt: 'Motion-blurred side-profile portrait against a deep orange backdrop' },
  { src: `${CDN}/gradients/shade_shiters/shade-shifters-05.png`, alt: 'Shifting shade gradient' },
  { src: `${CDN}/stock-images/98f89cb9994f5c382ab964062c4039db.jpg`, alt: 'Figure holding a racket that dissolves into a swirling colourful cloud' },
  { src: `${CDN}/gradients/shade_shiters/shade-shifters-09.png`, alt: 'Shifting shade gradient' },
  { src: `${CDN}/stock-images/ddcbee38be8b7274e19e132d7ab35b53.jpg`, alt: 'Hand gesture with a colourful cutout of a bird flying through the fingers' },
  { src: `${CDN}/gradients/moon/moon-grade-06.png`, alt: 'Moon-toned gradient' },
];

export default function App() {
  return (
    <main className="page">
      <section className="first-hero">
      <ImmersiveFullscreenNav
        navConfig={{ brand: "Northline", brandHref: "#", overlayBg: "#101014", clipOrigin: "left", headerClassName: "hero-nav-header" }}
        navContent={{
          agencyName: "Northline Studio",
          tagline: "Brand and product design for early-stage teams.",
          location: "Lisbon, Portugal",
          links: [{ label: "Work", href: "#" }, { label: "Studio", href: "#" }, { label: "Journal", href: "#" }, { label: "Contact", href: "#" }],
          images: ["https://cdn.21st.dev/assets/mirror/ba/baa678cbac1a29e773743a10ff421b226cf0e9b5d786ebf34415063d6dabc02b.jpg", "https://cdn.21st.dev/assets/mirror/ef/ef696854debc5d2d40cbe03ea0c3a04e29689529cf2f4bfe90c1c5978f00579a.jpg"],
          socials: [{ type: "instagram", href: "#" }, { type: "twitter", href: "#" }, { type: "linkedin", href: "#" }],
        }}
      />
      <TiltedGridHero images={IMAGES} className="hero">
        <div className="hero-content">
          <div className="hero-title"><h1>Every frame,<br />brought into focus.</h1></div>
          <p>Images curve in from one edge, flatten out at the centre, then fade away round the other.</p>
        </div>
      </TiltedGridHero>
      </section>
      <section className="tunnel-demo" aria-label="Perspective tunnel">
        <div className="tunnel-demo-inner"><CurtainSplitHero /></div>
      </section>
      <Features />
    </main>
  );
}
