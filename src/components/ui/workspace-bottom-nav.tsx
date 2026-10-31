import { useSession } from '@/auth/session';
import { useEffect, useId, useRef, useState } from 'react';
import { ArrowUpRight, ChevronDown, Copyright, FolderOpen, MapPin, Search, Users } from 'lucide-react';
import { LiquidNavbar, NimbusIcons } from './liquid-glass-navbar';
export type WorkspaceView = 'dashboard' | 'leads' | 'teams' | 'profile' | 'maps' | 'copyright';
export const viewLabels: Record<WorkspaceView, string> = { dashboard: 'Dashboard', leads: 'Leads', teams: 'Teams', profile: 'Profile', maps: 'Google Places', copyright: 'Website Copyright' };
export function WorkspaceBottomNav({ view, onNavigate }: { view: WorkspaceView; onNavigate: (view: WorkspaceView) => void }) {
  const { user, profile: accountProfile } = useSession();
  const [identity, setIdentity] = useState({ name: '', avatar: '' });
  useEffect(() => {
    const sync = () => {
      try {
        const profile = JSON.parse(localStorage.getItem(`heimdall-profile:${user?.email}`) || '{}');
        const fullName = [accountProfile?.firstName || profile.firstName, accountProfile?.lastName || profile.lastName].filter(value => typeof value === 'string' && value.trim()).join(' ');
        const name = accountProfile?.username || (typeof profile.username === 'string' ? profile.username : '') || fullName || user?.email.split('@')[0] || 'Profile';
        setIdentity({ name, avatar: typeof profile.avatar === 'string' && /^data:image\/(jpeg|png|webp);base64,/.test(profile.avatar) ? profile.avatar : '' });
      } catch { setIdentity({ name: user?.email.split('@')[0] || 'Profile', avatar: '' }); }
    };
    sync();
    window.addEventListener('heimdall-profile-updated', sync);
    window.addEventListener('storage', sync);
    return () => { window.removeEventListener('heimdall-profile-updated', sync); window.removeEventListener('storage', sync); };
  }, [user?.email, accountProfile]);
  const [menu, setMenu] = useState(false); const root = useRef<HTMLDivElement>(null); const popup = useRef<HTMLDivElement>(null); const id = useId();
  const active = menu || view === 'maps' || view === 'copyright' ? 3 : view === 'leads' ? 1 : view === 'teams' ? 2 : view === 'profile' ? 4 : 0;
  useEffect(() => { setMenu(false); }, [view]);
  useEffect(() => { if (!menu) return; const outside = (event: PointerEvent) => { if (!root.current?.contains(event.target as Node)) setMenu(false); }; const key = (event: KeyboardEvent) => { if (event.key === 'Escape') { event.preventDefault(); setMenu(false); root.current?.querySelector<HTMLButtonElement>('[aria-haspopup="menu"]')?.focus(); } }; document.addEventListener('pointerdown', outside); document.addEventListener('keydown', key); popup.current?.querySelector<HTMLButtonElement>('[role="menuitem"]')?.focus(); return () => { document.removeEventListener('pointerdown', outside); document.removeEventListener('keydown', key); }; }, [menu]);
  return <div className="workspace-bottom-nav" ref={root}>
    {menu && <div id={id} ref={popup} role="menu" aria-label="Scrape methods" className="workspace-scrape-menu" onKeyDown={event => { const items = Array.from(popup.current?.querySelectorAll<HTMLButtonElement>('[role="menuitem"]') || []); const at = items.indexOf(document.activeElement as HTMLButtonElement); if (event.key === 'ArrowDown' || event.key === 'ArrowUp') { event.preventDefault(); items[(at + (event.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length]?.focus(); } if (event.key === 'Tab') setMenu(false); }}><span>CHOOSE A SEARCH METHOD</span>{([{ view: 'maps', title: 'Google Places', detail: 'Find businesses without websites', icon: MapPin }, { view: 'copyright', title: 'Website Copyright', detail: 'Find website refresh opportunities', icon: Copyright }] as const).map(item => <button key={item.view} role="menuitem" aria-current={view === item.view ? 'page' : undefined} onClick={() => { setMenu(false); onNavigate(item.view); }}><span className="scrape-method-icon"><item.icon size={20}/></span><span className="scrape-method-copy"><strong>{item.title}</strong><small>{item.detail}</small></span><ArrowUpRight className="scrape-method-arrow" size={16} aria-hidden="true"/></button>)}</div>}
    <LiquidNavbar tone="dark" sound={false} compact={false} active={active} onChange={index => { if (index === 3) { setMenu(previous => !previous); return; } setMenu(false); onNavigate(index === 0 ? 'dashboard' : index === 1 ? 'leads' : index === 2 ? 'teams' : 'profile'); }} items={[{ label: 'Dashboard', icon: NimbusIcons.home, fx: 'glow' }, { label: 'Leads', icon: <FolderOpen size={18}/>, fx: 'pop' }, { label: 'Teams', icon: <Users size={18}/>, fx: 'pop' }, { label: 'Scrape', icon: <span className="workspace-scrape-icon"><Search size={18}/><ChevronDown size={11}/></span>, fx: 'drop', hasPopup: true, expanded: menu, controls: id }, { label: identity.name || 'Profile', icon: <span className="workspace-dock-avatar" title="Your profile">{identity.avatar ? <img src={identity.avatar} alt="" /> : <span>{(identity.name || 'P').slice(0, 2).toUpperCase()}</span>}</span>, fx: 'wave' }]}/>
  </div>;
}
