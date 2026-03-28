/**
 * admin/bookings.js — Admin bookings management.
 *
 * - Full booking table with filters
 * - Row click → booking detail modal with status timeline, map, reassign, force complete/cancel
 * - Server-side pagination + client-side search
 */
import { api }    from '../../api.js';
import { toast }  from '../../toast.js';
import { router } from '../../router.js';

const STATUSES = ['', 'PENDING_ASSIGNMENT', 'ASSIGNED', 'HELPER_EN_ROUTE', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
const SERVICES = ['', 'CLEANING', 'COOKING', 'BABYSITTING', 'ELDERLY_HELP'];

let currentPage = 0;
let totalPages = 0;
let detailMap = null;

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

function cleanup() {
  if (detailMap) { detailMap.remove(); detailMap = null; }
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

export async function render(container) {
  cleanup();
  router.onTeardown(cleanup);
  currentPage = 0;

  container.innerHTML = `
    <div class="section-header"><h2>All Bookings</h2></div>
    <nav class="sub-nav">
      <a href="#/admin/dashboard" class="sub-nav-btn">Overview</a>
      <a href="#/admin/bookings"  class="sub-nav-btn active">Bookings</a>
      <a href="#/admin/users"     class="sub-nav-btn">Users</a>
      <a href="#/admin/helpers"   class="sub-nav-btn">Helpers</a>
      <a href="#/admin/payments"  class="sub-nav-btn">Payments</a>
      <a href="#/admin/config"    class="sub-nav-btn">Config</a>
    </nav>

    <!-- Filters -->
    <div class="card mb-2">
      <div class="filter-row">
        <select class="form-control" id="filter-status" style="max-width:180px">
          ${STATUSES.map(s => `<option value="${s}">${s || 'All Statuses'}</option>`).join('')}
        </select>
        <select class="form-control" id="filter-service" style="max-width:180px">
          ${SERVICES.map(s => `<option value="${s}">${s ? s.replace(/_/g, ' ') : 'All Services'}</option>`).join('')}
        </select>
        <input type="text" class="form-control" id="filter-search" placeholder="Search customer/helper…" style="max-width:220px">
        <button class="btn btn-primary btn-sm" id="filter-apply">Apply</button>
      </div>
    </div>

    <!-- Table -->
    <div class="card" style="overflow-x:auto">
      <table class="admin-table" id="bookings-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Customer</th>
            <th>Helper</th>
            <th>Service</th>
            <th>Status</th>
            <th>Amount</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody id="bookings-tbody"></tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination" id="bookings-pagination"></div>

    <!-- Detail Modal -->
    <div id="booking-detail-modal"></div>
  `;

  // Filter handlers
  document.getElementById('filter-apply').addEventListener('click', () => {
    currentPage = 0;
    loadBookings();
  });

  // Enter key on search
  document.getElementById('filter-search').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { currentPage = 0; loadBookings(); }
  });

  loadBookings();
}

async function loadBookings() {
  const tbody = document.getElementById('bookings-tbody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted"><span class="spinner"></span></td></tr>';

  const status = document.getElementById('filter-status')?.value || '';
  const service = document.getElementById('filter-service')?.value || '';
  const search = (document.getElementById('filter-search')?.value || '').trim().toLowerCase();

  let url = `/admin/bookings?page=${currentPage}&size=15`;
  if (status) url += `&status=${status}`;
  if (service) url += `&serviceType=${service}`;

  try {
    const resp = await api.get(url);
    let bookings = resp?.content || [];
    totalPages = resp?.totalPages || 1;

    // Client-side search filter
    if (search) {
      bookings = bookings.filter(b =>
        (b.customerName || '').toLowerCase().includes(search) ||
        (b.helperName || '').toLowerCase().includes(search) ||
        (b.id || '').toLowerCase().includes(search)
      );
    }

    if (!bookings.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No bookings found.</td></tr>';
      renderPagination();
      return;
    }

    tbody.innerHTML = bookings.map(b => {
      const statusText = (b.status || '').replace(/_/g, ' ');
      const dt = b.createdAt ? new Date(b.createdAt).toLocaleDateString(undefined, { dateStyle: 'medium' }) : '—';
      return `
        <tr class="clickable-row" data-id="${b.id}">
          <td style="font-size:.75rem;font-family:monospace">${(b.id || '').substring(0, 8)}…</td>
          <td>${esc(b.customerName || '—')}</td>
          <td>${esc(b.helperName || '—')}</td>
          <td>${esc((b.serviceType || '').replace(/_/g, ' '))}</td>
          <td><span class="badge ${getBadgeClass(b.status)}">${esc(statusText)}</span></td>
          <td>₹${b.totalPrice || 0}</td>
          <td style="font-size:.78rem">${esc(dt)}</td>
        </tr>
      `;
    }).join('');

    // Row click → detail modal
    tbody.querySelectorAll('.clickable-row').forEach(row => {
      row.addEventListener('click', () => openDetailModal(row.dataset.id));
    });

    renderPagination();

  } catch {
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">Failed to load bookings.</td></tr>';
  }
}

function renderPagination() {
  const slot = document.getElementById('bookings-pagination');
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
        loadBookings();
      }
    });
  });
}

