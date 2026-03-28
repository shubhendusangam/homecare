/**
 * customer/book.js — 3-step booking flow.
 *
 * Step 1: Service selection (pre-filled from query param)
 * Step 2: Address + time (map pin, duration, now/schedule)
 * Step 3: Summary + payment → POST /api/v1/bookings → success / tracking
 */
import { api }    from '../../api.js';
import { auth }   from '../../auth.js';
import { router } from '../../router.js';
import { toast }  from '../../toast.js';

const SERVICES = [
  { type: 'CLEANING',     icon: '🧹', name: 'Cleaning',     base: 299, perHour: 199 },
  { type: 'COOKING',      icon: '🍳', name: 'Cooking',      base: 399, perHour: 249 },
  { type: 'BABYSITTING',  icon: '👶', name: 'Babysitting',  base: 499, perHour: 299 },
  { type: 'ELDERLY_HELP', icon: '🧓', name: 'Elderly Help', base: 449, perHour: 279 },
];

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

/* ── Booking state ────────────────────────────────────────────────── */
let state = {};
let bookMap = null;
let bookMarker = null;
let wsClient = null;   // WebSocket client for post-booking status updates
let wsTimeout = null;  // auto-disconnect timeout

function resetState(params) {
  cleanupBooking();
  state = {
    step: 1,
    serviceType: params.service || 'CLEANING',
    durationHours: parseInt(params.duration, 10) || 2,
    bookingType: 'IMMEDIATE',
    scheduledAt: null,
    addressLine: '',
    latitude: 28.6139,
    longitude: 77.209,
    specialInstructions: '',
  };
  bookMap = null;
  bookMarker = null;
}

function cleanupBooking() {
  if (wsClient) { try { wsClient.deactivate(); } catch {} wsClient = null; }
  if (wsTimeout) { clearTimeout(wsTimeout); wsTimeout = null; }
  if (bookMap) { try { bookMap.remove(); } catch {} bookMap = null; }
  bookMarker = null;
}

/* ── Main render ──────────────────────────────────────────────────── */
export function render(container, params) {
  resetState(params);

  // Register cleanup for when user navigates away
  router.onTeardown(cleanupBooking);

  container.innerHTML = `
    <div class="section-header"><h2>Book a Service</h2></div>

    <!-- Step indicator -->
    <div class="step-bar">
      <span class="step-dot active" id="sd1">1</span>
      <span class="step-line" id="sl1"></span>
      <span class="step-dot" id="sd2">2</span>
      <span class="step-line" id="sl2"></span>
      <span class="step-dot" id="sd3">3</span>
    </div>

    <div class="step-body" id="step-body"></div>

    <!-- Navigation buttons -->
    <div style="display:flex;gap:.5rem;margin-top:1rem" id="step-nav">
      <button class="btn btn-outline" id="btn-back" style="display:none">← Back</button>
      <button class="btn btn-primary" id="btn-next" style="flex:1">Next →</button>
    </div>

    <!-- Success screen (hidden initially) -->
    <div id="booking-success" class="hidden"></div>
  `;

  renderStep();

  document.getElementById('btn-next').addEventListener('click', handleNext);
  document.getElementById('btn-back').addEventListener('click', handleBack);
}

function handleNext() {
  if (state.step === 1) {
    state.step = 2;
    renderStep();
  } else if (state.step === 2) {
    if (!validateStep2()) return;
    state.step = 3;
    renderStep();
  } else if (state.step === 3) {
    submitBooking();
  }
}

function handleBack() {
  if (state.step > 1) {
    state.step--;
    renderStep();
  }
}

