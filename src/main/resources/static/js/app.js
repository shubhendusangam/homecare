/**
 * app.js — Main entry point for the HomeCare SPA.
 *
 * Initialises the router, wires up global event listeners (logout button,
 * tab clicks, resize handler for sidebar/bottom-bar switching),
 * notification bell, onboarding overlay, push notification prompt.
 */
import { auth }   from './auth.js';
import { router } from './router.js';
import { toast }  from './toast.js';

// ─── Tab click handler ──────────────────────────────────────────────
document.getElementById('tab-nav').addEventListener('click', (e) => {
  const btn = e.target.closest('.tab-btn');
  if (!btn) return;
  const tab = btn.dataset.tab;
  const defaults = { customer: '/customer/home', helper: '/helper/home', admin: '/admin/dashboard' };
  if (defaults[tab]) router.navigate(defaults[tab]);
});

// ─── Logout button ──────────────────────────────────────────────────
document.getElementById('btn-logout').addEventListener('click', () => {
  toast('Logged out successfully', 'success');
  auth.logout();
});

// ─── Helpers ────────────────────────────────────────────────────────
function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
function timeAgo(date) {
  const s = Math.floor((Date.now() - date.getTime()) / 1000);
  if (s < 60) return 'just now';
  if (s < 3600) return Math.floor(s / 60) + 'm ago';
  if (s < 86400) return Math.floor(s / 3600) + 'h ago';
  return Math.floor(s / 86400) + 'd ago';
}
const NOTIF_ICONS = {
  BOOKING_CONFIRMED:'📋', BOOKING_ASSIGNED:'✅', HELPER_EN_ROUTE:'🚗',
  BOOKING_COMPLETED:'🎉', BOOKING_CANCELLED:'❌', BOOKING_REMINDER:'⏰',
  PAYMENT_SUCCESS:'💰', PAYMENT_REFUND:'↩️', NEW_REVIEW:'⭐', WALLET_LOW:'⚠️',
  SYSTEM_ALERT:'📢', CHAT_MESSAGE:'💬', SUBSCRIPTION_STARTED:'📋',
  SUBSCRIPTION_RENEWED:'🔄', SUBSCRIPTION_CANCELLED:'❌',
  WALLET_INSUFFICIENT_FOR_RENEWAL:'💸', DISPUTE_RAISED:'⚖️', DISPUTE_RESOLVED:'✅',
  REFERRAL_SIGNUP_CREDIT:'🎁', REFERRAL_BONUS_CREDIT:'🎉',
};

// ─── Notification Bell ──────────────────────────────────────────────
const notifBell = document.getElementById('notif-bell');
const notifBadge = document.getElementById('notif-badge');
let notifPollTimer = null;

notifBell.addEventListener('click', () => {
  const slot = document.getElementById('notif-panel-slot');
  if (slot.querySelector('.notif-panel')) { slot.innerHTML = ''; return; }
  openNotifPanel();
});

function startNotifPolling() {
  notifBell.classList.remove('hidden');
  pollUnread();
  if (notifPollTimer) clearInterval(notifPollTimer);
  notifPollTimer = setInterval(pollUnread, 30000);
}
function stopNotifPolling() {
  notifBell.classList.add('hidden');
  if (notifPollTimer) { clearInterval(notifPollTimer); notifPollTimer = null; }
}
async function pollUnread() {
  if (!auth.isLoggedIn()) return;
  try {
    const res = await fetch('/api/v1/notifications/unread-count', { headers: { 'Authorization': 'Bearer ' + auth.token() } });
    if (!res.ok) return;
    const json = await res.json();
    const count = json.data || 0;
    if (count > 0) { notifBadge.textContent = count > 99 ? '99+' : count; notifBadge.classList.remove('hidden'); }
    else { notifBadge.classList.add('hidden'); }
  } catch {}
}
async function openNotifPanel() {
  const slot = document.getElementById('notif-panel-slot');
  const role = (auth.role() || 'customer').toLowerCase();
  slot.innerHTML = `
    <div class="notif-overlay" id="notif-overlay-bg"></div>
    <div class="notif-panel" role="dialog" aria-label="Notifications">
      <div class="notif-panel-header"><h3>🔔 Notifications</h3>
        <div style="display:flex;gap:.5rem">
          <button class="btn-sm btn-outline" id="notif-read-all" aria-label="Mark all read">✓ All</button>
          <button class="modal-close" id="notif-close" aria-label="Close">&times;</button>
        </div>
      </div>
      <div class="notif-panel-body" id="notif-list"><div class="text-center mt-2"><span class="spinner"></span></div></div>
      <div style="padding:.5rem 1rem;border-top:1px solid var(--border);text-align:center">
        <a href="#/${role}/notifications" id="notif-see-all" style="font-size:.82rem">See all notifications →</a>
      </div>
    </div>`;
  document.getElementById('notif-overlay-bg').addEventListener('click', () => { slot.innerHTML = ''; });
  document.getElementById('notif-close').addEventListener('click', () => { slot.innerHTML = ''; });
  document.getElementById('notif-see-all').addEventListener('click', () => { slot.innerHTML = ''; });
  document.getElementById('notif-read-all').addEventListener('click', async () => {
    try { await fetch('/api/v1/notifications/read-all', { method: 'PATCH', headers: { 'Authorization': 'Bearer ' + auth.token() } }); notifBadge.classList.add('hidden'); loadNotifs(); } catch {}
  });
  loadNotifs();
}
async function loadNotifs() {
  const list = document.getElementById('notif-list');
  if (!list) return;
  try {
    const res = await fetch('/api/v1/notifications?page=0&size=20', { headers: { 'Authorization': 'Bearer ' + auth.token() } });
    const json = await res.json();
    const items = json.data?.content || [];
    if (!items.length) { list.innerHTML = '<div class="empty-state"><span class="empty-icon">🔕</span><p>No notifications yet</p></div>'; return; }
    list.innerHTML = items.map(n => `
      <div class="notif-item ${n.read ? '' : 'unread'}" data-id="${n.id}">
        <span class="ni-icon">${NOTIF_ICONS[n.type] || '🔔'}</span>
        <div class="ni-body">
          <div class="ni-title">${esc(n.title || n.type)}</div>
          <div class="ni-text">${esc(n.body || '')}</div>
          <div class="ni-time">${n.createdAt ? timeAgo(new Date(n.createdAt)) : ''}</div>
        </div>
      </div>`).join('');
    list.querySelectorAll('.notif-item').forEach(el => {
      el.addEventListener('click', async () => {
        try { await fetch(`/api/v1/notifications/${el.dataset.id}/read`, { method: 'PATCH', headers: { 'Authorization': 'Bearer ' + auth.token() } }); el.classList.remove('unread'); pollUnread(); } catch {}
      });
    });
  } catch { list.innerHTML = '<p class="text-muted text-center">Could not load notifications</p>'; }
}

