import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { GlassReveal21st } from './liquid-glass-logo-reveal';

const IntroReadyContext = createContext(true);
export const useIntroReady = () => useContext(IntroReadyContext);

/** Reveal on homepage visits and once after successful authentication. */
export function SiteIntro({ children, play = window.location.pathname === '/' }: { children: ReactNode; play?: boolean }) {
  const [visible, setVisible] = useState(play);
  const reduce = useReducedMotion();
  const [compact] = useState(() => window.matchMedia('(max-width: 767px), (prefers-reduced-motion: reduce)').matches);
  const finish = useCallback(() => setVisible(false), []);

  useEffect(() => {
    if (!visible) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    // Always release the page even if graphics initialization fails.
    const timeout = window.setTimeout(finish, compact || reduce ? 900 : 7000);
    return () => {
      document.body.style.overflow = previous;
      window.clearTimeout(timeout);
    };
  }, [visible, compact, reduce, finish]);

  return <>
    <IntroReadyContext.Provider value={!visible}><div inert={visible} aria-hidden={visible || undefined}>{children}</div></IntroReadyContext.Provider>
    <AnimatePresence>
      {visible && <motion.div
        key="heimdall-intro"
        className={`site-intro${compact ? ' site-intro-compact' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label="Welcome to Heimdall"
        exit={{ opacity: 0 }}
        transition={{ duration: reduce ? 0 : 0.45 }}
        onKeyDown={event => { if (event.key === 'Escape') finish(); }}
      >
        {compact ? <motion.img src="/images/heimdall-logo.svg" width="120" height="120" alt="Heimdall" initial={{opacity:0,scale:.9}} animate={{opacity:1,scale:1}} transition={{duration:.5}} /> : <GlassReveal21st className="h-svh min-h-[420px] w-full" onComplete={finish} />}
      </motion.div>}
    </AnimatePresence>
  </>;
}