function renderStep() {
  // Update step dots
  for (let i = 1; i <= 3; i++) {
    const dot  = document.getElementById('sd' + i);
    dot.className = 'step-dot' + (i === state.step ? ' active' : (i < state.step ? ' done' : ''));
  }
  document.getElementById('sl1').className = 'step-line' + (state.step > 1 ? ' done' : '');
  document.getElementById('sl2').className = 'step-line' + (state.step > 2 ? ' done' : '');

  // Back button visibility
  document.getElementById('btn-back').style.display = state.step > 1 ? '' : 'none';

  // Next button text
  const nextBtn = document.getElementById('btn-next');
  if (state.step === 3) {
    nextBtn.textContent = 'Confirm Booking';
    nextBtn.disabled = false;
  } else {
    nextBtn.textContent = 'Next →';
    nextBtn.disabled = false;
  }

  const body = document.getElementById('step-body');

  switch (state.step) {
    case 1: renderStep1(body); break;
    case 2: renderStep2(body); break;
    case 3: renderStep3(body); break;
  }
}

/* ── Step 1: Service selection ────────────────────────────────────── */
function renderStep1(body) {
  body.innerHTML = `
    <h3 style="font-size:.95rem;margin-bottom:.75rem">Choose a service</h3>
    <div class="services-grid" id="svc-grid">
      ${SERVICES.map(s => `
        <div class="service-card ${s.type === state.serviceType ? 'selected' : ''}"
             data-type="${s.type}" style="${s.type === state.serviceType ? 'border-color:var(--primary);box-shadow:var(--shadow-md)' : ''}">
          <span class="service-icon">${s.icon}</span>
          <h3>${s.name}</h3>
          <p>From ₹${s.base}</p>
        </div>
      `).join('')}
    </div>
  `;

  body.querySelectorAll('.service-card').forEach(card => {
    card.addEventListener('click', () => {
      state.serviceType = card.dataset.type;
      body.querySelectorAll('.service-card').forEach(c => {
        c.style.borderColor = '';
        c.style.boxShadow = '';
      });
      card.style.borderColor = 'var(--primary)';
      card.style.boxShadow = 'var(--shadow-md)';
    });
  });
}

/* ── Step 2: Address + time ───────────────────────────────────────── */
function renderStep2(body) {
  // Min datetime: 30 min from now
  const minDt = new Date(Date.now() + 30 * 60000);
  const maxDt = new Date(Date.now() + 30 * 24 * 3600000);
  // datetime-local inputs require local time, not UTC
  const toLocal = d => {
    const off = d.getTimezoneOffset();
    const local = new Date(d.getTime() - off * 60000);
    return local.toISOString().slice(0, 16);
  };

  body.innerHTML = `
    <div class="form-group">
      <label>Service address</label>
      <input type="text" class="form-control" id="bk-address" placeholder="Enter your address"
             value="${esc(state.addressLine)}" />
    </div>
    <div class="form-group">
      <label>Pin location on map</label>
      <div id="book-map" class="map-container" style="height:35vh;margin-bottom:.75rem"></div>
    </div>
    <button class="btn btn-outline btn-sm mb-2" id="btn-saved-addr" type="button">📍 Use my saved address</button>

    <div class="form-group">
      <label>Duration</label>
      <div class="duration-row" id="dur-row">
        ${[1,2,3,4].map(h => `<button class="dur-btn ${state.durationHours === h ? 'active' : ''}" data-h="${h}" type="button">${h}h</button>`).join('')}
      </div>
    </div>

    <div class="form-group">
      <label>When</label>
      <div class="toggle-row" id="type-row">
        <button class="toggle-btn ${state.bookingType === 'IMMEDIATE' ? 'active' : ''}" data-t="IMMEDIATE" type="button">Now</button>
        <button class="toggle-btn ${state.bookingType === 'SCHEDULED' ? 'active' : ''}" data-t="SCHEDULED" type="button">Schedule for later</button>
      </div>
    </div>

    <div class="form-group ${state.bookingType === 'IMMEDIATE' ? 'hidden' : ''}" id="schedule-group">
      <label>Date &amp; time</label>
      <input type="datetime-local" class="form-control" id="bk-schedule"
             min="${toLocal(minDt)}" max="${toLocal(maxDt)}" value="${state.scheduledAt || ''}" />
    </div>

    <div class="form-group">
      <label>Special instructions (optional)</label>
      <textarea class="form-control" id="bk-notes" rows="2" maxlength="1000"
                placeholder="Any details for the helper…">${esc(state.specialInstructions)}</textarea>
    </div>
  `;

  // Map
  setTimeout(() => initBookMap(), 50);

  // Duration buttons
  document.getElementById('dur-row').addEventListener('click', e => {
    const btn = e.target.closest('.dur-btn');
    if (!btn) return;
    state.durationHours = parseInt(btn.dataset.h, 10);
    document.querySelectorAll('#dur-row .dur-btn').forEach(b => b.classList.toggle('active', b === btn));
  });

  // Booking type toggle
  document.getElementById('type-row').addEventListener('click', e => {
    const btn = e.target.closest('.toggle-btn');
    if (!btn) return;
    state.bookingType = btn.dataset.t;
    document.querySelectorAll('#type-row .toggle-btn').forEach(b => b.classList.toggle('active', b.dataset.t === state.bookingType));
    document.getElementById('schedule-group').classList.toggle('hidden', state.bookingType === 'IMMEDIATE');
  });

  // Saved address
  document.getElementById('btn-saved-addr').addEventListener('click', loadSavedAddress);

  // Sync inputs to state on change
  document.getElementById('bk-address').addEventListener('input', e => { state.addressLine = e.target.value; });
  document.getElementById('bk-notes').addEventListener('input', e => { state.specialInstructions = e.target.value; });
  document.getElementById('bk-schedule').addEventListener('change', e => { state.scheduledAt = e.target.value; });
}

