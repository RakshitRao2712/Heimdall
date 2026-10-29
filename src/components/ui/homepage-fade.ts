import { useLayoutEffect } from 'react';
import { useIntroReady } from './site-intro';

/** Opacity only: preserve the tunnel's sticky layout and existing 3D transforms. */
export function useHomepageFade() {
  const ready = useIntroReady();
  useLayoutEffect(() => {
    if (!ready || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const selectors = [
      '.first-hero .hero-nav-header', '.first-hero .hero',
      '.hero-font-pair > span', '.hero-description > p', '.hero-actions',
      '.tunnel-demo h1', '.tunnel-demo h1 + p', '.tunnel-demo p:first-child',
      '.features-intro h2', '.features-subheading', '#features [data-slot="card"]',
      '#workflow .ib-tag', '#workflow .ib-h2', '#workflow .ib-card',
      '.page .lifetime-card', '.page .lifetime-eyebrow', '.page .lifetime-copy h2',
      '.page .lifetime-copy > p', '.page .lifetime-detail', '.page .lifetime-workflow',
    ];
    const elements = Array.from(document.querySelectorAll<HTMLElement>(selectors.join(',')));
    const animations = new Set<Animation>();
    const observer = new IntersectionObserver(entries => {
      let stagger = 0;
      entries.forEach(entry => {
        if (!entry.isIntersecting) return;
        const element = entry.target as HTMLElement;
        observer.unobserve(element);
        const animation = element.animate([{ opacity: 0 }, { opacity: 1 }], {
          duration: 850, delay: Math.min(stagger++ * 85, 425),
          easing: 'cubic-bezier(.22,1,.36,1)', fill: 'both',
        });
        animations.add(animation);
        animation.onfinish = () => {
          element.style.removeProperty('opacity');
          animation.cancel();
          animations.delete(animation);
        };
      });
    }, { threshold: 0.08 });
    elements.forEach(element => { element.style.opacity = '0'; observer.observe(element); });
    return () => {
      observer.disconnect();
      animations.forEach(animation => animation.cancel());
      elements.forEach(element => element.style.removeProperty('opacity'));
    };
  }, [ready]);
}
