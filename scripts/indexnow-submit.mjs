import {readFile} from 'node:fs/promises';
const {key,origin}=JSON.parse(await readFile('src/seo/indexnow.json','utf8'));
const pages=JSON.parse(await readFile('src/seo/pages.json','utf8'));
const urls=Object.keys(pages).map(path=>origin+(path==='/'?'':path));
const get=async url=>fetch(url,{signal:AbortSignal.timeout(20000),cache:'no-store'});
if(process.argv.includes('--wait')){
 const revision=process.env.GITHUB_SHA;
 if(!revision)throw Error('GITHUB_SHA required for deployment wait');
 let ready=false;
 for(let attempt=0;attempt<30;attempt++){
  try{const response=await get(`${origin}/release.json?revision=${revision}`);if(response.ok&&(await response.json()).revision===revision){ready=true;break;}}catch{}
  await new Promise(resolve=>setTimeout(resolve,20000));
 }
 if(!ready)throw Error('Production deployment did not match this commit; no URLs submitted.');
}
const proof=await get(`${origin}/${key}.txt`);
if(!proof.ok||(await proof.text()).trim()!==key)throw Error('Production IndexNow key proof unavailable; deploy first.');
const response=await fetch('https://api.indexnow.org/indexnow',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({host:new URL(origin).host,key,keyLocation:`${origin}/${key}.txt`,urlList:urls}),signal:AbortSignal.timeout(30000)});
if(![200,202].includes(response.status))throw Error(`IndexNow rejected submission: HTTP ${response.status}`);
console.log(`IndexNow received ${urls.length} public URLs: HTTP ${response.status}. Receipt is not an indexing guarantee.`);