function initBookMap() {
  const el = document.getElementById('book-map');
  if (!el || typeof L === 'undefined') return;
  if (bookMap) { try { bookMap.remove(); } catch {} bookMap = null; }

  bookMap = L.map(el).setView([state.latitude, state.longitude], 14);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap', maxZoom: 19
  }).addTo(bookMap);

  bookMarker = L.marker([state.latitude, state.longitude], { draggable: true }).addTo(bookMap);
  bookMarker.bindPopup('Drag me or click map').openPopup();

  bookMarker.on('dragend', () => {
    const pos = bookMarker.getLatLng();
    state.latitude  = pos.lat;
    state.longitude = pos.lng;
    reverseGeocode(pos.lat, pos.lng);
  });

  bookMap.on('click', e => {
    state.latitude  = e.latlng.lat;
    state.longitude = e.latlng.lng;
    bookMarker.setLatLng(e.latlng);
    reverseGeocode(e.latlng.lat, e.latlng.lng);
  });

  // If user grants geolocation and no saved address, centre on them
  if (!state.addressLine && navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(pos => {
      const { latitude, longitude } = pos.coords;
      state.latitude  = latitude;
      state.longitude = longitude;
      bookMap.setView([latitude, longitude], 15);
      bookMarker.setLatLng([latitude, longitude]);
      reverseGeocode(latitude, longitude);
    }, () => {/* denied — keep default */});
  }
}

let geocodeTimer = null;
async function reverseGeocode(lat, lng) {
  // Debounce: wait 500ms before firing to avoid spamming Nominatim on rapid clicks
  if (geocodeTimer) clearTimeout(geocodeTimer);
  geocodeTimer = setTimeout(async () => {
    try {
      const r = await fetch(
        `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`,
        { headers: { 'User-Agent': 'HomeCareApp/1.0' } }
      );
      const j = await r.json();
      if (j.display_name) {
        state.addressLine = j.display_name;
        const el = document.getElementById('bk-address');
        if (el) el.value = j.display_name;
      }
    } catch { /* noop */ }
  }, 500);
}

