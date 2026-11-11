import assert from 'node:assert/strict';
import {readFile,access} from 'node:fs/promises';
const pages=JSON.parse(await readFile('src/seo/pages.json','utf8'));
const faqContent=JSON.parse(await readFile('src/seo/faq.json','utf8'));
const escapeHtml=value=>value.replaceAll('&','&amp;').replaceAll('"','&quot;').replaceAll('<','&lt;').replaceAll('>','&gt;');
const helpContent=JSON.parse(await readFile('src/seo/troubleshooting.json','utf8'));
const review=JSON.parse(await readFile('src/seo/review.json','utf8'));
const titles=new Set();const descriptions=new Set();let origin='';
for(const [path,page] of Object.entries(pages)){
 const html=await readFile(`dist${path==='/'?'':path}/index.html`,'utf8');
 assert.equal((html.match(/<title>/g)||[]).length,1,`${path}: one title`);
 assert.equal((html.match(/<h1[ >]/g)||[]).length,1,`${path}: one initial H1`);
 assert.equal((html.match(/<meta name="description"/g)||[]).length,1,`${path}: one description`);
 assert(!titles.has(page.title),`${path}: duplicate title`);titles.add(page.title);
 assert(!descriptions.has(page.description),`${path}: duplicate description`);descriptions.add(page.description);
 assert(page.title.length<=65&&page.description.length<=165,`${path}: metadata too long`);
 assert(html.includes('<html lang="en"'),`${path}: document language`);
 for(const group of faqContent.pages[path]??[])for(const item of faqContent.groups[group].items){assert(html.includes(`<summary>${escapeHtml(item.question)}</summary>`),`${path}: missing initial FAQ question`);assert(html.includes(escapeHtml(item.answer)),`${path}: missing FAQ answer`);assert(html.includes(`href="${item.href}"`),`${path}: missing FAQ guide link`);}
 if(helpContent[path])for(const item of helpContent[path].items){assert(html.includes(escapeHtml(item.question)),`${path}: missing troubleshooting question`);for(const step of item.steps)assert(html.includes(escapeHtml(step)),`${path}: missing troubleshooting step`);}
 if(['/pricing','/how-it-works','/google-places-setup','/foursquare-setup'].includes(path)){assert(html.toLowerCase().includes(`datetime="${review.date}"`),`${path}: missing review date`);for(const source of review.sources)assert(html.includes(source.url),`${path}: missing official source`);}
 const canonical=html.match(/<link rel="canonical" href="([^"]+)"/);
 if(canonical){const url=new URL(canonical[1]);origin=url.origin;assert.equal(url.pathname,path);assert(!url.search&&!url.hash);assert(html.includes('index,follow,max-image-preview:large'));assert(html.includes('og:image'));assert(html.includes('twitter:card'));const schema=JSON.parse(html.match(/<script type="application\/ld\+json">(.*?)<\/script>/s)[1]);assert(schema['@graph'].some(v=>v['@type']==='WebPage'));if(['/', '/pricing', '/how-it-works'].includes(path)){const app=schema['@graph'].find(v=>v['@type']==='SoftwareApplication');assert(app&&app.offers.price==='100'&&app.offers.priceCurrency==='INR',`${path}: product offer schema`);}assert(!schema['@graph'].some(v=>v.aggregateRating||v.review),'No invented reviews');}
 else assert(html.includes('noindex,follow'),`${path}: unconfigured sites must not index`);
 for(const link of html.matchAll(/href="(\/[^"?#]*)/g)){const target=link[1];if(target.startsWith('/assets/')||target.startsWith('/images/')||target.startsWith('/fonts/'))await access('dist'+target);else assert(pages[target]||['/login','/dashboard'].includes(target),`${path}: unresolved internal link ${target}`);}
}
const sitemap=await readFile('dist/sitemap.xml','utf8');const robots=await readFile('dist/robots.txt','utf8');
assert(!sitemap.includes('/dashboard')&&!sitemap.includes('/login')&&!sitemap.includes('/api/'),'Private routes in sitemap');
assert.equal((sitemap.match(/<loc>/g)||[]).length,origin?Object.keys(pages).length:0);
if(origin)assert(robots.includes(`Sitemap: ${origin}/sitemap.xml`));else assert(robots.includes('Disallow: /'));
for(const path of ['/login','/dashboard','/404']){const html=await readFile(`dist${path}/index.html`,'utf8');assert(html.includes('noindex,follow'),`${path}: private/error index protection`);assert(!html.includes('rel="canonical"'),`${path}: private/error canonical`);}
await access('dist/images/heimdall-social.png');await access('dist/images/heimdall-logo.svg');
console.log(`SEO checks passed: ${Object.keys(pages).length} public routes, unique metadata, initial headings, links, schema, assets, sitemap and noindex rules. ${origin?'Origin: '+origin:'Staging indexing intentionally disabled.'}`);
