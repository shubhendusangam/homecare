/**
 * helper/home.js — Helper dashboard home screen.
 *
 * - Online/offline toggle (large, prominent): calls PATCH /api/v1/helpers/me/status
 * - Today's stats: jobs done, earnings today
 * - Rating display: ⭐ 4.8 (142 reviews)
 * - Current active booking card (if ON_JOB)
 * - Location polling implementation when ONLINE
 */
import { auth }   from '../../auth.js';
import { api }    from '../../api.js';
import { toast }  from '../../toast.js';
import { router } from '../../router.js';

let locationInterval = null;
let stompClient = null;

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

function getCurrentPosition() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) { reject(new Error('Geolocation not supported')); return; }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 5000,
    });
  });
}

function getStompClient(token) {
  if (stompClient && stompClient.connected) return stompClient;
  if (typeof SockJS === 'undefined' || typeof StompJs === 'undefined') return null;
  const socket = new SockJS('/ws');
  stompClient = new StompJs.Client({
    webSocketFactory: () => socket,
    connectHeaders: { 'Authorization': 'Bearer ' + token },
    reconnectDelay: 5000,
    debug: () => {},
    onStompError: () => {},
  });
  stompClient.activate();
  return stompClient;
}

function startLocationPolling(bookingId) {
  stopLocationPolling();
  const token = auth.token();
  const client = getStompClient(token);

  locationInterval = setInterval(async () => {
    try {
      const pos = await getCurrentPosition();
      if (client && client.connected) {
        client.publish({
          destination: '/app/location/update',
          body: JSON.stringify({
            bookingId: bookingId || null,
            lat: pos.coords.latitude,
            lng: pos.coords.longitude,
            accuracy: pos.coords.accuracy,
            heading: pos.coords.heading,
            speedKmh: (pos.coords.speed || 0) * 3.6,
          }),
        });
      } else {
        // Fallback to REST
        await api.patch('/helpers/me/location', {
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
        });
      }
    } catch {
      // Silently ignore location errors during polling
    }
  }, 10_000);
}

function stopLocationPolling() {
  if (locationInterval) {
    clearInterval(locationInterval);
    locationInterval = null;
  }
}

function cleanup() {
  stopLocationPolling();
  if (stompClient) { try { stompClient.deactivate(); } catch {} stompClient = null; }
}

export async function render(container) {
  router.onTeardown(cleanup);

  container.innerHTML = `
    <div class="section-header">
      <h2>Helper Dashboard</h2>
    </div>

    <nav class="sub-nav">
      <a href="#/helper/home"     class="sub-nav-btn active">Dashboard</a>
      <a href="#/helper/bookings" class="sub-nav-btn">Bookings</a>
      <a href="#/helper/earnings" class="sub-nav-btn">Earnings</a>
      <a href="#/helper/profile"  class="sub-nav-btn">Profile</a>
    </nav>

    <!-- Online/Offline Toggle -->
    <div class="card mb-2" id="status-toggle-card">
      <div class="toggle-status-row">
        <div>
          <div class="toggle-status-label" id="status-label">Loading…</div>
          <div class="toggle-status-sub text-muted" id="status-sub"></div>
        </div>
        <button class="toggle-switch" id="status-toggle" disabled>
          <span class="toggle-knob"></span>
        </button>
      </div>
    </div>

    <!-- Stats -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-value" id="stat-jobs">—</div>
        <div class="stat-label">Jobs Done</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" id="stat-earnings">—</div>
        <div class="stat-label">Earnings Today</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" id="stat-rating">—</div>
        <div class="stat-label">Rating</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" id="stat-reviews">—</div>
        <div class="stat-label">Total Jobs</div>
      </div>
    </div>

    <!-- Active Booking Card -->
    <div id="active-job-slot"></div>
  `;

  loadProfile();
  loadActiveBooking();
}

async function loadProfile() {
  const toggleBtn = document.getElementById('status-toggle');

  try {
    const profile = await api.get('/helpers/me');
    if (!profile) return;

    const isOnline = profile.status === 'ONLINE' || profile.status === 'ON_JOB';

    // Update stats
    const jobsEl = document.getElementById('stat-jobs');
    const ratingEl = document.getElementById('stat-rating');

    if (jobsEl) jobsEl.textContent = profile.totalJobsCompleted || 0;
    if (ratingEl) ratingEl.textContent = '⭐ ' + (profile.rating ? profile.rating.toFixed(1) : '0.0');

    // Load earnings
    loadEarningsData();

    // Toggle state
    updateToggleUI(isOnline, profile.status);

    if (toggleBtn) {
      toggleBtn.disabled = false;
      toggleBtn.addEventListener('click', async () => {
        const currentlyOnline = toggleBtn.classList.contains('toggle-on');
        const newStatus = currentlyOnline ? 'OFFLINE' : 'ONLINE';

        try {
          if (newStatus === 'ONLINE') {
            try {
              await getCurrentPosition();
            } catch {
              toast('Please allow location access to go online', 'warn');
              return;
            }
          }

          toggleBtn.disabled = true;
          const updated = await api.patch('/helpers/me/status', { status: newStatus });
          const nowOnline = updated.status === 'ONLINE' || updated.status === 'ON_JOB';
          updateToggleUI(nowOnline, updated.status);

          if (nowOnline) {
            startLocationPolling(null);
            toast('You are now online', 'success');
          } else {
            stopLocationPolling();
            toast('You are now offline', 'info');
          }
        } catch {
          // Error already shown by api module
        } finally {
          if (toggleBtn) toggleBtn.disabled = false;
        }
      });
    }

    // If currently online, start polling
    if (isOnline) {
      try {
        await getCurrentPosition();
        startLocationPolling(null);
      } catch {
        // No location access
      }
    }
  } catch {
    const label = document.getElementById('status-label');
    if (label) label.textContent = 'Could not load profile';
  }
}

