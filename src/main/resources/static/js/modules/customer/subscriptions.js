/**
 * customer/subscriptions.js — Browse plans, subscribe, view active subscriptions.
 */
import { api } from '../../api.js';
import { toast } from '../../toast.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

export async function render(container) {
  container.innerHTML = `
    <div class="section-header"><h2>📋 Subscriptions</h2></div>
    <nav class="sub-nav">
      <a href="#/customer/home" class="sub-nav-btn">Home</a>
      <a href="#/customer/subscriptions" class="sub-nav-btn active">Subscriptions</a>
      <a href="#/customer/wallet" class="sub-nav-btn">Wallet</a>
    </nav>

    <!-- My active subscriptions -->
    <div id="my-subs"><div class="text-center mt-2"><span class="spinner"></span></div></div>

    <!-- Available plans -->
    <h3 style="font-size:.95rem;margin:1.5rem 0 .75rem">Available Plans</h3>
    <div class="grid-3" id="plans-grid"><div class="text-center text-muted"><span class="spinner"></span></div></div>
  `;

  loadMySubs();
  loadPlans();
}

async function loadMySubs() {
  const el = document.getElementById('my-subs');
  try {
    const subs = await api.get('/subscriptions/my');
    if (!subs || !subs.length) { el.innerHTML = '<div class="card mb-2"><p class="text-muted text-center">No active subscriptions. Browse plans below!</p></div>'; return; }
    el.innerHTML = subs.map(s => `
      <div class="card mb-2">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div>
            <div style="font-weight:700">${esc(s.planName || 'Plan')}</div>
            <div class="text-muted" style="font-size:.82rem">${esc(s.serviceType || '')} · ${s.sessionsUsed || 0}/${s.totalSessions || '∞'} sessions</div>
            <div class="text-muted" style="font-size:.78rem">Next renewal: ${s.nextRenewalAt ? new Date(s.nextRenewalAt).toLocaleDateString() : '—'}</div>
          </div>
          <div style="text-align:right">
            <span class="badge ${s.status==='ACTIVE'?'badge-completed':s.status==='PAUSED'?'badge-pending':'badge-cancelled'}">${esc(s.status)}</span>
            ${s.status === 'ACTIVE' ? `<br><button class="btn btn-outline btn-sm mt-1" data-cancel="${s.id}">Cancel</button>` : ''}
          </div>
        </div>
      </div>`).join('');
    el.querySelectorAll('[data-cancel]').forEach(btn => {
      btn.addEventListener('click', async () => {
        if (!confirm('Cancel this subscription?')) return;
        btn.disabled = true;
        try { await api.delete('/subscriptions/' + btn.dataset.cancel); toast('Subscription cancelled', 'info'); loadMySubs(); } catch { btn.disabled = false; }
      });
    });
  } catch { el.innerHTML = '<p class="text-muted text-center">Could not load subscriptions. <button class="btn btn-outline btn-sm" onclick="location.reload()">Retry</button></p>'; }
}

async function loadPlans() {
  const el = document.getElementById('plans-grid');
  try {
    const plans = await api.get('/subscription-plans');
    if (!plans || !plans.length) { el.innerHTML = '<p class="text-muted">No plans available right now.</p>'; return; }
    el.innerHTML = plans.map(p => `
      <div class="plan-card">
        <div class="plan-name">${esc(p.name || 'Plan')}</div>
        <div class="plan-price">₹${p.price || 0}</div>
        <div class="plan-cycle">${esc(p.cyclePeriod || 'MONTHLY')}</div>
        <div class="plan-sessions">${p.sessionsPerCycle || '∞'} sessions/cycle</div>
        <div class="text-muted" style="font-size:.78rem;margin-bottom:.5rem">${esc(p.serviceType || '')}</div>
        <button class="btn btn-primary btn-sm btn-block" data-plan="${p.id}">Subscribe</button>
      </div>`).join('');
    el.querySelectorAll('[data-plan]').forEach(btn => {
      btn.addEventListener('click', async () => {
        btn.disabled = true; btn.textContent = 'Subscribing…';
        try { await api.post('/subscriptions', { planId: btn.dataset.plan }); toast('Subscribed! 🎉', 'success'); loadMySubs(); loadPlans(); } catch { btn.disabled = false; btn.textContent = 'Subscribe'; }
      });
    });
  } catch { el.innerHTML = '<p class="text-muted">Could not load plans. <button class="btn btn-outline btn-sm" onclick="location.reload()">Retry</button></p>'; }
}

