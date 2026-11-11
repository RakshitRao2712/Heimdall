import {renderPublicPage} from '../.seo-render/prerender.js';
import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {loadEnv} from 'vite';
const revision=process.env.CF_PAGES_COMMIT_SHA||process.env.GITHUB_SHA||'';
const brand=JSON.parse(await readFile('src/seo/brand.json','utf8'));
const pages=JSON.parse(await readFile('src/seo/pages.json','utf8'));
const review=JSON.parse(await readFile('src/seo/review.json','utf8'));
const env={...loadEnv('production',process.cwd(),''),...process.env};
const raw=env.VITE_SITE_URL||'';let origin='';
if(raw){const u=new URL(raw);if(u.protocol!=='https:'||['localhost','127.0.0.1'].includes(u.hostname)||u.pathname!=='/'||u.search||u.hash)throw new Error('VITE_SITE_URL must be your public HTTPS origin, without a path.');origin=u.origin;}
const esc=s=>s.replaceAll('&','&amp;').replaceAll('"','&quot;').replaceAll('<','&lt;').replaceAll('>','&gt;');
const template=await readFile('dist/index.html','utf8');
const nav=Object.entries(pages).map(([p,v])=>`<a href="${p}">${esc(p==='/'?'Home':v.title.split('|')[0].trim())}</a>`).join(' ');
for(const path of [...Object.keys(pages),'/login','/dashboard','/404']){
 const info=pages[path];const title=info?.title||(path==='/login'?'Sign In | Heimdall':path==='/dashboard'?'Dashboard | Heimdall':'Page Not Found | Heimdall');
 const description=info?.description||(path==='/404'?'This page could not be found. Return to Heimdall’s home page.':'Sign in to your private Heimdall workspace.');
 const indexable=!!origin&&!!info;
 const url=origin?origin+path:'';
 const graph=origin&&info?{'@context':'https://schema.org','@graph':[{'@type':'Organization','@id':origin+'/#organization',name:'Heimdall',url:origin,logo:origin+'/images/heimdall-logo.svg',sameAs:['https://www.linkedin.com/company/heimdall-linkedin/'],description:brand.description,contactPoint:{'@type':'ContactPoint',contactType:'customer support',email:brand.supportEmail}},{'@type':'WebSite','@id':origin+'/#website',url:origin,name:'Heimdall',publisher:{'@id':origin+'/#organization'}},{'@type':'WebPage','@id':url+'#page',url,name:title,description,inLanguage:'en',...(['/pricing','/how-it-works','/google-places-setup','/foursquare-setup'].includes(path)?{dateModified:review.date}:{}),isPartOf:{'@id':origin+'/#website'},about:{'@id':origin+'/#organization'}},...(path!=='/'?[{'@type':'BreadcrumbList',itemListElement:[{'@type':'ListItem',position:1,name:'Home',item:origin},{'@type':'ListItem',position:2,name:title.split('|')[0].trim(),item:url}]}]:[])]}:null;
 if(graph && ['/', '/pricing', '/how-it-works'].includes(path)) graph['@graph'].push({'@type':'SoftwareApplication','@id':origin+'/#software',name:'Heimdall',url:origin,applicationCategory:'BusinessApplication',operatingSystem:'Web browser',description:brand.description,isAccessibleForFree:false,featureList:['Google Places discovery','Foursquare business discovery','Named lead files','Team collaboration'],offers:{'@type':'Offer',url:origin+'/pricing',price:'100',priceCurrency:'INR',description:'One-time payment for current Premium features during the product operating lifetime. Provider API fees are separate.'},publisher:{'@id':origin+'/#organization'}});
 if(graph){graph['@graph'].push({'@type':'Person','@id':origin+'/about#operator',name:brand.operator,url:origin+'/about'});const page=graph['@graph'].find(node=>node['@type']==='WebPage');if(['/how-it-works','/google-places-setup','/foursquare-setup'].includes(path))page.author={'@id':origin+'/about#operator'};}
 if(graph && path.startsWith('/guides/'))graph['@graph'].push({'@type':'Article',headline:info.heading,description,url,datePublished:'2026-11-12',dateModified:'2026-11-12',author:{'@id':origin+'/about#operator'},publisher:{'@id':origin+'/#organization'},image:[origin+'/images/guides/google-setup.webp',origin+'/images/guides/workflow.webp'],mainEntityOfPage:{'@id':url+'#page'}});
 const head=`${path==='/'?'<link rel="preload" href="/fonts/Anton-Regular.woff2" as="font" type="font/woff2" crossorigin/><link rel="preload" href="/fonts/Yellowtail-Regular.woff2" as="font" type="font/woff2" crossorigin/>':''}<title>${esc(title)}</title><meta name="description" content="${esc(description)}"/><meta name="robots" content="${indexable?'index,follow,max-image-preview:large':'noindex,follow'}"/><meta property="og:type" content="website"/><meta property="og:site_name" content="Heimdall"/><meta property="og:title" content="${esc(title)}"/><meta property="og:description" content="${esc(description)}"/><meta name="twitter:card" content="summary_large_image"/><meta name="twitter:title" content="${esc(title)}"/><meta name="twitter:description" content="${esc(description)}"/>${indexable?`<link rel="canonical" href="${url}"/><meta property="og:url" content="${url}"/><meta property="og:image" content="${origin}/images/heimdall-social.png"/><meta name="twitter:image" content="${origin}/images/heimdall-social.png"/><meta property="og:image:width" content="1200"/><meta property="og:image:height" content="630"/><meta property="og:image:alt" content="Heimdall — Local business lead discovery"/>`:''}${graph?`<script type="application/ld+json">${JSON.stringify(graph).replaceAll('<','\\u003c')}</script>`:''}`;
 let html=template.replace(/<title>.*?<\/title>/s,'').replace(/<meta name="description"[^>]*\/?\s*>/,'').replace('</head>',head+'</head>');
 const content=info?renderPublicPage(path):path==='/404'?'<main><h1>Page not found</h1><a href="/">Back to Heimdall</a></main>':'<main><h1>Heimdall private workspace</h1><p>JavaScript is required to sign in and use your workspace.</p><a href="/">Back to home</a></main>';
 html=html.replace('<div id="root"></div>',`<div id="root"><div class="seo-initial-content">${content}</div></div>`);
 const directory=path==='/'?'dist':`dist${path}`;await mkdir(directory,{recursive:true});await writeFile(`${directory}/index.html`,html);
 if(path==='/404')await writeFile('dist/404.html',html);
}
await writeFile('dist/robots.txt',origin?`User-agent: *\nAllow: /\nDisallow: /api/\nSitemap: ${origin}/sitemap.xml\n`:'User-agent: *\nDisallow: /\n');
await writeFile('dist/release.json',JSON.stringify({revision}));
await writeFile('dist/sitemap.xml',`<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">${origin?Object.keys(pages).map(p=>`<url><loc>${origin}${p==='/'?'':p}</loc></url>`).join(''):''}</urlset>`);
console.log(`SEO: generated ${Object.keys(pages).length} public HTML pages. ${origin?'Production origin: '+origin:'No VITE_SITE_URL: indexing disabled until your domain is configured.'}`);
