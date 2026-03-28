/**
 * helper/active.js — Active job screen (full screen during IN_PROGRESS).
 *
 * - Customer address with "Open in Maps" link
 * - Job timer (started when status = IN_PROGRESS)
 * - Service checklist (from specialInstructions)
 * - [Arrived / Start Job] → IN_PROGRESS
 * - [Mark Complete] → COMPLETED (with confirmation modal)
 * - Emergency contact button
 */
import { api }    from '../../api.js';
import { toast }  from '../../toast.js';
import { router } from '../../router.js';

let jobTimer = null;
let jobSeconds = 0;

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

function cleanup() {
  if (jobTimer) { clearInterval(jobTimer); jobTimer = null; }
  jobSeconds = 0;
}

function getMapsUrl(lat, lng, address) {
  // Try geo: URI first (works on mobile), fallback to Google Maps
  const q = address || (lat + ',' + lng);
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`;
}

function formatTimer(seconds) {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function startJobTimer() {
  if (jobTimer) clearInterval(jobTimer);
  jobTimer = setInterval(() => {
    jobSeconds++;
    const el = document.getElementById('job-timer-display');
    if (el) el.textContent = formatTimer(jobSeconds);
  }, 1000);
}

export async function render(container, params) {
  cleanup();
  const bookingId = params.id;
  router.onTeardown(cleanup);

  container.innerHTML = `
    <div class="text-center mt-2"><span class="spinner"></span><p class="text-muted mt-1">Loading job…</p></div>
  `;

  let booking;
  try {
    booking = await api.get('/bookings/' + bookingId);
  } catch {
    container.innerHTML = `
      <div class="empty-state">
        <span class="empty-icon">⚠️</span>
        <p>Could not load booking.</p>
        <a href="#/helper/bookings" class="btn btn-outline btn-sm mt-1">← Back to Bookings</a>
      </div>
    `;
    return;
  }

  const mapsUrl = getMapsUrl(booking.latitude, booking.longitude, booking.addressLine);
  const statusText = (booking.status || '').replace(/_/g, ' ');

  // Parse specialInstructions into checklist items
  const instructions = (booking.specialInstructions || '')
    .split(/[\n;•]/)
    .map(s => s.trim())
    .filter(Boolean);

  // Calculate elapsed time if IN_PROGRESS
  if (booking.status === 'IN_PROGRESS' && booking.startedAt) {
    jobSeconds = Math.floor((Date.now() - new Date(booking.startedAt).getTime()) / 1000);
    if (jobSeconds < 0) jobSeconds = 0;
  }

  const isInProgress = booking.status === 'IN_PROGRESS';
  const isEnRoute = booking.status === 'HELPER_EN_ROUTE';
  const isAssigned = booking.status === 'ASSIGNED';
  const isCompleted = booking.status === 'COMPLETED';

  container.innerHTML = `
    <div class="section-header">
      <h2>Active Job</h2>
      <span class="badge ${getBadgeClass(booking.status)}">${esc(statusText)}</span>
    </div>

    <!-- Job Timer -->
    ${isInProgress ? `
    <div class="card mb-2 text-center" style="background:var(--primary-light)">
      <div class="text-muted" style="font-size:.8rem">Job Duration</div>
      <div id="job-timer-display" style="font-size:2rem;font-weight:700;color:var(--primary)">${formatTimer(jobSeconds)}</div>
    </div>
    ` : ''}

    <!-- Customer Address -->
    <div class="card mb-2">
      <h3 style="font-size:.9rem;margin-bottom:.5rem">📍 Customer Address</h3>
      <p style="font-size:.9rem">${esc(booking.addressLine || 'Address not provided')}</p>
      <a href="${esc(mapsUrl)}" target="_blank" rel="noopener" class="btn btn-outline btn-sm mt-1">
        🗺️ Open in Maps
      </a>
    </div>

    <!-- Customer Info -->
    <div class="card mb-2">
      <h3 style="font-size:.9rem;margin-bottom:.5rem">👤 Customer</h3>
      <p style="font-size:.9rem">${esc(booking.customerName || 'Customer')}</p>
      <p class="text-muted" style="font-size:.8rem">Service: ${esc(booking.serviceType)} · ${booking.durationHours || 0}h</p>
      <p class="text-muted" style="font-size:.8rem">Amount: ₹${booking.totalPrice || 0}</p>
    </div>

    <!-- Service Checklist -->
    ${instructions.length ? `
    <div class="card mb-2">
      <h3 style="font-size:.9rem;margin-bottom:.5rem">📋 Service Checklist</h3>
      <ul class="checklist" id="service-checklist">
        ${instructions.map((item, i) => `
          <li class="checklist-item" data-idx="${i}">
            <input type="checkbox" id="check-${i}">
            <label for="check-${i}">${esc(item)}</label>
          </li>
        `).join('')}
      </ul>
    </div>
    ` : ''}

    <!-- Action Buttons -->
    <div class="card mb-2" id="action-buttons">
      ${isAssigned || isEnRoute ? `
        <button class="btn btn-primary btn-block" id="btn-start-job">
          ${isEnRoute ? '📍 Arrived / Start Job' : '🚗 Start Journey'}
        </button>
      ` : ''}
      ${isInProgress ? `
        <button class="btn btn-primary btn-block" id="btn-complete">
          ✅ Mark Complete
        </button>
      ` : ''}
      ${isCompleted ? `
        <div class="text-center text-muted">
          <span style="font-size:2rem">🎉</span>
          <p>Job completed!</p>
        </div>
      ` : ''}
    </div>

    <!-- Emergency Contact -->
    <div class="text-center mt-2">
      <button class="btn btn-outline btn-sm" id="btn-emergency">🆘 Emergency Contact</button>
      <a href="#/helper/bookings" class="btn btn-outline btn-sm" style="margin-left:.5rem">← Back</a>
    </div>

    <!-- Confirmation Modal -->
    <div id="complete-modal-slot"></div>
  `;

  // Start timer if in progress
  if (isInProgress) {
    startJobTimer();
  }

  // Start Job button (handles both ASSIGNED → start-travel and EN_ROUTE → start-job)
  const startBtn = document.getElementById('btn-start-job');
  if (startBtn) {
    startBtn.addEventListener('click', async () => {
      startBtn.disabled = true;
      try {
        if (isEnRoute) {
          await api.patch('/helpers/bookings/' + bookingId + '/start-job');
          toast('Job started!', 'success');
        } else {
          await api.patch('/helpers/bookings/' + bookingId + '/start-travel');
          toast('Journey started!', 'success');
        }
        render(container, params); // re-render to update state
      } catch {
        startBtn.disabled = false;
      }
    });
  }

  // Mark Complete button — shows confirmation modal
  const completeBtn = document.getElementById('btn-complete');
  if (completeBtn) {
    completeBtn.addEventListener('click', () => {
      showCompleteModal(bookingId, container, params);
    });
  }

  // Emergency contact
  const emergencyBtn = document.getElementById('btn-emergency');
  if (emergencyBtn) {
    emergencyBtn.addEventListener('click', () => {
      toast('Admin support: +91-9876543210', 'info', 5000);
    });
  }
}

function showCompleteModal(bookingId, container, params) {
  const slot = document.getElementById('complete-modal-slot');
  if (!slot) return;

  slot.innerHTML = `
    <div class="modal-overlay" id="complete-modal">
      <div class="modal-card">
        <div class="modal-header">
          <h3>Complete Job?</h3>
          <button class="modal-close" id="modal-close">&times;</button>
        </div>
        <p style="margin-bottom:1rem">Are you sure you want to mark this job as completed? This action cannot be undone.</p>
        <div style="display:flex;gap:.5rem">
          <button class="btn btn-primary" style="flex:1" id="modal-confirm">✅ Yes, Complete</button>
          <button class="btn btn-outline" style="flex:1" id="modal-cancel">Cancel</button>
        </div>
      </div>
    </div>
  `;

  document.getElementById('modal-close').addEventListener('click', () => { slot.innerHTML = ''; });
  document.getElementById('modal-cancel').addEventListener('click', () => { slot.innerHTML = ''; });
  document.getElementById('complete-modal').addEventListener('click', (e) => {
    if (e.target.id === 'complete-modal') slot.innerHTML = '';
  });

  document.getElementById('modal-confirm').addEventListener('click', async () => {
    const btn = document.getElementById('modal-confirm');
    btn.disabled = true;
    btn.textContent = 'Completing…';
    try {
      await api.patch('/helpers/bookings/' + bookingId + '/complete');
      toast('Job completed! 🎉', 'success');
      slot.innerHTML = '';
      render(container, params); // re-render
    } catch {
      btn.disabled = false;
      btn.textContent = '✅ Yes, Complete';
    }
  });
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
