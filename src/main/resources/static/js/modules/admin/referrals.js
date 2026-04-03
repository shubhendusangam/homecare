/**
 * admin/referrals.js — Admin referral analytics.
 */
import { api } from '../../api.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
let page = 0;

export async function render(container) {
  page = 0;
  container.innerHTML = `
    <div class="section-header"><h2>🎁 Referrals</h2></div>
    <nav class="sub-nav">
      <a href="#/admin/dashboard" class="sub-nav-btn">Overview</a>
      <a href="#/admin/disputes" class="sub-nav-btn">Disputes</a>
      <a href="#/admin/referrals" class="sub-nav-btn active">Referrals</a>
    </nav>
    <div class="stats-grid" id="ref-kpis">
      <div class="stat-card"><div class="stat-value" id="rk-signups">—</div><div class="stat-label">Total Signups</div></div>
      <div class="stat-card"><div class="stat-value" id="rk-conv">—</div><div class="stat-label">Conversions</div></div>
      <div class="stat-card"><div class="stat-value" id="rk-rate">—</div><div class="stat-label">Conversion %</div></div>
      <div class="stat-card"><div class="stat-value" id="rk-credits">—</div><div class="stat-label">Credits Issued</div></div>
    </div>
    <div class="card mb-2"><div class="filter-row">
      <select class="form-control" id="rf-status" style="max-width:180px"><option value="">All statuses</option><option value="SIGNUP_DONE">Signup Done</option><option value="CREDIT_ISSUED">Credit Issued</option><option value="EXPIRED">Expired</option></select>
      <button class="btn btn-primary btn-sm" id="rf-filter">Filter</button>
    </div></div>
    <div class="card" style="overflow-x:auto"><div id="ref-table"><div class="text-center mt-2"><span class="spinner"></span></div></div></div>
    <div id="ref-pag"></div>
  `;
  document.getElementById('rf-filter').addEventListener('click', () => { page = 0; loadEvents(); });
  loadSummary();
  loadEvents();
}

async function loadSummary() {
  try {
    const s = await api.get('/admin/referrals/summary');
    document.getElementById('rk-signups').textContent = s.totalSignups || 0;
    document.getElementById('rk-conv').textContent = s.totalConversions || 0;
    document.getElementById('rk-rate').textContent = (s.conversionRate || 0) + '%';
    document.getElementById('rk-credits').textContent = '₹' + (s.totalCreditsIssued || 0);
  } catch {}
}

async function loadEvents() {
  const el = document.getElementById('ref-table');
  const pag = document.getElementById('ref-pag');
  const status = document.getElementById('rf-status').value;
  try {
    const params = `page=${page}&size=20${status ? '&status=' + status : ''}`;
    const resp = await api.get(`/admin/referrals?${params}`);
    const items = resp.content || [];
    const totalPages = resp.totalPages || 1;
    if (!items.length) { el.innerHTML = '<p class="text-muted text-center">No referral events.</p>'; pag.innerHTML = ''; return; }
    el.innerHTML = `<table class="admin-table"><thead><tr><th>Referrer</th><th>Referee</th><th>Code</th><th>Status</th><th>Referrer Credit</th><th>Date</th></tr></thead><tbody>
      ${items.map(r => `<tr>
        <td>${esc(r.referrerName||'')}</td><td>${esc(r.refereeName||'')}</td>
        <td><code>${esc(r.referralCode||'')}</code></td>
        <td><span class="badge ${r.status==='CREDIT_ISSUED'?'badge-completed':r.status==='EXPIRED'?'badge-cancelled':'badge-pending'}">${esc(r.status)}</span></td>
        <td>₹${r.referrerCredit||0} ${r.referrerCreditIssued?'✅':''}</td>
        <td style="font-size:.78rem">${r.createdAt?new Date(r.createdAt).toLocaleDateString():''}</td>
      </tr>`).join('')}
    </tbody></table>`;
    if (totalPages > 1) {
      pag.innerHTML = `<div class="pagination"><button class="page-btn" ${page===0?'disabled':''} id="rp">‹</button><span class="page-info">Page ${page+1} of ${totalPages}</span><button class="page-btn" ${page>=totalPages-1?'disabled':''} id="rn">›</button></div>`;
      document.getElementById('rp')?.addEventListener('click',()=>{if(page>0){page--;loadEvents();}});
      document.getElementById('rn')?.addEventListener('click',()=>{if(page<totalPages-1){page++;loadEvents();}});
    } else pag.innerHTML = '';
  } catch { el.innerHTML = '<p class="text-muted text-center">Could not load referral data.</p>'; pag.innerHTML = ''; }
}

