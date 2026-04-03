/**
 * customer/disputes.js — View & raise disputes.
 */
import { api } from '../../api.js';
import { toast } from '../../toast.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

export async function render(container) {
  container.innerHTML = `
    <div class="section-header"><h2>⚖️ My Disputes</h2></div>
    <nav class="sub-nav">
      <a href="#/customer/home" class="sub-nav-btn">Home</a>
      <a href="#/customer/history" class="sub-nav-btn">Bookings</a>
      <a href="#/customer/disputes" class="sub-nav-btn active">Disputes</a>
    </nav>
    <div id="disputes-list"><div class="text-center mt-2"><span class="spinner"></span></div></div>
  `;
  try {
    const disputes = await api.get('/disputes/my');
    const el = document.getElementById('disputes-list');
    if (!disputes || !disputes.length) { el.innerHTML = '<div class="empty-state"><span class="empty-icon">✅</span><p>No disputes. That\'s great!</p></div>'; return; }
    el.innerHTML = disputes.map(d => {
      const badge = d.status==='RESOLVED'?'badge-completed':d.status==='OPEN'||d.status==='UNDER_REVIEW'?'badge-pending':'badge-cancelled';
      return `<div class="card mb-2">
        <div style="display:flex;justify-content:space-between;align-items:start">
          <div>
            <div style="font-weight:600">${esc(d.disputeType||'Issue')}</div>
            <div class="text-muted" style="font-size:.82rem">Booking #${(d.bookingId||'').substring(0,8)}…</div>
            <div class="text-muted" style="font-size:.78rem">${esc(d.description||'')}</div>
            <div class="text-muted" style="font-size:.75rem;margin-top:.25rem">${d.createdAt?new Date(d.createdAt).toLocaleDateString():''}</div>
          </div>
          <span class="badge ${badge}">${esc(d.status)}</span>
        </div>
        ${d.resolution?`<div style="margin-top:.5rem;padding:.5rem;background:var(--bg-alt);border-radius:var(--radius-sm);font-size:.82rem"><strong>Resolution:</strong> ${esc(d.resolution)} ${d.adminNotes?'— '+esc(d.adminNotes):''}</div>`:''}
      </div>`;
    }).join('');
  } catch { document.getElementById('disputes-list').innerHTML = '<div class="empty-state"><p>Could not load disputes. <button class="btn btn-outline btn-sm" onclick="location.reload()">Retry</button></p></div>'; }
}