async function loadSavedAddress() {
  try {
    const profile = await api.get('/customers/me');
    if (profile && profile.addressLine) {
      state.addressLine = profile.addressLine;
      state.latitude    = profile.latitude  || state.latitude;
      state.longitude   = profile.longitude || state.longitude;
      const el = document.getElementById('bk-address');
      if (el) el.value = profile.addressLine;
      if (bookMap && bookMarker) {
        bookMap.setView([state.latitude, state.longitude], 15);
        bookMarker.setLatLng([state.latitude, state.longitude]);
      }
      toast('Address loaded from profile', 'success');
    } else {
      toast('No saved address found', 'info');
    }
  } catch { /* api.js already toasts */ }
}

function validateStep2() {
  if (!state.addressLine.trim()) {
    toast('Please enter or select an address', 'warn');
    return false;
  }
  if (state.bookingType === 'SCHEDULED') {
    if (!state.scheduledAt) {
      toast('Please select a date and time', 'warn');
      return false;
    }
    const dt = new Date(state.scheduledAt);
    if (dt < new Date(Date.now() + 25 * 60000)) {
      toast('Schedule must be at least 30 minutes from now', 'warn');
      return false;
    }
  }
  return true;
}

/* ── Step 3: Summary + payment ────────────────────────────────────── */
function renderStep3(body) {
  const svc = SERVICES.find(s => s.type === state.serviceType) || SERVICES[0];
  const total = svc.base + (state.durationHours * svc.perHour);

  body.innerHTML = `
    <div class="card mb-2">
      <h3 style="font-size:.95rem;margin-bottom:.75rem">Booking Summary</h3>
      <table class="summary-table">
        <tr><td>Service</td><td>${svc.icon} ${svc.name}</td></tr>
        <tr><td>Address</td><td id="sum-addr"></td></tr>
        <tr><td>Duration</td><td>${state.durationHours} hour${state.durationHours > 1 ? 's' : ''}</td></tr>
        <tr><td>When</td><td>${state.bookingType === 'IMMEDIATE' ? 'Now' : esc(new Date(state.scheduledAt).toLocaleString())}</td></tr>
        <tr><td>Base price</td><td>₹${svc.base}</td></tr>
        <tr><td>${state.durationHours}h × ₹${svc.perHour}/h</td><td>₹${state.durationHours * svc.perHour}</td></tr>
        <tr class="total-row"><td>Total</td><td>₹${total}</td></tr>
      </table>
    </div>

    ${state.specialInstructions ? `<div class="card mb-2"><p style="font-size:.85rem"><strong>Notes:</strong> <span id="sum-notes"></span></p></div>` : ''}

    <div class="card">
      <h3 style="font-size:.95rem;margin-bottom:.5rem">Payment method</h3>
      <div class="pay-options">
        <label class="pay-option selected" id="po-wallet">
          <input type="radio" name="pay" value="WALLET" checked />
          <div>
            <div class="po-label">Wallet</div>
            <div class="po-sub" id="wallet-bal-display">Loading balance…</div>
          </div>
        </label>
        <label class="pay-option" id="po-online">
          <input type="radio" name="pay" value="ONLINE" />
          <div>
            <div class="po-label">Pay Online</div>
            <div class="po-sub">Razorpay / UPI / Card</div>
          </div>
        </label>
      </div>
    </div>
  `;

  // Address (XSS-safe)
  document.getElementById('sum-addr').textContent = state.addressLine;
  if (state.specialInstructions) {
    const notesEl = document.getElementById('sum-notes');
    if (notesEl) notesEl.textContent = state.specialInstructions;
  }

  // Payment option toggle
  document.querySelectorAll('.pay-option').forEach(opt => {
    opt.addEventListener('click', () => {
      document.querySelectorAll('.pay-option').forEach(o => o.classList.remove('selected'));
      opt.classList.add('selected');
      opt.querySelector('input').checked = true;
    });
  });

  // Fetch wallet balance
  loadWalletBalance();
}

