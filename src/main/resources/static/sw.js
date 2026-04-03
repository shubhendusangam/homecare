// sw.js — HomeCare Service Worker (app shell caching)
const CACHE_NAME = 'homecare-v1';
const SHELL = ['/', '/index.html', '/css/app.css', '/js/app.js', '/js/auth.js', '/js/api.js', '/js/router.js', '/js/toast.js'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE_NAME).then(c => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});

self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  // Network-first for API, cache-first for static
  if (e.request.url.includes('/api/')) return;
  e.respondWith(caches.match(e.request).then(cached => cached || fetch(e.request)));
});

