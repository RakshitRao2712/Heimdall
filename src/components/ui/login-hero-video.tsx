import { useEffect, useRef } from 'react';

export function LoginHeroVideo({ src, poster }: { src: string; poster?: string }) {
  const ref = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    const video = ref.current;
    if (!video) return;
    const desktop = window.matchMedia('(min-width: 768px)');
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    let inView = false;
    let disposed = false;
    video.volume = 0.12;

    const syncPlayback = () => {
      if (!inView || !desktop.matches || reducedMotion.matches || document.hidden) {
        video.pause();
        return;
      }
      void video.play().catch(() => {
        // Some browsers require muted playback even after an interaction.
        if (disposed || !inView || !desktop.matches || reducedMotion.matches || document.hidden) return;
        video.muted = true;
        void video.play().catch(() => {});
      });
    };
    const enableQuietAudio = () => {
      if (!inView || !desktop.matches || document.hidden || reducedMotion.matches) return;
      video.volume = 0.12;
      video.muted = false;
      syncPlayback();
    };
    const observer = new IntersectionObserver(([entry]) => {
      inView = entry.isIntersecting;
      syncPlayback();
    }, { threshold: 0.1 });
    observer.observe(video);
    document.addEventListener('pointerdown', enableQuietAudio);
    document.addEventListener('keydown', enableQuietAudio);
    document.addEventListener('visibilitychange', syncPlayback);
    desktop.addEventListener('change', syncPlayback);
    reducedMotion.addEventListener('change', syncPlayback);
    return () => {
      disposed = true;
      observer.disconnect();
      video.pause();
      document.removeEventListener('pointerdown', enableQuietAudio);
      document.removeEventListener('keydown', enableQuietAudio);
      document.removeEventListener('visibilitychange', syncPlayback);
      desktop.removeEventListener('change', syncPlayback);
      reducedMotion.removeEventListener('change', syncPlayback);
    };
  }, [src]);

  return <video ref={ref} className="auth-hero-video" src={src} poster={poster}
    loop muted playsInline preload="metadata" disablePictureInPicture
    disableRemotePlayback aria-hidden="true" tabIndex={-1} />;
}