// ─── Onboarding overlay ─────────────────────────────────────────────
function showOnboarding(role) {
  const key = 'hc_onboarded_' + role;
  if (localStorage.getItem(key)) return;
  const steps = role === 'HELPER' ? [
    { icon: '🟢', title: 'Go Online', text: 'Toggle online from your dashboard to start receiving booking requests.' },
    { icon: '✅', title: 'Accept Bookings', text: 'Review incoming requests and accept to start earning.' },
    { icon: '💰', title: 'Complete & Earn', text: 'Finish jobs and get paid directly to your wallet.' },
  ] : role === 'ADMIN' ? [] : [
    { icon: '🏠', title: 'Book a Service', text: 'Choose from cleaning, cooking, babysitting, or elderly help.' },
    { icon: '📍', title: 'Track Your Helper', text: "Follow your helper's location in real-time on the map." },
    { icon: '⭐', title: 'Rate & Review', text: 'Share your experience to help the community.' },
  ];
  if (!steps.length) { localStorage.setItem(key, '1'); return; }
  let step = 0;
  const slot = document.getElementById('onboarding-slot');
  function renderOB() {
    const s = steps[step];
    slot.innerHTML = `<div class="onboarding-overlay" role="dialog" aria-label="Welcome tour"><div class="onboarding-card">
      <span class="ob-icon">${s.icon}</span><h2>${s.title}</h2><p>${s.text}</p>
      <div class="onboarding-dots">${steps.map((_, i) => `<span class="ob-dot ${i === step ? 'active' : ''}"></span>`).join('')}</div>
      <div style="display:flex;gap:.5rem;justify-content:center">
        ${step < steps.length - 1 ? `<button class="btn btn-primary" id="ob-next">Next →</button><button class="btn btn-outline" id="ob-skip">Skip</button>` : `<button class="btn btn-primary" id="ob-done">Get Started! 🚀</button>`}
      </div></div></div>`;
    (document.getElementById('ob-next') || document.getElementById('ob-done')).addEventListener('click', () => { if (step < steps.length - 1) { step++; renderOB(); } else dismiss(); });
    document.getElementById('ob-skip')?.addEventListener('click', dismiss);
  }
  function dismiss() { localStorage.setItem(key, '1'); slot.innerHTML = ''; promptPush(); }
  renderOB();
}
function promptPush() {
  if (!('Notification' in window) || Notification.permission !== 'default') return;
  setTimeout(() => { Notification.requestPermission().then(p => { if (p === 'granted') toast('Notifications enabled!', 'success'); }); }, 2000);
}

// ─── Responsive sidebar ↔ bottom-bar switch ─────────────────────────
let resizeTimer;
window.addEventListener('resize', () => {
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(() => {
    if (auth.isLoggedIn()) document.body.classList.toggle('has-sidebar', window.innerWidth >= 768);
  }, 100);
});

// ─── Start/stop notification polling on navigation ──────────────────
window.addEventListener('hashchange', () => {
  if (auth.isLoggedIn()) startNotifPolling(); else stopNotifPolling();
});

// ─── Boot ───────────────────────────────────────────────────────────
router.init();
if (auth.isLoggedIn()) { startNotifPolling(); showOnboarding(auth.role()); }
