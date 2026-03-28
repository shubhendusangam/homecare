/**
 * customer/history.js — Paginated booking history with detail modal,
 * review form, and "book again" action.
 */
import { api }    from '../../api.js';
import { router } from '../../router.js';
import { toast }  from '../../toast.js';

const SERVICE_ICON = { CLEANING: '🧹', COOKING: '🍳', BABYSITTING: '👶', ELDERLY_HELP: '🧓' };
const STATUS_BADGE = {
  PENDING_ASSIGNMENT: 'badge-pending',
  ASSIGNED:    'badge-assigned',
  HELPER_EN_ROUTE: 'badge-assigned',
  IN_PROGRESS: 'badge-progress',
  COMPLETED:   'badge-completed',
  CANCELLED:   'badge-cancelled',
  EXPIRED:     'badge-cancelled',
};

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
function fmtDate(iso) { return iso ? new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' }) : '—'; }
function fmtCur(n) { return '₹' + Number(n || 0).toFixed(2); }

let currentPage = 0;
const PAGE_SIZE = 10;

/* ── Main render ──────────────────────────────────────────────────── */
export async function render(container) {
  currentPage = 0;
  container.innerHTML = `
    <div class="section-header"><h2>My Bookings</h2></div>
    <nav class="sub-nav">
      <a href="#/customer/home"    class="sub-nav-btn">Home</a>
      <a href="#/customer/history" class="sub-nav-btn active">My Bookings</a>
      <a href="#/customer/wallet"  class="sub-nav-btn">Wallet</a>
    </nav>
    <div id="history-list">
      <div class="skeleton skeleton-card mb-2"></div>
      <div class="skeleton skeleton-card mb-2"></div>
      <div class="skeleton skeleton-card"></div>
    </div>
    <div id="history-pagination"></div>
    <!-- modal slot -->
    <div id="modal-slot"></div>
  `;

  await loadPage();
}

async function loadPage() {
  const listEl = document.getElementById('history-list');
  const pagEl  = document.getElementById('history-pagination');
  try {
    const resp = await api.get(`/bookings?page=${currentPage}&size=${PAGE_SIZE}`);
    const bookings = resp.content || [];
    const totalPages = resp.totalPages || 1;

    if (!bookings.length && currentPage === 0) {
      listEl.innerHTML = `<div class="empty-state"><span class="empty-icon">📋</span><p>No bookings yet. Book your first service!</p></div>`;
      pagEl.innerHTML = '';
      return;
    }

    listEl.innerHTML = `<ul class="data-list">${bookings.map(b => {
      const icon = SERVICE_ICON[b.serviceType] || '📦';
      return `
        <li class="data-list-item" style="cursor:pointer" data-id="${b.id}">
          <div style="display:flex;align-items:center;gap:.6rem">
            <span style="font-size:1.3rem">${icon}</span>
            <div>
              <strong>${esc(b.serviceType)}</strong>
              <br><small class="text-muted">${fmtDate(b.scheduledAt || b.createdAt)}</small>
              ${b.helperName ? `<br><small class="text-muted">${esc(b.helperName)}</small>` : ''}
            </div>
          </div>
          <div style="text-align:right">
            <span class="badge ${STATUS_BADGE[b.status] || ''}">${esc(b.status)}</span>
            <br><small>${fmtCur(b.totalPrice)}</small>
          </div>
        </li>`;
    }).join('')}</ul>`;

    // Click → detail modal
    listEl.querySelectorAll('.data-list-item').forEach(item => {
      item.addEventListener('click', () => openDetail(item.dataset.id));
    });

    // Pagination
    renderPagination(pagEl, totalPages);
  } catch {
    listEl.innerHTML = '<p class="text-muted text-center mt-2">Failed to load bookings.</p>';
    pagEl.innerHTML = '';
  }
}

function renderPagination(el, totalPages) {
  if (totalPages <= 1) { el.innerHTML = ''; return; }
  let html = '<div class="pagination">';
  html += `<button class="page-btn" ${currentPage === 0 ? 'disabled' : ''} data-p="${currentPage - 1}">‹</button>`;
  for (let i = 0; i < totalPages; i++) {
    html += `<button class="page-btn ${i === currentPage ? 'active' : ''}" data-p="${i}">${i + 1}</button>`;
  }
  html += `<button class="page-btn" ${currentPage >= totalPages - 1 ? 'disabled' : ''} data-p="${currentPage + 1}">›</button>`;
  html += '</div>';
  el.innerHTML = html;
  el.querySelectorAll('.page-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const p = parseInt(btn.dataset.p, 10);
      if (isNaN(p) || p < 0 || p >= totalPages) return;
      currentPage = p;
      loadPage();
    });
  });
}

