/**
 * admin/users.js — Admin user management.
 *
 * - Tabs: Customers / Helpers / Pending Verification
 * - Customer table: name, email, city, bookings count, last active, status
 * - Helper table: name, skills, rating, status, verified badge, jobs count
 * - Pending verification: [Verify] button, [View ID proof] link
 * - User detail slide-over panel
 */
import { api }    from '../../api.js';
import { toast }  from '../../toast.js';
import { router } from '../../router.js';

let currentTab = 'customers';
let currentPage = 0;
let totalPages = 0;

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

export async function render(container) {
  currentPage = 0;

  container.innerHTML = `
    <div class="section-header"><h2>Users</h2></div>
    <nav class="sub-nav">
      <a href="#/admin/dashboard" class="sub-nav-btn">Overview</a>
      <a href="#/admin/bookings"  class="sub-nav-btn">Bookings</a>
      <a href="#/admin/users"     class="sub-nav-btn active">Users</a>
      <a href="#/admin/helpers"   class="sub-nav-btn">Helpers</a>
      <a href="#/admin/payments"  class="sub-nav-btn">Payments</a>
      <a href="#/admin/config"    class="sub-nav-btn">Config</a>
    </nav>

    <!-- User Type Tabs -->
    <div class="toggle-row" id="user-tabs">
      <button class="toggle-btn active" data-tab="customers">Customers</button>
      <button class="toggle-btn" data-tab="helpers">Helpers</button>
      <button class="toggle-btn" data-tab="pending">Pending Verification</button>
    </div>

    <!-- Search -->
    <div class="card mb-2">
      <div class="filter-row">
        <input type="text" class="form-control" id="user-search" placeholder="Search by name or email…" style="max-width:300px">
        <button class="btn btn-primary btn-sm" id="user-search-btn">Search</button>
      </div>
    </div>

    <!-- Table -->
    <div class="card" style="overflow-x:auto">
      <div id="users-table-slot"></div>
    </div>

    <!-- Pagination -->
    <div class="pagination" id="users-pagination"></div>

    <!-- Detail Panel -->
    <div id="user-detail-panel"></div>
  `;

  // Tab switching
  const tabBtns = container.querySelectorAll('#user-tabs .toggle-btn');
  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      tabBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      currentTab = btn.dataset.tab;
      currentPage = 0;
      loadUsers();
    });
  });

  // Search
  document.getElementById('user-search-btn').addEventListener('click', () => { currentPage = 0; loadUsers(); });
  document.getElementById('user-search').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { currentPage = 0; loadUsers(); }
  });

  loadUsers();
}

async function loadUsers() {
  const slot = document.getElementById('users-table-slot');
  if (!slot) return;
  slot.innerHTML = '<div class="text-center text-muted"><span class="spinner"></span></div>';

  const search = (document.getElementById('user-search')?.value || '').trim();

  if (currentTab === 'pending') {
    await loadPendingVerification(slot, search);
    return;
  }

  const endpoint = currentTab === 'customers' ? '/admin/customers' : '/admin/helpers';
  let url = `${endpoint}?page=${currentPage}&size=15`;
  if (search) url += `&search=${encodeURIComponent(search)}`;

  try {
    const resp = await api.get(url);
    const users = resp?.content || [];
    totalPages = resp?.totalPages || 1;

    if (!users.length) {
      slot.innerHTML = '<p class="text-muted text-center">No users found.</p>';
      renderPagination();
      return;
    }

    if (currentTab === 'customers') {
      slot.innerHTML = `
        <table class="admin-table">
          <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Status</th><th>Last Login</th><th>Actions</th></tr></thead>
          <tbody>
            ${users.map(u => `
              <tr>
                <td>${esc(u.name)}</td>
                <td style="font-size:.8rem">${esc(u.email)}</td>
                <td style="font-size:.8rem">${esc(u.phone || '—')}</td>
                <td><span class="badge ${u.active ? 'badge-completed' : 'badge-cancelled'}">${u.active ? 'Active' : 'Inactive'}</span></td>
                <td style="font-size:.78rem">${u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleDateString() : '—'}</td>
                <td><button class="btn btn-outline btn-sm" data-detail-customer="${u.id}">View</button></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;
    } else {
      slot.innerHTML = `
        <table class="admin-table">
          <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Status</th><th>Verified</th><th>Last Login</th><th>Actions</th></tr></thead>
          <tbody>
            ${users.map(u => `
              <tr>
                <td>${esc(u.name)}</td>
                <td style="font-size:.8rem">${esc(u.email)}</td>
                <td style="font-size:.8rem">${esc(u.phone || '—')}</td>
                <td><span class="badge ${u.active ? 'badge-completed' : 'badge-cancelled'}">${u.active ? 'Active' : 'Inactive'}</span></td>
                <td>${u.emailVerified ? '✅' : '❌'}</td>
                <td style="font-size:.78rem">${u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleDateString() : '—'}</td>
                <td><button class="btn btn-outline btn-sm" data-detail-helper="${u.id}">View</button></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;
    }

    // View detail handlers
    slot.querySelectorAll('[data-detail-customer]').forEach(btn => {
      btn.addEventListener('click', () => openUserDetail(btn.dataset.detailCustomer, 'customer'));
    });
    slot.querySelectorAll('[data-detail-helper]').forEach(btn => {
      btn.addEventListener('click', () => openUserDetail(btn.dataset.detailHelper, 'helper'));
    });

    renderPagination();

  } catch {
    slot.innerHTML = '<p class="text-muted text-center">Failed to load users.</p>';
  }
}

