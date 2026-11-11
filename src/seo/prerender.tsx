import DiscoveryPage from '../pages/DiscoveryPage';
import AboutPage from '../pages/AboutPage';
import { renderToStaticMarkup } from 'react-dom/server';
import Home from '../App';
import InfoPage from '../pages/InfoPage';
import PolicyPage from '../pages/PolicyPage';
export function renderPublicPage(path: string) {
  const element = ['/google-places','/foursquare','/for-freelancers','/for-web-designers','/for-agencies','/guides','/guides/first-search'].includes(path) ? <DiscoveryPage path={path}/> : path === '/about' ? <AboutPage/> : path === '/' ? <Home/> : ['/contact','/privacy','/terms','/refund'].includes(path) ? <PolicyPage path={path}/> : <InfoPage path={path}/>;
  // Entrance transforms are decorative; initial HTML must remain readable without JavaScript.
  return renderToStaticMarkup(element).replace(/style="[^"]*"/g, '');
}
