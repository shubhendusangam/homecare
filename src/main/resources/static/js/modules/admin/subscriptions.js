/**
 * admin/subscriptions.js — Manage subscription plans & view active subscriptions.
 */
import { api } from '../../api.js';
import { toast } from '../../toast.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

export async function render(container) {
  container.innerHTML = `
    <div class="section-header"><h2>📋 Subscriptions</h2></div>
    <nav class="sub-nav">
      <a href="#/admin/dashboard" class="sub-nav-btn">Overview</a>
      <a href="#/admin/config" class="sub-nav-btn">Config</a>
      <a href="#/admin/subscriptions" class="sub-nav-btn active">Subscriptions</a>
    </nav>
    <h3 style="font-size:.95rem;margin-bottom:.75rem">Subscription Plans</h3>
    <div id="plans-list"><div class="text-center mt-2"><span class="spinner"></span></div></div>
    <h3 style="font-size:.95rem;margin:1.5rem 0 .75rem">Active Subscriptions</h3>
    <div class="card" style="overflow-x:auto"><div id="subs-table"><div class="text-center mt-2"><span class="spinner"></span></div></div></div>
  `;
  loadPlans();
  loadSubs();
}

async function loadPlans() {
  const el = document.getElementById('plans-list');
  try {
    const plans = await api.get('/admin/subscription-plans');
    if (!plans || !plans.length) { el.innerHTML = '<p class="text-muted">No plans configured.</p>'; return; }
    el.innerHTML = `<div class="grid-3">${plans.map(p => `
      <div class="plan-card">
        <div class="plan-name">${esc(p.name)}</div>
        <div class="plan-price">₹${p.price}</div>
        <div class="plan-cycle">${esc(p.cyclePeriod)} · ${p.sessionsPerCycle} sessions</div>
        <div class="text-muted" style="font-size:.78rem">${esc(p.serviceType||'')} · ${p.active?'✅ Active':'❌ Inactive'}</div>
      </div>`).join('')}</div>`;
  } catch { el.innerHTML = '<p class="text-muted">Could not load plans.</p>'; }
}

async function loadSubs() {
  const el = document.getElementById('subs-table');
  try {
    const resp = await api.get('/admin/subscriptions?page=0&size=20');
    const items = resp.content || [];
    if (!items.length) { el.innerHTML = '<p class="text-muted text-center">No active subscriptions.</p>'; return; }
    el.innerHTML = `<table class="admin-table"><thead><tr><th>Customer</th><th>Plan</th><th>Status</th><th>Sessions</th><th>Next Renewal</th></tr></thead><tbody>
      ${items.map(s => `<tr><td>${esc(s.customerName||'')}</td><td>${esc(s.planName||'')}</td>
        <td><span class="badge ${s.status==='ACTIVE'?'badge-completed':'badge-pending'}">${esc(s.status)}</span></td>
        <td>${s.sessionsUsed||0}/${s.totalSessions||'∞'}</td><td>${s.nextRenewalAt?new Date(s.nextRenewalAt).toLocaleDateString():'—'}</td></tr>`).join('')}
    </tbody></table>`;
  } catch { el.innerHTML = '<p class="text-muted text-center">Could not load subscriptions.</p>'; }
}

