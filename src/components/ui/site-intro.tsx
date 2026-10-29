import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { GlassReveal21st } from './liquid-glass-logo-reveal';

const IntroReadyContext = createContext(true);
export const useIntroReady = () => useContext(IntroReadyContext);

/** Reveal on homepage visits and once after successful authentication. */
export function SiteIntro({ children }: { children: ReactNode }) {
  const [visible, setVisible] = useState(() => {
    const path = window.location.pathname;
    if (path === '/') return true;
    if (path !== '/dashboard') return false;
    try { return sessionStorage.getItem('heimdall-login-intro') === 'pending'; }
    catch { return false; }
  });

  useEffect(() => {
    if (window.location.pathname === '/dashboard') {
      try { sessionStorage.removeItem('heimdall-login-intro'); } catch { /* Storage may be unavailable. */ }
    }
  }, []);
  const reduce = useReducedMotion();
  const finish = useCallback(() => setVisible(false), []);

  useEffect(() => {
    if (!visible) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    // Always release the page even if graphics initialization fails.
    const timeout = window.setTimeout(finish, reduce ? 800 : 7000);
    return () => {
      document.body.style.overflow = previous;
      window.clearTimeout(timeout);
    };
  }, [visible, reduce, finish]);

  return <>
    <IntroReadyContext.Provider value={!visible}><div inert={visible} aria-hidden={visible || undefined}>{children}</div></IntroReadyContext.Provider>
    <AnimatePresence>
      {visible && <motion.div
        key="heimdall-intro"
        className="site-intro"
        role="dialog"
        aria-modal="true"
        aria-label="Welcome to Heimdall"
        exit={{ opacity: 0 }}
        transition={{ duration: reduce ? 0 : 0.45 }}
        onKeyDown={event => { if (event.key === 'Escape') finish(); }}
      >
        <GlassReveal21st className="h-svh min-h-[420px] w-full" onComplete={finish} />
      </motion.div>}
    </AnimatePresence>
  </>;
}
