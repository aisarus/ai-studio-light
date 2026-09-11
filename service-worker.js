const VERSION='dual-audio-offline-v4';
const LOCAL=['./','./index.html','./manifest.webmanifest','./icon.svg'];
const EXTERNAL=['https://cdn.jsdelivr.net/npm/qrious@4.0.2/dist/qrious.min.js','https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.js'];
self.addEventListener('install',event=>{
  event.waitUntil((async()=>{
    const cache=await caches.open(VERSION);
    await cache.addAll(LOCAL);
    for(const url of EXTERNAL){
      try{const r=await fetch(url,{mode:'no-cors',cache:'reload'});await cache.put(url,r)}catch(e){}
    }
    await self.skipWaiting();
  })());
});
self.addEventListener('activate',event=>{
  event.waitUntil((async()=>{
    const keys=await caches.keys();await Promise.all(keys.filter(k=>k!==VERSION).map(k=>caches.delete(k)));await self.clients.claim();
  })());
});
self.addEventListener('fetch',event=>{
  const req=event.request;
  if(req.method!=='GET')return;
  if(req.mode==='navigate'){
    event.respondWith((async()=>{try{const fresh=await fetch(req);const c=await caches.open(VERSION);c.put('./index.html',fresh.clone());return fresh}catch{return (await caches.match('./index.html'))||(await caches.match('./'))}})());return;
  }
  event.respondWith((async()=>{
    const cached=await caches.match(req);if(cached)return cached;
    try{const fresh=await fetch(req);const c=await caches.open(VERSION);c.put(req,fresh.clone());return fresh}catch{return Response.error()}
  })());
});
