import { useId, useState, type ComponentPropsWithoutRef } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { Plus, ArrowUpRight } from 'lucide-react';
import { cn } from '@/lib/utils';

export type FAQEntry = { question: string; answer: string; href?: string; linkLabel?: string };
type FAQProps = Omit<ComponentPropsWithoutRef<'section'>, 'title'> & {
  title?: string; subtitle?: string; categories: Record<string, string>; faqData: Record<string, FAQEntry[]>;
};
export function FAQ({ title = 'Clearly answered', subtitle = 'Your questions', categories, faqData, className, ...props }: FAQProps) {
  const keys = Object.keys(categories);
  const [selected, setSelected] = useState(keys[0]);
  const active = keys.includes(selected) ? selected : keys[0];
  const id = useId();
  const reduced = useReducedMotion();
  if (import.meta.env.SSR) return <section className="heimdall-faq"><h2>{subtitle} {title}</h2>{keys.map(key => <section key={key}><h3>{categories[key]}</h3>{(faqData[key] || []).map(entry => <details key={entry.question}><summary>{entry.question}</summary><p>{entry.answer}</p>{entry.href && <a href={entry.href}>{entry.linkLabel}</a>}</details>)}</section>)}</section>;
  return <section className={cn('heimdall-faq', className)} aria-labelledby={`${id}-heading`} {...props}>
    <header className="faq-heading"><h2 id={`${id}-heading`}><em>{subtitle}</em><span>{title}</span></h2><p>Clear answers. A little less guesswork.</p></header>
    <div className="faq-tabs" aria-label="Question categories">{keys.map(key => <button type="button" key={key} aria-pressed={active === key} onClick={() => setSelected(key)} className={cn('faq-tab',active === key && 'is-selected')}><span>{categories[key]}</span><AnimatePresence>{active === key && <motion.i aria-hidden="true" initial={{ y: '100%' }} animate={{ y: 0 }} exit={{ y: '100%' }} transition={{ duration: reduced ? 0 : .25 }}/>}</AnimatePresence></button>)}</div>
    <div className="faq-list"><AnimatePresence mode="wait" initial={false}><motion.div key={active} initial={{ opacity: 0, y: reduced ? 0 : 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: reduced ? 0 : -8 }} transition={{ duration: reduced ? 0 : .2 }}>{(faqData[active] || []).map(entry => <FAQItem key={entry.question} {...entry}/>)}</motion.div></AnimatePresence></div>
  </section>;
}
function FAQItem({ question, answer, href, linkLabel }: FAQEntry) {
  const [open, setOpen] = useState(false);
  const id = useId(); const reduced = useReducedMotion();
  return <div className={cn('faq-item',open && 'is-open')}>
    <h3><button id={`${id}-question`} type="button" aria-expanded={open} aria-controls={`${id}-answer`} onClick={() => setOpen(!open)}><span>{question}</span><motion.span aria-hidden="true" animate={{ rotate: open ? 45 : 0 }} transition={{ duration: reduced ? 0 : .2 }}><Plus size={20}/></motion.span></button></h3>
    <motion.div id={`${id}-answer`} role="region" aria-labelledby={`${id}-question`} aria-hidden={!open} inert={!open} initial={false} animate={{ height: open ? 'auto' : 0, opacity: open ? 1 : 0 }} transition={{ duration: reduced ? 0 : .25 }} className="faq-answer"><div><p>{answer}</p>{href && <a href={href}>{linkLabel || 'Learn more'}<ArrowUpRight size={15} aria-hidden="true"/></a>}</div></motion.div>
  </div>;
}
