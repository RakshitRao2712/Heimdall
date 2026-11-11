import {readFile,writeFile} from 'node:fs/promises';
const origin='https://heimdall.life';
const pages=JSON.parse(await readFile('src/seo/pages.json','utf8'));
const results=[];
for(const path of [...Object.keys(pages),'/robots.txt','/sitemap.xml','/login','/dashboard','/missing-seo-audit-page']){
 const response=await fetch(origin+path,{signal:AbortSignal.timeout(90000)});const html=await response.text();
 const expected=path==='/missing-seo-audit-page'?404:200;
 const problems=[];if(response.status!==expected)problems.push(`expected ${expected}`);
 if(pages[path]){if(!html.includes(`<link rel="canonical" href="${origin+path}"`))problems.push('canonical mismatch');if(!html.includes('index,follow'))problems.push('not indexable');}
 if(['/login','/dashboard'].includes(path)&&!html.includes('noindex'))problems.push('missing noindex');
 results.push({path,status:response.status,finalUrl:response.url,problems});
}
for(const url of ['http://heimdall.life/','https://www.heimdall.life/']){const r=await fetch(url,{redirect:'manual',signal:AbortSignal.timeout(30000)});results.push({path:url,status:r.status,location:r.headers.get('location'),problems:![301,308].includes(r.status)?['permanent redirect expected']:[]});}
await writeFile('/tmp/heimdall-seo-live.json',JSON.stringify({checkedAt:new Date().toISOString(),results},null,2));
console.table(results.map(({path,status,problems})=>({path,status,problems:problems.join('; ')})));
if(results.some(r=>r.problems.length))process.exitCode=1;