/* ── Detail Modal ─────────────────────────────────────────────────── */
async function openDetail(bookingId) {
  let booking;
  try {
    booking = await api.get('/bookings/' + bookingId);
  } catch { return; }

  const icon = SERVICE_ICON[booking.serviceType] || '📦';
  const canReview = booking.status === 'COMPLETED' && !booking.rating;

  const slot = document.getElementById('modal-slot');
  slot.innerHTML = `
    <div class="modal-overlay" id="detail-overlay">
      <div class="modal-card">
        <div class="modal-header">
          <h3>${icon} ${esc(booking.serviceType)}</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>

        <span class="badge ${STATUS_BADGE[booking.status] || ''}" style="margin-bottom:.75rem;display:inline-block">${esc(booking.status)}</span>

        <!-- Status timeline -->
        <div class="timeline">
          ${timelineItem('Booked', booking.createdAt, true)}
          ${booking.acceptedAt ? timelineItem('Assigned', booking.acceptedAt, true) : ''}
          ${booking.startedAt  ? timelineItem('Started', booking.startedAt, true) : ''}
          ${booking.completedAt ? timelineItem('Completed', booking.completedAt, true) : ''}
          ${booking.status === 'CANCELLED' ? timelineItem('Cancelled', booking.updatedAt, true) : ''}
        </div>

        <table class="summary-table" style="margin-bottom:.75rem">
          <tr><td>Address</td><td id="det-addr"></td></tr>
          <tr><td>Duration</td><td>${booking.durationHours}h</td></tr>
          <tr><td>Helper</td><td id="det-helper"></td></tr>
          <tr class="total-row"><td>Total</td><td>${fmtCur(booking.totalPrice)}</td></tr>
        </table>

        ${booking.specialInstructions ? `<p style="font-size:.85rem;margin-bottom:.75rem"><strong>Notes:</strong> <span id="det-notes"></span></p>` : ''}

        ${canReview ? `
        <div id="review-section">
          <h4 style="font-size:.9rem;margin-bottom:.5rem">Write a Review</h4>
          <div class="star-rating" id="star-rating">
            ${[1,2,3,4,5].map(i => `<span class="star" data-v="${i}">★</span>`).join('')}
          </div>
          <textarea class="form-control mt-1" id="review-comment" rows="2" placeholder="Your feedback…" maxlength="500"></textarea>
          <button class="btn btn-primary btn-sm mt-1" id="btn-submit-review">Submit Review</button>
        </div>` : ''}

        ${booking.rating ? `
        <div style="margin-bottom:.75rem">
          <strong>Your review:</strong>
          <div class="star-rating-display">${[1,2,3,4,5].map(i => `<span class="star ${i <= booking.rating ? 'filled' : ''}">★</span>`).join('')}</div>
          ${booking.reviewText ? `<p style="font-size:.85rem;margin-top:.25rem" id="det-review-text"></p>` : ''}
        </div>` : ''}

        <div style="display:flex;gap:.5rem;margin-top:1rem">
          <button class="btn btn-primary btn-sm" id="btn-book-again">Book Again</button>
          <button class="btn btn-outline btn-sm" id="btn-modal-close2">Close</button>
        </div>
      </div>
    </div>
  `;

  // XSS-safe text assignments
  document.getElementById('det-addr').textContent = booking.addressLine || '—';
  document.getElementById('det-helper').textContent = booking.helperName || '—';
  if (booking.specialInstructions) {
    const notesEl = document.getElementById('det-notes');
    if (notesEl) notesEl.textContent = booking.specialInstructions;
  }
  if (booking.reviewText) {
    const revEl = document.getElementById('det-review-text');
    if (revEl) revEl.textContent = booking.reviewText;
  }

  // Close
  const closeModal = () => { slot.innerHTML = ''; };
  document.getElementById('modal-close-btn').addEventListener('click', closeModal);
  document.getElementById('btn-modal-close2').addEventListener('click', closeModal);
  document.getElementById('detail-overlay').addEventListener('click', e => {
    if (e.target.id === 'detail-overlay') closeModal();
  });

  // Book again
  document.getElementById('btn-book-again').addEventListener('click', () => {
    closeModal();
    router.navigate('/customer/book?service=' + booking.serviceType + '&duration=' + booking.durationHours);
  });

  // Star rating interaction
  if (canReview) {
    let selectedRating = 0;
    const stars = document.querySelectorAll('#star-rating .star');
    stars.forEach(star => {
      star.addEventListener('mouseenter', () => {
        const v = parseInt(star.dataset.v, 10);
        stars.forEach(s => s.classList.toggle('hover', parseInt(s.dataset.v, 10) <= v));
      });
      star.addEventListener('mouseleave', () => {
        stars.forEach(s => {
          s.classList.remove('hover');
          s.classList.toggle('filled', parseInt(s.dataset.v, 10) <= selectedRating);
        });
      });
      star.addEventListener('click', () => {
        selectedRating = parseInt(star.dataset.v, 10);
        stars.forEach(s => s.classList.toggle('filled', parseInt(s.dataset.v, 10) <= selectedRating));
      });
    });

    const submitBtn = document.getElementById('btn-submit-review');
    submitBtn.addEventListener('click', async () => {
      if (!selectedRating) { toast('Please select a rating', 'warn'); return; }
      submitBtn.disabled = true;
      submitBtn.textContent = 'Submitting…';
      try {
        await api.post('/reviews', {
          bookingId: booking.id,
          rating: selectedRating,
          comment: document.getElementById('review-comment').value.trim() || null,
        });
        toast('Review submitted!', 'success');
        // Replace review section with confirmation
        const sec = document.getElementById('review-section');
        if (sec) sec.innerHTML = `<p style="color:var(--toast-success);font-weight:600">✅ Review submitted — thank you!</p>`;
      } catch {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Submit Review';
      }
    });
  }
}

function timelineItem(label, isoDate, active) {
  return `<div class="timeline-item ${active ? 'active' : ''}">
    <div class="tl-label">${esc(label)}</div>
    <div class="tl-time">${fmtDate(isoDate)}</div>
  </div>`;
}
