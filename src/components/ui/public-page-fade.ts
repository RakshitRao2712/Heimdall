import { useEffect } from 'react';

/** Reveal once per page visit; keep content readable without JavaScript or motion. */
export function usePublicPageFade(path: string) {
  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    const elements = Array.from(document.querySelectorAll<HTMLElement>(
      '.info-main > h1, .info-main > .info-eyebrow, .info-main > .info-intro, .info-card, .info-note, .info-main > section, .info-main > aside, .policy-sidebar, .guide-screenshots figure, .menu-footer'
    )).filter(element => !element.parentElement?.closest('.info-card, .info-note, .info-main > section, .info-main > aside'));
    const animations = new Set<Animation>();
    const observer = new IntersectionObserver(entries => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        const element = entry.target as HTMLElement;
        observer.unobserve(element);
        const animation = element.animate([{opacity: 0}, {opacity: 1}], {duration: 700, easing: 'ease-out', fill: 'both'});
        animations.add(animation);
        animation.onfinish = () => { element.style.removeProperty('opacity'); animation.cancel(); animations.delete(animation); };
      }
    }, {threshold: 0, rootMargin: '0px 0px -24px 0px'});
    elements.forEach(element => { element.style.opacity = '0'; observer.observe(element); });
    return () => {observer.disconnect(); animations.forEach(animation => animation.cancel()); elements.forEach(element => element.style.removeProperty('opacity'));};
  }, [path]);
}