async function openDetailModal(bookingId) {
  const slot = document.getElementById('booking-detail-modal');
  if (!slot) return;

  slot.innerHTML = `
    <div class="modal-overlay" id="detail-overlay">
      <div class="modal-card" style="max-width:650px">
        <div class="modal-header">
          <h3>Booking Detail</h3>
          <button class="modal-close" id="detail-close">&times;</button>
        </div>
        <div class="text-center"><span class="spinner"></span></div>
      </div>
    </div>
  `;

  const closeModal = () => {
    if (detailMap) { detailMap.remove(); detailMap = null; }
    slot.innerHTML = '';
  };
  document.getElementById('detail-close').addEventListener('click', closeModal);
  document.getElementById('detail-overlay').addEventListener('click', (e) => {
    if (e.target.id === 'detail-overlay') closeModal();
  });

  try {
    const detail = await api.get('/admin/bookings/' + bookingId);
    const b = detail.booking || {};
    const history = detail.statusHistory || [];
    const trail = detail.locationTrail || [];
    const statusText = (b.status || '').replace(/_/g, ' ');

    const card = slot.querySelector('.modal-card');
    card.innerHTML = `
      <div class="modal-header">
        <h3>Booking #${(b.id || '').substring(0, 8)}</h3>
        <button class="modal-close" id="detail-close2">&times;</button>
      </div>

      <!-- Info -->
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:.5rem;margin-bottom:1rem">
        <div><strong>Customer:</strong> ${esc(b.customerName || '—')}</div>
        <div><strong>Helper:</strong> ${esc(b.helperName || '—')}</div>
        <div><strong>Service:</strong> ${esc((b.serviceType || '').replace(/_/g, ' '))}</div>
        <div><strong>Amount:</strong> ₹${b.totalPrice || 0}</div>
        <div><strong>Status:</strong> <span class="badge ${getBadgeClass(b.status)}">${esc(statusText)}</span></div>
        <div><strong>Duration:</strong> ${b.durationHours || 0}h</div>
      </div>

      <!-- Status History Timeline -->
      <h4 style="font-size:.85rem;margin-bottom:.5rem">Status History</h4>
      <div class="timeline">
        ${history.length ? history.map((h, i) => `
          <div class="timeline-item ${i === history.length - 1 ? 'active' : ''}">
            <div class="tl-label">${esc((h.toStatus || '').replace(/_/g, ' '))}</div>
            <div class="tl-time">${h.changedAt ? new Date(h.changedAt).toLocaleString() : '—'}${h.reason ? ' — ' + esc(h.reason) : ''}</div>
          </div>
        `).join('') : '<div class="text-muted" style="font-size:.82rem">No status changes recorded.</div>'}
      </div>

      <!-- Route Map -->
      ${trail.length ? `
        <h4 style="font-size:.85rem;margin:.75rem 0 .5rem">Route Map</h4>
        <div id="detail-map" style="height:200px;border-radius:var(--radius);overflow:hidden"></div>
      ` : ''}

      <!-- Admin Actions -->
      <h4 style="font-size:.85rem;margin:.75rem 0 .5rem">Actions</h4>
      <div id="admin-actions" style="display:flex;flex-wrap:wrap;gap:.5rem">
        ${b.status !== 'COMPLETED' && b.status !== 'CANCELLED' ? `
          <div style="flex:1;min-width:200px">
            <label style="font-size:.8rem;font-weight:600">Reassign Helper</label>
            <div style="display:flex;gap:.4rem;margin-top:.25rem">
              <select class="form-control" id="reassign-helper" style="flex:1"><option value="">Loading…</option></select>
              <button class="btn btn-primary btn-sm" id="btn-reassign">Reassign</button>
            </div>
          </div>
          <button class="btn btn-outline btn-sm" id="btn-force-complete" style="align-self:flex-end">Force Complete</button>
          <button class="btn btn-danger btn-sm" id="btn-force-cancel" style="align-self:flex-end">Force Cancel + Refund</button>
        ` : '<p class="text-muted" style="font-size:.82rem">No actions available for this status.</p>'}
      </div>
    `;

    document.getElementById('detail-close2').addEventListener('click', closeModal);

    // Render map with location trail
    if (trail.length && typeof L !== 'undefined') {
      setTimeout(() => {
        const mapEl = document.getElementById('detail-map');
        if (!mapEl) return;
        detailMap = L.map(mapEl).setView([trail[0].lat, trail[0].lng], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          attribution: '&copy; OpenStreetMap', maxZoom: 19,
        }).addTo(detailMap);

        const points = trail.map(t => [t.lat, t.lng]);
        L.polyline(points, { color: '#4f46e5', weight: 3 }).addTo(detailMap);
        detailMap.fitBounds(L.latLngBounds(points).pad(0.1));
      }, 100);
    }

    // Load online helpers for reassign dropdown
    if (b.status !== 'COMPLETED' && b.status !== 'CANCELLED') {
      loadOnlineHelpers(b.serviceType);
      setupAdminActions(bookingId, closeModal);
    }

  } catch {
    const card = slot.querySelector('.modal-card');
    if (card) card.innerHTML = '<p class="text-muted">Could not load booking detail.</p>';
  }
}

