/**
 * admin/payments.js — Admin payment reports: revenue overview, transaction list.
 */
import { api } from '../../api.js';
import { toast } from '../../toast.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
function fmtCur(n) { return '₹' + Number(n || 0).toLocaleString('en-IN'); }
let page = 0;

export async function render(container) {
  page = 0;
  container.innerHTML = `
    <div class="section-header"><h2>💳 Payments</h2></div>
    <nav class="sub-nav">
      <a href="#/admin/dashboard" class="sub-nav-btn">Overview</a>
      <a href="#/admin/bookings" class="sub-nav-btn">Bookings</a>
      <a href="#/admin/users" class="sub-nav-btn">Users</a>
      <a href="#/admin/helpers" class="sub-nav-btn">Helpers</a>
      <a href="#/admin/payments" class="sub-nav-btn active">Payments</a>
      <a href="#/admin/config" class="sub-nav-btn">Config</a>
    </nav>

    <!-- Revenue KPIs -->
    <div class="stats-grid" id="pay-kpis">
      <div class="stat-card"><div class="stat-value" id="pk-revenue">—</div><div class="stat-label">Today Revenue</div></div>
      <div class="stat-card"><div class="stat-value" id="pk-bookings">—</div><div class="stat-label">Today Bookings</div></div>
      <div class="stat-card"><div class="stat-value" id="pk-refunds">—</div><div class="stat-label">Pending Refunds</div></div>
      <div class="stat-card"><div class="stat-value" id="pk-platform">—</div><div class="stat-label">Platform Fees</div></div>
    </div>

    <!-- Transaction list -->
    <div class="card" style="overflow-x:auto">
      <h3 style="font-size:.95rem;margin-bottom:.75rem">Recent Transactions</h3>
      <div id="pay-table"><div class="text-center mt-2"><span class="spinner"></span></div></div>
    </div>
    <div id="pay-pag"></div>
  `;

  loadKPIs();
  loadTransactions();
}

async function loadKPIs() {
  try {
    const d = await api.get('/admin/dashboard');
    document.getElementById('pk-revenue').textContent = fmtCur(d.todayRevenue);
    document.getElementById('pk-bookings').textContent = d.todayBookings || 0;
    document.getElementById('pk-refunds').textContent = d.pendingRefunds || 0;
    document.getElementById('pk-platform').textContent = fmtCur(d.platformFees || 0);
  } catch {}
}

async function loadTransactions() {
  const el = document.getElementById('pay-table');
  try {
    const resp = await api.get(`/admin/bookings?page=${page}&size=20`);
    const items = resp.content || [];
    if (!items.length) { el.innerHTML = '<p class="text-muted text-center">No transactions found.</p>'; return; }
    el.innerHTML = `<table class="admin-table"><thead><tr><th>Booking</th><th>Customer</th><th>Service</th><th>Amount</th><th>Payment</th><th>Status</th></tr></thead><tbody>
      ${items.map(b => `<tr>
        <td style="font-size:.75rem">${(b.id||'').substring(0,8)}…</td>
        <td>${esc(b.customerName||'—')}</td>
        <td>${esc(b.serviceType||'—')}</td>
        <td>${fmtCur(b.totalPrice)}</td>
        <td><span class="badge ${b.paymentStatus==='PAID'?'badge-completed':b.paymentStatus==='REFUNDED'?'badge-cancelled':'badge-pending'}">${esc(b.paymentStatus||'')}</span></td>
        <td><span class="badge ${b.status==='COMPLETED'?'badge-completed':b.status==='CANCELLED'?'badge-cancelled':'badge-pending'}">${esc(b.status||'')}</span></td>
      </tr>`).join('')}
    </tbody></table>`;
  } catch { el.innerHTML = '<p class="text-muted text-center">Could not load transactions.</p>'; }
}
