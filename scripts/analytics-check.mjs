import assert from 'node:assert/strict';
import vm from 'node:vm';
import ts from 'typescript';
import {readFile} from 'node:fs/promises';
const source=await readFile('src/analytics.tsx','utf8');
const safeSource=await readFile('src/seo/ai-referral.ts','utf8');
const compile=text=>ts.transpileModule(text,{compilerOptions:{module:ts.ModuleKind.CommonJS,jsx:ts.JsxEmit.ReactJSX,target:ts.ScriptTarget.ES2022}}).outputText;
const safe={exports:{}};vm.runInNewContext(compile(safeSource),{exports:safe.exports,URL});
function run(choice,dnt='0'){
 const calls=[],cookies=[],scripts=[];const state={choice};
 const window={location:{hostname:'heimdall.life',pathname:'/dashboard',search:'?key=secret',hash:'#private'}};
 const document={referrer:'https://chatgpt.com/c/private-conversation?token=secret',head:{appendChild:s=>scripts.push(s)},createElement:()=>({})};
 Object.defineProperty(document,'cookie',{get:()=> 'heimdall_ga=123; session=private',set:value=>cookies.push(value)});
 const exports={};const context={exports,window,document,navigator:{doNotTrack:dnt},localStorage:{getItem:()=>state.choice,setItem:(_,v)=>state.choice=v},Date,URL,require:name=>name.includes('ai-referral')?safe.exports:name==='react'?{useEffect:fn=>fn(),useState:value=>[typeof value==='function'?value():value,()=>{}]}:{jsx:()=>null,jsxs:()=>null}};
 vm.runInNewContext(compile(source.replace('import.meta.env','({})')),context);
 exports.AnalyticsConsent();
 if(window.gtag){const original=window.gtag;window.gtag=(...args)=>{calls.push(args);original(...args);};}
 exports.trackEvent('search_completed','maps');
 return {state,window,calls,cookies,scripts,exports};
}
const rejected=run('rejected');assert.equal(rejected.scripts.length,0);assert.equal(rejected.calls.length,0);
const undecided=run(null);assert.equal(undecided.scripts.length,0);
const dnt=run('accepted','1');assert.equal(dnt.scripts.length,0);
const accepted=run('accepted');assert.equal(accepted.scripts.length,1);assert.equal(accepted.calls.length,1);
const data=accepted.calls[0][2];assert.equal(data.page_location,'https://heimdall.life/dashboard');assert.equal(data.page_referrer,'https://chatgpt.com/');assert(!JSON.stringify(data).includes('secret'));
accepted.state.choice='rejected';accepted.exports.trackEvent('sign_up');assert.equal(accepted.calls.length,1);
assert(rejected.cookies.every(value=>!value.startsWith('session=')));
console.log('Analytics checks passed: opt-in, rejection, DNT, sensitive URL removal, AI referrer minimization, and essential-cookie preservation.');
