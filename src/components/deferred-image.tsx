import { useEffect, useRef, useState, type ImgHTMLAttributes } from 'react';
/** Decorative images start loading only as their section approaches the viewport. */
export function DeferredImage({src,...props}:ImgHTMLAttributes<HTMLImageElement>){
 const ref=useRef<HTMLImageElement>(null);const [near,setNear]=useState(false);
 useEffect(()=>{const element=ref.current;if(!element)return;const observer=new IntersectionObserver(entries=>{if(entries.some(entry=>entry.isIntersecting)){setNear(true);observer.disconnect();}},{rootMargin:'200px'});observer.observe(element);return()=>observer.disconnect();},[]);
 return <img {...props} ref={ref} src={near?src:undefined} loading="lazy" decoding="async"/>;
}
