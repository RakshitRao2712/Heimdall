import { renderToStaticMarkup } from 'react-dom/server';
import Home from '../App';
import InfoPage from '../pages/InfoPage';
import PolicyPage from '../pages/PolicyPage';
export function renderPublicPage(path: string) {
  const element = path === '/' ? <Home/> : ['/contact','/privacy','/terms','/refund'].includes(path) ? <PolicyPage path={path}/> : <InfoPage path={path}/>;
  // Entrance transforms are decorative; initial HTML must remain readable without JavaScript.
  return renderToStaticMarkup(element).replace(/style="[^"]*"/g, '');
}