async function loadOnlineHelpers(serviceType) {
  const select = document.getElementById('reassign-helper');
  if (!select) return;

  try {
    const resp = await api.get('/admin/helpers?active=true&size=100');
    const helpers = (resp?.content || []).filter(h => h.active);

    select.innerHTML = '<option value="">Select helper…</option>';
    helpers.forEach(h => {
      select.innerHTML += `<option value="${h.id}">${esc(h.name)} (${esc(h.email || '')})</option>`;
    });
  } catch {
    select.innerHTML = '<option value="">No helpers available</option>';
  }
}

function setupAdminActions(bookingId, closeModal) {
  const reassignBtn = document.getElementById('btn-reassign');
  if (reassignBtn) {
    reassignBtn.addEventListener('click', async () => {
      const helperId = document.getElementById('reassign-helper')?.value;
      if (!helperId) { toast('Please select a helper', 'warn'); return; }
      reassignBtn.disabled = true;
      try {
        await api.post('/admin/bookings/' + bookingId + '/reassign', { helperId });
        toast('Booking reassigned!', 'success');
        closeModal();
        loadBookings();
      } catch {
        reassignBtn.disabled = false;
      }
    });
  }

  const forceCompleteBtn = document.getElementById('btn-force-complete');
  if (forceCompleteBtn) {
    forceCompleteBtn.addEventListener('click', async () => {
      if (!confirm('Force-complete this booking?')) return;
      forceCompleteBtn.disabled = true;
      try {
        await api.post('/admin/bookings/' + bookingId + '/force-complete');
        toast('Booking force-completed', 'success');
        closeModal();
        loadBookings();
      } catch {
        forceCompleteBtn.disabled = false;
      }
    });
  }

  const forceCancelBtn = document.getElementById('btn-force-cancel');
  if (forceCancelBtn) {
    forceCancelBtn.addEventListener('click', async () => {
      if (!confirm('Force-cancel this booking with full refund?')) return;
      forceCancelBtn.disabled = true;
      try {
        await api.post('/admin/bookings/' + bookingId + '/force-cancel', { reason: 'Admin force cancel' });
        toast('Booking cancelled with refund', 'success');
        closeModal();
        loadBookings();
      } catch {
        forceCancelBtn.disabled = false;
      }
    });
  }
}
