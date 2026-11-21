import { useSession } from '@/auth/session';
import { ShinyButton } from '@/components/ui/shiny-button';
import {
  NavigationMenu, NavigationMenuList, NavigationMenuItem,
  NavigationMenuLink, NavigationMenuTrigger, NavigationMenuContent,
  NavigationMenuPositioner, NavigationMenuPopup,
} from '@/components/ui/navigation-menu-1';

const links = [
  { label: 'Guides', href: '/guides' },
  { label: 'About', href: '/about' },
  { label: 'Pricing', href: '/pricing' },
  { label: 'How it works', href: '/how-it-works' },
];

export function HomepageNav() {
  const { user } = useSession();
  return (
    <header className="homepage-nav">
      <a href="/" className="nav-brand" aria-label="Heimdall home">
        <img src="/images/heimdall-logo.svg" alt="" className="nav-brand-logo" />
        <span>HEIMDALL</span>
      </a>
      <NavigationMenu className="homepage-navigation" aria-label="Main navigation">
        <NavigationMenuList className="homepage-navigation-list">
          {links.map(link => (
            <NavigationMenuItem key={link.href}>
              <NavigationMenuLink className="homepage-navigation-link" href={link.href}>{link.label}</NavigationMenuLink>
            </NavigationMenuItem>
          ))}
          <NavigationMenuItem>
            <NavigationMenuTrigger className="homepage-navigation-link">Setup</NavigationMenuTrigger>
            <NavigationMenuContent className="homepage-setup-content">
              <NavigationMenuLink className="homepage-setup-link" href="/google-places-setup">
                <img src="/images/google-logo.svg" width="28" height="28" alt="" />
                <span className="homepage-setup-copy"><span>Google Places Setup</span></span>
              </NavigationMenuLink>
              <NavigationMenuLink className="homepage-setup-link" href="/foursquare-setup">
                <img src="/images/foursquare-logo-white.svg" width="28" height="28" alt="" />
                <span className="homepage-setup-copy"><span>Foursquare Setup</span></span>
              </NavigationMenuLink>
            </NavigationMenuContent>
          </NavigationMenuItem>
        </NavigationMenuList>
        <NavigationMenuPositioner className="homepage-setup-positioner">
          <NavigationMenuPopup className="homepage-setup-popup" />
        </NavigationMenuPositioner>
      </NavigationMenu>
      <div className="nav-header-actions">
        <ShinyButton className="nav-start-button" onClick={() => window.location.assign(user ? '/dashboard' : '/login')}>
          {user ? 'Dashboard' : 'Get started'}
        </ShinyButton>
      </div>
    </header>
  );
}
