"use client";

import { useEffect, useState } from 'react';
import { motion, AnimatePresence, useReducedMotion, type Transition } from 'framer-motion';
import { cn } from '@/lib/utils';

interface TextLoopProps {
  staticText?: string;
  rotatingTexts?: string[];
  className?: string;
  interval?: number;
  transition?: Transition;
  staticTextClassName?: string;
  rotatingTextClassName?: string;
  backgroundClassName?: string;
  cursorClassName?: string;
}

export default function TextLoop({
  staticText = 'Connect',
  rotatingTexts = ['Google Places.', 'your API key.', 'new opportunities.'],
  className,
  interval = 3000,
  transition = { duration: 0.8, ease: 'easeInOut' },
  staticTextClassName,
  rotatingTextClassName,
  backgroundClassName,
  cursorClassName,
}: TextLoopProps) {
  const [index, setIndex] = useState(0);
  const reduce = useReducedMotion();
  useEffect(() => {
    if (reduce || rotatingTexts.length < 2) return;
    const timer = window.setInterval(() => setIndex(previous => (previous + 1) % rotatingTexts.length), interval);
    return () => window.clearInterval(timer);
  }, [rotatingTexts.length, interval, reduce]);
  const text = rotatingTexts[index % rotatingTexts.length] ?? '';

  return <span className={cn('text-loop', className)}>
    <span className="sr-only">{staticText} {rotatingTexts[0]}</span>
    <span aria-hidden="true" className={cn('text-loop-static', staticTextClassName)}>{staticText}</span>
    <span aria-hidden="true" className="text-loop-changing">
      <AnimatePresence mode="wait" initial={!reduce}>
        <motion.span key={text}
          initial={reduce ? false : { width: 0, opacity: 0 }}
          animate={{ width: 'auto', opacity: 1 }}
          exit={reduce ? undefined : { width: 0, opacity: 0 }}
          transition={reduce ? { duration: 0 } : transition}
          className="text-loop-word"
        >
          <span className={cn('text-loop-background', backgroundClassName)} />
          <span className={cn('text-loop-gradient', rotatingTextClassName)}>{text}</span>
        </motion.span>
      </AnimatePresence>
      <motion.span className={cn('text-loop-cursor', cursorClassName)}
        animate={{ opacity: reduce ? 1 : [1, 0.5] }}
        transition={{ duration: 0.8, repeat: reduce ? 0 : Infinity, repeatType: 'reverse' }} />
    </span>
  </span>;
}
