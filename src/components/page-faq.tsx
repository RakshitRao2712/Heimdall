import content from '@/seo/faq.json';
import { FAQ } from './ui/faq-tabs';
export function PageFAQ({ path }: { path: string }) {
  const keys = content.pages[path as keyof typeof content.pages];
  if (!keys) return null;
  const groups = keys.map(key => [key, content.groups[key as keyof typeof content.groups]] as const);
  return <FAQ id="faq" categories={Object.fromEntries(groups.map(([key, group]) => [key, group.label]))} faqData={Object.fromEntries(groups.map(([key, group]) => [key, group.items]))}/>;
}
