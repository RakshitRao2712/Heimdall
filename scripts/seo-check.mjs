import assert from 'node:assert/strict';
import {readFile,access} from 'node:fs/promises';
const pages=JSON.parse(await readFile('src/seo/pages.json','utf8'));
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
 const canonical=html.match(/<link rel="canonical" href="([^"]+)"/);
 if(canonical){const url=new URL(canonical[1]);origin=url.origin;assert.equal(url.pathname,path);assert(!url.search&&!url.hash);assert(html.includes('index,follow,max-image-preview:large'));assert(html.includes('og:image'));assert(html.includes('twitter:card'));const schema=JSON.parse(html.match(/<script type="application\/ld\+json">(.*?)<\/script>/s)[1]);assert(schema['@graph'].some(v=>v['@type']==='WebPage'));assert(!schema['@graph'].some(v=>v.aggregateRating||v.review),'No invented reviews');}
 else assert(html.includes('noindex,follow'),`${path}: unconfigured sites must not index`);
 for(const link of html.matchAll(/href="(\/[^"?#]*)/g)){const target=link[1];if(target.startsWith('/assets/')||target.startsWith('/images/'))await access('dist'+target);else assert(pages[target]||['/login','/dashboard'].includes(target),`${path}: unresolved internal link ${target}`);}
}
const sitemap=await readFile('dist/sitemap.xml','utf8');const robots=await readFile('dist/robots.txt','utf8');
assert(!sitemap.includes('/dashboard')&&!sitemap.includes('/login')&&!sitemap.includes('/api/'),'Private routes in sitemap');
assert.equal((sitemap.match(/<loc>/g)||[]).length,origin?Object.keys(pages).length:0);
if(origin)assert(robots.includes(`Sitemap: ${origin}/sitemap.xml`));else assert(robots.includes('Disallow: /'));
for(const path of ['/login','/dashboard','/404']){const html=await readFile(`dist${path}/index.html`,'utf8');assert(html.includes('noindex,follow'),`${path}: private/error index protection`);}
await access('dist/images/heimdall-social.png');await access('dist/images/heimdall-logo.svg');
console.log(`SEO checks passed: ${Object.keys(pages).length} public routes, unique metadata, initial headings, links, schema, assets, sitemap and noindex rules. ${origin?'Origin: '+origin:'Staging indexing intentionally disabled.'}`);