async function loadPendingVerification(slot, search) {
  let url = '/admin/helpers/pending-verification?page=' + currentPage + '&size=15';

  try {
    const resp = await api.get(url);
    let helpers = resp?.content || [];
    totalPages = resp?.totalPages || 1;

    if (search) {
      helpers = helpers.filter(h =>
        (h.name || '').toLowerCase().includes(search.toLowerCase()) ||
        (h.email || '').toLowerCase().includes(search.toLowerCase())
      );
    }

    if (!helpers.length) {
      slot.innerHTML = '<p class="text-muted text-center">No helpers pending verification.</p>';
      renderPagination();
      return;
    }

    slot.innerHTML = `
      <table class="admin-table">
        <thead><tr><th>Name</th><th>Email</th><th>Skills</th><th>City</th><th>ID Proof</th><th>Actions</th></tr></thead>
        <tbody>
          ${helpers.map(h => `
            <tr>
              <td>${esc(h.name)}</td>
              <td style="font-size:.8rem">${esc(h.email)}</td>
              <td style="font-size:.8rem">${(h.skills || []).join(', ')}</td>
              <td>${esc(h.city || '—')}</td>
              <td>${h.idProofUrl ? `<a href="${esc(h.idProofUrl)}" target="_blank" class="btn btn-outline btn-sm">View ID</a>` : '—'}</td>
              <td>
                <button class="btn btn-primary btn-sm" data-verify="${h.userId}">✓ Verify</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;

    slot.querySelectorAll('[data-verify]').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.verify;
        btn.disabled = true;
        try {
          await api.patch('/admin/helpers/' + id + '/verify');
          toast('Helper verified!', 'success');
          loadUsers(); // refresh
        } catch {
          btn.disabled = false;
        }
      });
    });

    renderPagination();

  } catch {
    slot.innerHTML = '<p class="text-muted text-center">Failed to load pending verifications.</p>';
  }
}

function renderPagination() {
  const slot = document.getElementById('users-pagination');
  if (!slot) return;
  if (totalPages <= 1) { slot.innerHTML = ''; return; }

  let html = `<button class="page-btn" ${currentPage === 0 ? 'disabled' : ''} data-page="${currentPage - 1}">‹ Prev</button>`;
  html += `<span class="page-info">Page ${currentPage + 1} of ${totalPages}</span>`;
  html += `<button class="page-btn" ${currentPage >= totalPages - 1 ? 'disabled' : ''} data-page="${currentPage + 1}">Next ›</button>`;

  slot.innerHTML = html;
  slot.querySelectorAll('.page-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const page = parseInt(btn.dataset.page);
      if (page >= 0 && page < totalPages) {
        currentPage = page;
        loadUsers();
      }
    });
  });
}