function updateToggleUI(isOnline, status) {
  const toggleBtn = document.getElementById('status-toggle');
  const label = document.getElementById('status-label');
  const sub = document.getElementById('status-sub');
  const card = document.getElementById('status-toggle-card');

  if (toggleBtn) toggleBtn.classList.toggle('toggle-on', isOnline);

  if (label) {
    if (status === 'ON_JOB') {
      label.textContent = '🔧 On a Job';
    } else if (isOnline) {
      label.textContent = '🟢 You are Online';
    } else {
      label.textContent = '🔴 You are Offline';
    }
  }

  if (sub) {
    sub.textContent = isOnline
      ? 'You will receive new booking requests'
      : 'Toggle on to start receiving bookings';
  }

  if (card) card.classList.toggle('card-online', isOnline);
}

async function loadEarningsData() {
  try {
    const data = await api.get('/helpers/earnings');
    const earningsEl = document.getElementById('stat-earnings');
    const reviewsEl = document.getElementById('stat-reviews');
    if (earningsEl && data) earningsEl.textContent = '₹' + (data.totalEarnings || 0);
    if (reviewsEl && data) reviewsEl.textContent = data.totalCompletedJobs || 0;
  } catch {
    const earningsEl = document.getElementById('stat-earnings');
    if (earningsEl) earningsEl.textContent = '₹0';
  }
}

async function loadActiveBooking() {
  const slot = document.getElementById('active-job-slot');
  if (!slot) return;

  try {
    const resp = await api.get('/helpers/bookings?page=0&size=5');
    const bookings = resp?.content || [];
    const activeStatuses = ['ASSIGNED', 'HELPER_EN_ROUTE', 'IN_PROGRESS'];
    const active = bookings.filter(b => activeStatuses.includes(b.status));

    if (!active.length) { slot.innerHTML = ''; return; }

    const b = active[0];
    const SERVICE_ICON = { CLEANING: '🧹', COOKING: '🍳', BABYSITTING: '👶', ELDERLY_HELP: '🧓' };
    const icon = SERVICE_ICON[b.serviceType] || '📦';
    const statusText = (b.status || '').replace(/_/g, ' ');

    let actionBtn = '';
    if (b.status === 'ASSIGNED') {
      actionBtn = `<button class="btn btn-primary btn-sm" data-action="start-travel" data-id="${b.id}">Start Journey 🚗</button>`;
    } else if (b.status === 'HELPER_EN_ROUTE') {
      actionBtn = `<button class="btn btn-primary btn-sm" data-action="start-job" data-id="${b.id}">Arrived / Start Job ▶</button>`;
    } else if (b.status === 'IN_PROGRESS') {
      actionBtn = `<button class="btn btn-primary btn-sm" data-action="view-active" data-id="${b.id}">View Active Job →</button>`;
    }

    slot.innerHTML = `
      <div class="card mt-2">
        <h3 style="font-size:.95rem;margin-bottom:.5rem">Current Active Job</h3>
        <div class="active-banner" id="active-job-banner">
          <div class="ab-left">
            <span class="ab-icon">${icon}</span>
            <div>
              <div class="ab-title">${esc(b.serviceType)} — ${esc(statusText)}</div>
              <div class="ab-sub">${esc(b.customerName || 'Customer')}</div>
              <div class="ab-sub">${esc(b.addressLine || '')}</div>
            </div>
          </div>
          <span class="ab-arrow">›</span>
        </div>
        <div class="mt-1">${actionBtn}</div>
      </div>
    `;

    slot.querySelectorAll('[data-action]').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        e.stopPropagation();
        const action = btn.dataset.action;
        const id = btn.dataset.id;
        if (action === 'start-travel') {
          try {
            await api.patch('/helpers/bookings/' + id + '/start-travel');
            toast('Journey started!', 'success');
            router.navigate('/helper/active/' + id);
          } catch { /* handled by api */ }
        } else if (action === 'start-job') {
          try {
            await api.patch('/helpers/bookings/' + id + '/start-job');
            toast('Job started!', 'success');
            router.navigate('/helper/active/' + id);
          } catch { /* handled by api */ }
        } else if (action === 'view-active') {
          router.navigate('/helper/active/' + id);
        }
      });
    });

    const banner = document.getElementById('active-job-banner');
    if (banner) {
      banner.addEventListener('click', () => router.navigate('/helper/active/' + b.id));
    }

    if (b.status === 'HELPER_EN_ROUTE' || b.status === 'IN_PROGRESS') {
      try {
        await getCurrentPosition();
        startLocationPolling(b.id);
      } catch { /* no location */ }
    }
  } catch {
    slot.innerHTML = '';
  }
}
