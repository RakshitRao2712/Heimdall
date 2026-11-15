import { useEffect, useRef, useState } from 'react';
import { Pause, Play, Volume2, VolumeX } from 'lucide-react';

export function ProductVideo() {
  const video = useRef<HTMLVideoElement>(null);
  const visible = useRef(false);
  const wantsPlayback = useRef(true);
  const [playing, setPlaying] = useState(false);
  const [muted, setMuted] = useState(true);

  useEffect(() => {
    const element = video.current;
    if (!element) return;
    const syncPlayback = () => {
      if (visible.current && !document.hidden && wantsPlayback.current) {
        void element.play().catch(() => setPlaying(false));
      } else element.pause();
    };
    const observer = new IntersectionObserver(([entry]) => {
      visible.current = entry.isIntersecting && entry.intersectionRatio >= 0.25;
      syncPlayback();
    }, { threshold: [0, 0.25] });
    observer.observe(element);
    document.addEventListener('visibilitychange', syncPlayback);
    return () => {
      observer.disconnect();
      document.removeEventListener('visibilitychange', syncPlayback);
      element.pause();
    };
  }, []);

  async function togglePlayback() {
    const element = video.current;
    if (!element) return;
    wantsPlayback.current = element.paused;
    if (!wantsPlayback.current) element.pause();
    else if (visible.current && !document.hidden) {
      try { await element.play(); } catch { setPlaying(false); }
    }
  }
  function toggleAudio() {
    const element = video.current;
    if (!element) return;
    const nextMuted = !element.muted;
    element.muted = nextMuted;
    element.volume = 1;
    setMuted(nextMuted);
    // Clicking sound is a user gesture: resume here if autoplay was blocked.
    if (!nextMuted && visible.current && !document.hidden) {
      wantsPlayback.current = true;
      void element.play().catch(() => setPlaying(false));
    }
  }
  return <section id="how-it-works" className="product-video-section" aria-label="See Heimdall in action">
    <div className="product-video-frame">
      <video ref={video} src="/videos/heimdall-overview-audio.mp4" loop muted={muted} playsInline preload="metadata" poster="/images/heimdall-video-poster.webp" onPlay={() => {
        if (!visible.current || document.hidden || !wantsPlayback.current) video.current?.pause();
        else setPlaying(true);
      }} onPause={() => setPlaying(false)} aria-label="Heimdall product walkthrough">
      </video>
      <button type="button" className="product-video-toggle product-video-audio" onClick={toggleAudio} aria-label={muted ? 'Unmute video' : 'Mute video'}>{muted ? <VolumeX size={18} /> : <Volume2 size={18} />}</button>
      <button type="button" className="product-video-toggle" onClick={() => void togglePlayback()} aria-label={playing ? 'Pause video' : 'Play video'}>{playing ? <Pause size={18} /> : <Play size={18} />}</button>
    </div>
  </section>;
}
