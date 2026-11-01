import { Mail } from 'lucide-react';
export const supportEmail='kirankrish2019@gmail.com';
export const policyLinks=[{label:'Contact',href:'/contact'},{label:'Privacy',href:'/privacy'},{label:'Terms & Conditions',href:'/terms'},{label:'Refund Policy',href:'/refund'}];
export function SocialLinks(){return <div className="public-socials"><a href={`mailto:${supportEmail}`} aria-label="Email Heimdall"><Mail size={20}/></a><span aria-label="LinkedIn — link coming soon" title="LinkedIn — coming soon"><svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M5.4 7.5H2V22h3.4V7.5ZM3.7 2a2 2 0 1 0 0 4 2 2 0 0 0 0-4ZM22 13.7c0-4.3-2.3-6.3-5.3-6.3-2.4 0-3.4 1.3-4 2.1v-2H9.3V22h3.4v-8.1c0-2.1.4-4.1 3-4.1s2.8 2.4 2.8 4.3V22H22v-8.3Z"/></svg></span></div>;}
export function PolicyLinks(){return <nav className="policy-links" aria-label="Policies and support">{policyLinks.map(link=><a href={link.href} key={link.href}>{link.label}</a>)}</nav>;}
