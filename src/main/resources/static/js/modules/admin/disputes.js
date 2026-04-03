/**
 * admin/disputes.js — Admin dispute management: queue, assign, resolve.
 */
import { api } from '../../api.js';
import { toast } from '../../toast.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
let page = 0;

export async function render(container) {
  page = 0;
  container.innerHTML = `
    <div class="section-header"><h2>⚖️ Disputes</h2></div>
    <nav class="sub-nav">
      <a href="#/admin/dashboard" class="sub-nav-btn">Overview</a>
      <a href="#/admin/bookings" class="sub-nav-btn">Bookings</a>
      <a href="#/admin/disputes" class="sub-nav-btn active">Disputes</a>
      <a href="#/admin/referrals" class="sub-nav-btn">Referrals</a>
    </nav>
    <div class="card mb-2"><div class="filter-row">
      <select class="form-control" id="d-status" style="max-width:180px"><option value="">All statuses</option><option value="OPEN">Open</option><option value="UNDER_REVIEW">Under Review</option><option value="RESOLVED">Resolved</option></select>
      <button class="btn btn-primary btn-sm" id="d-filter">Filter</button>
    </div></div>
    <div class="card" style="overflow-x:auto"><div id="dispute-table"><div class="text-center mt-2"><span class="spinner"></span></div></div></div>
    <div id="dispute-pag"></div>
    <div id="dispute-modal-slot"></div>
  `;
  document.getElementById('d-filter').addEventListener('click', () => { page = 0; loadDisputes(); });
  loadDisputes();
}

async function loadDisputes() {
  const el = document.getElementById('dispute-table');
  const status = document.getElementById('d-status').value;
  try {
    const params = `page=${page}&size=20${status ? '&status=' + status : ''}`;
    const resp = await api.get(`/admin/disputes?${params}`);
    const items = resp.content || [];
    if (!items.length) { el.innerHTML = '<p class="text-muted text-center">No disputes found.</p>'; return; }
    el.innerHTML = `<table class="admin-table"><thead><tr><th>ID</th><th>Booking</th><th>Type</th><th>Raised By</th><th>Status</th><th>Actions</th></tr></thead><tbody>
      ${items.map(d => `<tr>
        <td style="font-size:.75rem">${(d.id||'').substring(0,8)}…</td>
        <td style="font-size:.75rem">${(d.bookingId||'').substring(0,8)}…</td>
        <td>${esc(d.disputeType||'')}</td>
        <td>${esc(d.raisedByName||d.raisedBy||'')}</td>
        <td><span class="badge ${d.status==='RESOLVED'?'badge-completed':'badge-pending'}">${esc(d.status)}</span></td>
        <td>
          ${d.status!=='RESOLVED'?`<button class="btn btn-sm btn-outline" data-assign="${d.id}">Assign</button> <button class="btn btn-sm btn-primary" data-resolve="${d.id}">Resolve</button>`:'—'}
        </td>
      </tr>`).join('')}
    </tbody></table>`;
    el.querySelectorAll('[data-assign]').forEach(btn => {
      btn.addEventListener('click', async () => {
        btn.disabled = true;
        try { await api.patch('/admin/disputes/' + btn.dataset.assign + '/assign'); toast('Assigned to you', 'success'); loadDisputes(); } catch { btn.disabled = false; }
      });
    });
    el.querySelectorAll('[data-resolve]').forEach(btn => {
      btn.addEventListener('click', () => showResolveModal(btn.dataset.resolve));
    });
  } catch { el.innerHTML = '<p class="text-muted text-center">Could not load disputes.</p>'; }
}

function showResolveModal(disputeId) {
  const slot = document.getElementById('dispute-modal-slot');
  slot.innerHTML = `<div class="modal-overlay" id="dm-overlay"><div class="modal-card">
    <div class="modal-header"><h3>Resolve Dispute</h3><button class="modal-close" id="dm-close">&times;</button></div>
    <div class="form-group"><label>Resolution</label><select class="form-control" id="dm-resolution">
      <option value="FULL_REFUND">Full Refund</option><option value="PARTIAL_REFUND">Partial Refund</option>
      <option value="NO_REFUND">No Refund</option><option value="RE_SERVICE">Re-Service</option><option value="WARNING_ISSUED">Warning Issued</option>
    </select></div>
    <div class="form-group"><label>Admin Notes</label><textarea class="form-control" id="dm-notes" rows="3" placeholder="Explain the resolution…"></textarea></div>
    <button class="btn btn-primary btn-block" id="dm-submit">Submit Resolution</button>
  </div></div>`;
  document.getElementById('dm-close').addEventListener('click', () => { slot.innerHTML = ''; });
  document.getElementById('dm-overlay').addEventListener('click', e => { if (e.target.id === 'dm-overlay') slot.innerHTML = ''; });
  document.getElementById('dm-submit').addEventListener('click', async () => {
    const btn = document.getElementById('dm-submit');
    btn.disabled = true; btn.textContent = 'Submitting…';
    try {
      await api.post('/admin/disputes/' + disputeId + '/resolve', {
        resolution: document.getElementById('dm-resolution').value,
        adminNotes: document.getElementById('dm-notes').value.trim()
      });
      toast('Dispute resolved', 'success'); slot.innerHTML = ''; loadDisputes();
    } catch { btn.disabled = false; btn.textContent = 'Submit Resolution'; }
  });
}

