/**
 * customer/notifications.js — Full notifications list (shared by customer & helper).
 */
import { api } from '../../api.js';
import { auth } from '../../auth.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
function timeAgo(d) { const s=Math.floor((Date.now()-new Date(d).getTime())/1000); if(s<60)return'just now';if(s<3600)return Math.floor(s/60)+'m ago';if(s<86400)return Math.floor(s/3600)+'h ago';return Math.floor(s/86400)+'d ago';}
const ICONS = { BOOKING_CONFIRMED:'📋',BOOKING_ASSIGNED:'✅',HELPER_EN_ROUTE:'🚗',BOOKING_COMPLETED:'🎉',BOOKING_CANCELLED:'❌',BOOKING_REMINDER:'⏰',PAYMENT_SUCCESS:'💰',PAYMENT_REFUND:'↩️',NEW_REVIEW:'⭐',WALLET_LOW:'⚠️',SYSTEM_ALERT:'📢',CHAT_MESSAGE:'💬',SUBSCRIPTION_STARTED:'📋',SUBSCRIPTION_RENEWED:'🔄',SUBSCRIPTION_CANCELLED:'❌',WALLET_INSUFFICIENT_FOR_RENEWAL:'💸',DISPUTE_RAISED:'⚖️',DISPUTE_RESOLVED:'✅',REFERRAL_SIGNUP_CREDIT:'🎁',REFERRAL_BONUS_CREDIT:'🎉' };

let page = 0;
export async function render(container) {
  page = 0;
  const role = (auth.role()||'customer').toLowerCase();
  container.innerHTML = `
    <div class="section-header"><h2>🔔 Notifications</h2>
      <button class="btn btn-outline btn-sm" id="mark-all-btn">✓ Mark all read</button>
    </div>
    <nav class="sub-nav">
      <a href="#/${role}/home" class="sub-nav-btn">Home</a>
      <a href="#/${role}/notifications" class="sub-nav-btn active">Notifications</a>
    </nav>
    <div id="notif-full-list"><div class="text-center mt-2"><span class="spinner"></span></div></div>
    <div id="notif-full-pag"></div>
  `;
  document.getElementById('mark-all-btn').addEventListener('click', async () => {
    try { await api.patch('/notifications/read-all'); loadPage(); } catch {}
  });
  loadPage();
}

async function loadPage() {
  const list = document.getElementById('notif-full-list');
  const pag = document.getElementById('notif-full-pag');
  try {
    const resp = await api.get(`/notifications?page=${page}&size=20`);
    const items = resp.content || [];
    const totalPages = resp.totalPages || 1;
    if (!items.length && page === 0) { list.innerHTML = '<div class="empty-state"><span class="empty-icon">🔕</span><p>No notifications yet.</p></div>'; pag.innerHTML=''; return; }
    list.innerHTML = items.map(n => `
      <div class="notif-item ${n.read?'':'unread'}" data-id="${n.id}" style="margin-bottom:.25rem">
        <span class="ni-icon">${ICONS[n.type]||'🔔'}</span>
        <div class="ni-body">
          <div class="ni-title">${esc(n.title||n.type)}</div>
          <div class="ni-text">${esc(n.body||'')}</div>
          <div class="ni-time">${n.createdAt?timeAgo(n.createdAt):''}</div>
        </div>
      </div>`).join('');
    list.querySelectorAll('.notif-item').forEach(el => {
      el.addEventListener('click', async () => {
        try { await api.patch(`/notifications/${el.dataset.id}/read`); el.classList.remove('unread'); } catch {}
      });
    });
    if (totalPages > 1) {
      pag.innerHTML = `<div class="pagination"><button class="page-btn" ${page===0?'disabled':''} id="np">‹</button><span class="page-info">Page ${page+1} of ${totalPages}</span><button class="page-btn" ${page>=totalPages-1?'disabled':''} id="nn">›</button></div>`;
      document.getElementById('np')?.addEventListener('click',()=>{if(page>0){page--;loadPage();}});
      document.getElementById('nn')?.addEventListener('click',()=>{if(page<totalPages-1){page++;loadPage();}});
    } else pag.innerHTML='';
  } catch { list.innerHTML = '<p class="text-muted text-center">Could not load notifications. <button class="btn btn-outline btn-sm" id="retry-notif">Retry</button></p>'; document.getElementById('retry-notif')?.addEventListener('click', loadPage); }
}