async function loadWalletBalance() {
  try {
    const w = await api.get('/wallet');
    const el = document.getElementById('wallet-bal-display');
    if (el && w) {
      const avail = parseFloat(w.availableBalance || w.balance || 0);
      el.textContent = `Balance: ₹${avail.toFixed(2)}`;
    }
  } catch {
    const el = document.getElementById('wallet-bal-display');
    if (el) el.textContent = 'Could not load balance';
  }
}

/* ── Submit booking ───────────────────────────────────────────────── */
async function submitBooking() {
  const nextBtn = document.getElementById('btn-next');
  nextBtn.disabled = true;
  nextBtn.innerHTML = '<span class="spinner"></span> Confirming…';

  const payload = {
    serviceType:         state.serviceType,
    bookingType:         state.bookingType,
    addressLine:         state.addressLine,
    latitude:            state.latitude,
    longitude:           state.longitude,
    durationHours:       state.durationHours,
    specialInstructions: state.specialInstructions || null,
  };

  if (state.bookingType === 'SCHEDULED' && state.scheduledAt) {
    payload.scheduledAt = new Date(state.scheduledAt).toISOString();
  }

  try {
    const booking = await api.post('/bookings', payload);
    showSuccess(booking);
  } catch {
    nextBtn.disabled = false;
    nextBtn.textContent = 'Confirm Booking';
  }
}

function showSuccess(booking) {
  // Hide step UI, show success
  document.getElementById('step-body').classList.add('hidden');
  document.getElementById('step-nav').classList.add('hidden');
  document.querySelector('.step-bar').classList.add('hidden');

  const successEl = document.getElementById('booking-success');
  successEl.classList.remove('hidden');

  const assigned = booking.helperName;
  successEl.innerHTML = `
    <div class="success-screen">
      <span class="success-icon">🎉</span>
      <h2>Booking Confirmed!</h2>
      <p class="booking-id">ID: ${esc(String(booking.id).slice(0, 8))}…</p>
      ${assigned
        ? `<p class="mt-2">Helper: <strong id="suc-helper"></strong></p>`
        : `<p class="mt-2"><span class="spinner"></span><br>We're finding a helper for you…</p>`
      }
      <div class="mt-3" style="display:flex;gap:.5rem;justify-content:center;flex-wrap:wrap">
        <button class="btn btn-primary" id="btn-go-tracking">Track Booking</button>
        <button class="btn btn-outline" id="btn-go-home">Home</button>
      </div>
    </div>
  `;

  if (assigned) {
    document.getElementById('suc-helper').textContent = booking.helperName;
  }

  document.getElementById('btn-go-tracking').addEventListener('click', () => {
    router.navigate('/customer/tracking/' + booking.id);
  });
  document.getElementById('btn-go-home').addEventListener('click', () => {
    router.navigate('/customer/home');
  });

  // Subscribe to WebSocket for real-time status updates
  subscribeBookingStatus(booking.id);
}

function subscribeBookingStatus(bookingId) {
  const token = auth.token();
  if (!token || typeof SockJS === 'undefined' || typeof StompJs === 'undefined') return;
  try {
    const socket = new SockJS('/ws');
    wsClient = new StompJs.Client({
      webSocketFactory: () => socket,
      connectHeaders: { 'Authorization': 'Bearer ' + token },
      reconnectDelay: 5000,
      debug: () => {},
      onConnect: () => {
        wsClient.subscribe('/topic/booking/' + bookingId + '/status', (msg) => {
          const event = JSON.parse(msg.body);
          if (event.status === 'ASSIGNED' || event.status === 'HELPER_EN_ROUTE') {
            toast('A helper has been assigned!', 'success');
            // Update success screen if still showing
            const successEl = document.getElementById('booking-success');
            if (successEl && !successEl.classList.contains('hidden')) {
              router.navigate('/customer/tracking/' + bookingId);
            }
          }
        });
      },
    });
    wsClient.activate();
    // Auto-disconnect after 5 min to avoid leaks
    wsTimeout = setTimeout(() => { cleanupBooking(); }, 300000);
  } catch { /* WebSocket not critical here */ }
}