async function openUserDetail(userId, type) {
  const slot = document.getElementById('user-detail-panel');
  if (!slot) return;

  slot.innerHTML = `
    <div class="slideover-overlay" id="slideover-overlay">
      <div class="slideover-panel">
        <div class="modal-header">
          <h3>User Detail</h3>
          <button class="modal-close" id="slideover-close">&times;</button>
        </div>
        <div class="text-center mt-2"><span class="spinner"></span></div>
      </div>
    </div>
  `;

  const closePanel = () => { slot.innerHTML = ''; };
  document.getElementById('slideover-close').addEventListener('click', closePanel);
  document.getElementById('slideover-overlay').addEventListener('click', (e) => {
    if (e.target.id === 'slideover-overlay') closePanel();
  });

  try {
    const endpoint = type === 'customer' ? '/admin/customers/' : '/admin/helpers/';
    const detail = await api.get(endpoint + userId);
    const panel = slot.querySelector('.slideover-panel');

    if (type === 'customer') {
      const p = detail.profile || {};
      const bookings = detail.recentBookings || [];
      panel.innerHTML = `
        <div class="modal-header">
          <h3>${esc(p.name || 'Customer')}</h3>
          <button class="modal-close" id="slideover-close2">&times;</button>
        </div>
        <div class="mt-1">
          <p><strong>Email:</strong> ${esc(p.email || '—')}</p>
          <p><strong>Phone:</strong> ${esc(p.phone || '—')}</p>
          <p><strong>City:</strong> ${esc(p.city || '—')}</p>
          <p><strong>Total Bookings:</strong> ${detail.totalBookings || 0}</p>
          ${detail.wallet ? `<p><strong>Wallet:</strong> ₹${parseFloat(detail.wallet.balance || 0).toLocaleString('en-IN')}</p>` : ''}
        </div>
        <h4 style="font-size:.85rem;margin:.75rem 0 .5rem">Recent Bookings</h4>
        ${bookings.length ? `
          <ul class="data-list">
            ${bookings.slice(0, 5).map(b => `
              <li class="data-list-item">
                <div>${esc(b.serviceType)} — <span class="badge ${getBadgeClass(b.status)}">${esc((b.status || '').replace(/_/g, ' '))}</span></div>
                <small class="text-muted">₹${b.totalPrice || 0}</small>
              </li>
            `).join('')}
          </ul>
        ` : '<p class="text-muted">No bookings.</p>'}
      `;
    } else {
      const p = detail.profile || {};
      const bookings = detail.recentBookings || [];
      const reviews = detail.recentReviews || [];
      panel.innerHTML = `
        <div class="modal-header">
          <h3>${esc(p.name || 'Helper')}</h3>
          <button class="modal-close" id="slideover-close2">&times;</button>
        </div>
        <div class="mt-1">
          <p><strong>Email:</strong> ${esc(p.email || '—')}</p>
          <p><strong>Phone:</strong> ${esc(p.phone || '—')}</p>
          <p><strong>Skills:</strong> ${(p.skills || []).join(', ') || '—'}</p>
          <p><strong>City:</strong> ${esc(p.city || '—')}</p>
          <p><strong>Rating:</strong> ⭐ ${p.rating ? p.rating.toFixed(1) : '—'}</p>
          <p><strong>Status:</strong> ${esc(p.status || '—')}</p>
          <p><strong>Verified:</strong> ${p.backgroundVerified ? '✅' : '❌'}</p>
          <p><strong>Total Jobs:</strong> ${p.totalJobsCompleted || 0}</p>
          <p><strong>Total Earnings:</strong> ₹${parseFloat(detail.totalEarnings || 0).toLocaleString('en-IN')}</p>
        </div>
        <h4 style="font-size:.85rem;margin:.75rem 0 .5rem">Recent Bookings</h4>
        ${bookings.length ? `
          <ul class="data-list">
            ${bookings.slice(0, 5).map(b => `
              <li class="data-list-item">
                <div>${esc(b.serviceType)} — <span class="badge ${getBadgeClass(b.status)}">${esc((b.status || '').replace(/_/g, ' '))}</span></div>
                <small class="text-muted">₹${b.totalPrice || 0}</small>
              </li>
            `).join('')}
          </ul>
        ` : '<p class="text-muted">No bookings.</p>'}
        <h4 style="font-size:.85rem;margin:.75rem 0 .5rem">Recent Reviews</h4>
        ${reviews.length ? reviews.slice(0, 3).map(r => `
          <div class="card mb-2" style="padding:.75rem">
            <div>⭐ ${r.rating || '—'}</div>
            <p style="font-size:.85rem">${esc(r.comment || r.reviewText || '—')}</p>
          </div>
        `).join('') : '<p class="text-muted">No reviews.</p>'}
      `;
    }

    const close2 = document.getElementById('slideover-close2');
    if (close2) close2.addEventListener('click', closePanel);

  } catch {
    const panel = slot.querySelector('.slideover-panel');
    if (panel) panel.innerHTML = '<p class="text-muted">Could not load user details.</p>';
  }
}

function getBadgeClass(status) {
  switch (status) {
    case 'PENDING_ASSIGNMENT': return 'badge-pending';
    case 'ASSIGNED': return 'badge-assigned';
    case 'HELPER_EN_ROUTE':
    case 'IN_PROGRESS': return 'badge-progress';
    case 'COMPLETED': return 'badge-completed';
    case 'CANCELLED': return 'badge-cancelled';
    default: return 'badge-pending';
  }
}
