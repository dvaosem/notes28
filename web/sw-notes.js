/* N28 Service Worker — offline cache shell + notifikácie.
   Ulož tento súbor do rovnakého priečinka ako notes28.html (web root).
   Pri zmene app shellu zvýš číslo CACHE (n28-v2 → n28-v5) aby sa cache obnovila. */
const CACHE = 'n28-v77';
const NAV_TIMEOUT = 600;   // ms — po tomto čase sa ukáže uložená verzia appky

// App shell — uprav názvy ak je súbor pomenovaný inak / v podpriečinku.
const PRECACHE = [
  './',
  './notes28.html',
  './manifest-notes.json',
  'https://cdn.jsdelivr.net/npm/sortablejs@1.15.2/Sortable.min.js',
  'https://dvaosem.com/n28-icon-v2.png',
  'https://dvaosem.com/n28-maskable-512.png',
  'https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js',
  'https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js',
  'https://www.gstatic.com/firebasejs/10.12.0/firebase-firestore.js',
];

self.addEventListener('install', e => {
  // každý súbor zvlášť — keď jeden CDN zlyhá, app shell (notes28.html) sa aj tak uloží (addAll je "všetko alebo nič")
  e.waitUntil(caches.open(CACHE).then(c => Promise.allSettled(PRECACHE.map(u => c.add(u)))));
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
  );
  e.waitUntil(self.clients.claim().then(() => self.clients.matchAll({ type:'window' })).then(cs =>
    cs.forEach(c => c.postMessage({ type:'N28_SW_ACTIVE', build:CACHE }))));
});

self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  const url = e.request.url;
  if (!url.startsWith('http://') && !url.startsWith('https://')) return; // chrome-extension:// a pod. — Cache API to nevie uložiť

  // Firebase / Firestore / Auth / Cloudinary — vždy zo siete (real-time dáta), bez cache.
  if (url.includes('firestore') || url.includes('firebaseio') || url.includes('googleapis.com') ||
      url.includes('identitytoolkit') || url.includes('cloudinary.com')) {
    return; // necháme prejsť default fetch (real-time dáta / auth, nie statický SDK súbor)
  }

  // Navigácia (HTML) — RÝCHLY ŠTART: sieť dostane NAV_TIMEOUT ms; ak neodpovie (slabý signál),
  // ihneď sa ukáže uložená verzia a sieť dobehne na pozadí (čerstvé HTML pri ďalšom štarte).
  // Pri dobrej sieti dostaneš rovno čerstvú verziu, takže nahrané zmeny sa ukážu hneď.
  if (e.request.mode === 'navigate') {
    e.respondWith((async () => {
      const cache = await caches.open(CACHE);
      const cached = await cache.match(e.request, { ignoreSearch: true }) || await cache.match('./notes28.html');
      const network = fetch(e.request).then(res => { if (res.ok) cache.put(e.request, res.clone()); return res; });
      if (!cached) return network;                       // úplne prvý štart, cache ešte nie je
      const timeout = new Promise(r => setTimeout(() => r(null), NAV_TIMEOUT));
      try { const res = await Promise.race([network, timeout]); if (res && res.ok) return res; } catch (_) {}
      e.waitUntil(network.catch(() => {}));              // dokonči aktualizáciu cache aj po odpovedi z cache
      return cached;
    })());
    return;
  }

  // Statické assety — cache-first s doplnením.
  e.respondWith(
    caches.match(e.request).then(cached => cached || fetch(e.request).then(res => {
      if (res.ok) { const clone = res.clone(); caches.open(CACHE).then(c => c.put(e.request, clone)); }
      return res;
    }).catch(() => cached))
  );
});

// Klik na notifikáciu → otvor / sfokusuj appku.
self.addEventListener('notificationclick', e => {
  e.notification.close();
  e.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(list => {
      for (const c of list) { if ('focus' in c) return c.focus(); }
      if (self.clients.openWindow) return self.clients.openWindow('./');
    })
  );
});
