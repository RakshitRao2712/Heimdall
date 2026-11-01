import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {loadEnv} from 'vite';
const pages=JSON.parse(await readFile('src/seo/pages.json','utf8'));
const env={...loadEnv('production',process.cwd(),''),...process.env};
const raw=env.VITE_SITE_URL||'';let origin='';
if(raw){const u=new URL(raw);if(u.protocol!=='https:'||['localhost','127.0.0.1'].includes(u.hostname)||u.pathname!=='/'||u.search||u.hash)throw new Error('VITE_SITE_URL must be your public HTTPS origin, without a path.');origin=u.origin;}
const esc=s=>s.replaceAll('&','&amp;').replaceAll('"','&quot;').replaceAll('<','&lt;').replaceAll('>','&gt;');
const template=await readFile('dist/index.html','utf8');
const nav=Object.entries(pages).map(([p,v])=>`<a href="${p}">${esc(p==='/'?'Home':v.title.split('|')[0].trim())}</a>`).join(' ');
for(const path of [...Object.keys(pages),'/login','/dashboard','/404']){
 const info=pages[path];const title=info?.title||(path==='/login'?'Sign In | Heimdall':path==='/dashboard'?'Dashboard | Heimdall':'Page Not Found | Heimdall');
 const description=info?.description||'Sign in to your private Heimdall workspace.';
 const indexable=!!origin&&!!info;
 const url=origin?origin+(path==='/'?'':path):'';
 const graph=origin&&info?{'@context':'https://schema.org','@graph':[{'@type':'Organization','@id':origin+'/#organization',name:'Heimdall',url:origin,logo:origin+'/images/heimdall-logo.svg'},{'@type':'WebSite','@id':origin+'/#website',url:origin,name:'Heimdall',publisher:{'@id':origin+'/#organization'}},{'@type':'WebPage','@id':url+'#page',url,name:title,description,isPartOf:{'@id':origin+'/#website'}},...(path!=='/'?[{'@type':'BreadcrumbList',itemListElement:[{'@type':'ListItem',position:1,name:'Home',item:origin},{'@type':'ListItem',position:2,name:title.split('|')[0].trim(),item:url}]}]:[])]}:null;
 const head=`<title>${esc(title)}</title><meta name="description" content="${esc(description)}"/><meta name="robots" content="${indexable?'index,follow,max-image-preview:large':'noindex,follow'}"/><meta property="og:type" content="website"/><meta property="og:site_name" content="Heimdall"/><meta property="og:title" content="${esc(title)}"/><meta property="og:description" content="${esc(description)}"/><meta name="twitter:card" content="summary_large_image"/><meta name="twitter:title" content="${esc(title)}"/><meta name="twitter:description" content="${esc(description)}"/>${origin?`<link rel="canonical" href="${url}"/><meta property="og:url" content="${url}"/><meta property="og:image" content="${origin}/images/heimdall-social.png"/><meta name="twitter:image" content="${origin}/images/heimdall-social.png"/><meta property="og:image:width" content="1200"/><meta property="og:image:height" content="630"/><meta property="og:image:alt" content="Heimdall — Local business lead discovery"/>`:''}${graph?`<script type="application/ld+json">${JSON.stringify(graph).replaceAll('<','\\u003c')}</script>`:''}`;
 let html=template.replace(/<title>.*?<\/title>/s,'').replace(/<meta name="description"[^>]*\/?\s*>/,'').replace('</head>',head+'</head>');
 const content=info?`<header><a href="/">Heimdall</a><nav aria-label="Main navigation">${nav}</nav></header><main><h1>${esc(info.heading)}</h1><p>${esc(description)}</p>${info.sections.map(([h,p])=>`<section><h2>${esc(h)}</h2><p>${esc(p)}</p></section>`).join('')}<a href="/login">Get started with Heimdall</a></main><footer>${nav}</footer>`:path==='/404'?'<main><h1>Page not found</h1><a href="/">Back to Heimdall</a></main>':'<main><h1>Heimdall private workspace</h1><p>JavaScript is required to sign in and use your workspace.</p><a href="/">Back to home</a></main>';
 html=html.replace('<div id="root"></div>',`<div id="root"><div class="seo-initial-content">${content}</div></div>`);
 const directory=path==='/'?'dist':`dist${path}`;await mkdir(directory,{recursive:true});await writeFile(`${directory}/index.html`,html);
 if(path==='/404')await writeFile('dist/404.html',html);
}
await writeFile('dist/robots.txt',origin?`User-agent: *\nAllow: /\nDisallow: /api/\nSitemap: ${origin}/sitemap.xml\n`:'User-agent: *\nDisallow: /\n');
await writeFile('dist/sitemap.xml',`<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">${origin?Object.keys(pages).map(p=>`<url><loc>${origin}${p==='/'?'':p}</loc></url>`).join(''):''}</urlset>`);
console.log(`SEO: generated ${Object.keys(pages).length} public HTML pages. ${origin?'Production origin: '+origin:'No VITE_SITE_URL: indexing disabled until your domain is configured.'}`);
