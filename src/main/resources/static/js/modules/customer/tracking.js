/**
 * customer/tracking.js — Full-screen live tracking with Leaflet map (ES module).
 *
 * - Customer location marker (blue)
 * - Helper location marker (red, moving in real-time)
 * - Route line between them
 * - Status banner + helper info card
 * - Live ETA countdown
 */
import { api }  from '../../api.js';
import { auth } from '../../auth.js';
import { toast } from '../../toast.js';
import { router } from '../../router.js';

let stompClient = null;
let map = null;
let helperMarker = null;
let destMarker = null;
let routeLine = null;
let etaSeconds = 0;
let etaTimer = null;

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

/* ── Cleanup ──────────────────────────────────────────────────────── */
function cleanup() {
  if (etaTimer) { clearInterval(etaTimer); etaTimer = null; }
  if (stompClient) { try { stompClient.deactivate(); } catch {} stompClient = null; }
  if (map) { map.remove(); map = null; }
  helperMarker = null;
  destMarker = null;
  routeLine = null;
}

/* ── WebSocket ────────────────────────────────────────────────────── */
function connectWs(token, onConnected) {
  if (!token || typeof SockJS === 'undefined' || typeof StompJs === 'undefined') return;
  const socket = new SockJS('/ws');
  stompClient = new StompJs.Client({
    webSocketFactory: () => socket,
    connectHeaders: { 'Authorization': 'Bearer ' + token },
    reconnectDelay: 5000,
    debug: () => {},
    onConnect: () => { if (onConnected) onConnected(); },
    onStompError: (frame) => { console.error('STOMP error', frame.headers?.message); },
  });
  stompClient.activate();
}

/* ── Map helpers ──────────────────────────────────────────────────── */
function initMap(containerId, lat, lng) {
  map = L.map(containerId).setView([lat, lng], 14);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap', maxZoom: 19,
  }).addTo(map);

  const custIcon = L.divIcon({
    className: 'dest-marker',
    html: '<div class="marker-pin" style="color:#2563eb;font-size:1.8rem">📍</div>',
    iconSize: [30, 30], iconAnchor: [15, 30],
  });
  destMarker = L.marker([lat, lng], { icon: custIcon }).addTo(map);
  destMarker.bindPopup('Your location').openPopup();
}

function moveHelper(lat, lng) {
  if (!map) return;
  if (!helperMarker) {
    const icon = L.divIcon({
      className: 'helper-marker',
      html: '<div class="marker-pin" style="color:#dc2626;font-size:1.8rem">🚗</div>',
      iconSize: [30, 30], iconAnchor: [15, 30],
    });
    helperMarker = L.marker([lat, lng], { icon }).addTo(map);
  } else {
    helperMarker.setLatLng([lat, lng]);
  }

  // Route line
  if (destMarker) {
    const dest = destMarker.getLatLng();
    if (routeLine) map.removeLayer(routeLine);
    routeLine = L.polyline([[lat, lng], [dest.lat, dest.lng]], {
      color: '#4f46e5', weight: 3, dashArray: '10,10', opacity: .7,
    }).addTo(map);
    map.fitBounds(L.featureGroup([helperMarker, destMarker]).getBounds().pad(0.2));
  }
}

/* ── ETA countdown ────────────────────────────────────────────────── */
function startEtaCountdown(minutes) {
  etaSeconds = Math.max(0, Math.round(minutes * 60));
  updateEtaDisplay();
  if (etaTimer) clearInterval(etaTimer);
  etaTimer = setInterval(() => {
    if (etaSeconds > 0) etaSeconds--;
    updateEtaDisplay();
  }, 1000);
}

function updateEtaDisplay() {
  const el = document.getElementById('tracking-eta-countdown');
  if (!el) return;
  if (etaSeconds <= 0) { el.textContent = 'Arriving…'; return; }
  const m = Math.floor(etaSeconds / 60);
  const s = etaSeconds % 60;
  el.textContent = `${m}:${String(s).padStart(2, '0')}`;
}

/* ── Status banner ────────────────────────────────────────────────── */
const STATUS_TEXT = {
  PENDING_ASSIGNMENT: '🔍 Finding a helper…',
  ASSIGNED:           '✅ Helper assigned',
  HELPER_EN_ROUTE:    '🚗 Helper is on the way',
  IN_PROGRESS:        '🔧 Service in progress',
  COMPLETED:          '🎉 Service completed!',
  CANCELLED:          '❌ Booking cancelled',
};

function updateStatusUI(status) {
  const el = document.getElementById('tracking-status-text');
  if (el) el.textContent = STATUS_TEXT[status] || status.replace(/_/g, ' ');

  const banner = document.getElementById('tracking-status-banner');
  if (!banner) return;
  banner.className = 'tracking-banner';
  if (status === 'HELPER_EN_ROUTE') banner.classList.add('status-en-route');
  else if (status === 'IN_PROGRESS') banner.classList.add('status-in-progress');
  else if (status === 'COMPLETED') banner.classList.add('status-completed');
  else if (status === 'ASSIGNED') banner.classList.add('status-assigned');
}

/* ── Main render ──────────────────────────────────────────────────── */
export async function render(container, params) {
  cleanup();
  const bookingId = params.id;

  // Register cleanup for when user navigates away
  router.onTeardown(cleanup);

  container.innerHTML = `
    <div id="tracking-status-banner" class="tracking-banner">
      <span id="tracking-status-text">Loading…</span>
      <span id="tracking-eta" class="tracking-eta"></span>
    </div>
    <div class="eta-countdown" id="tracking-eta-countdown"></div>
    <div id="tracking-map" class="map-container"></div>

    <!-- Helper info card -->
    <div id="helper-card-slot"></div>

    <div class="tracking-info">
      <p id="tracking-detail">Connecting…</p>
    </div>
  `;

  // Fetch booking details to get destination + helper info
  let booking;
  try {
    booking = await api.get('/bookings/' + bookingId);
  } catch {
    container.innerHTML = '<div class="empty-state"><span class="empty-icon">⚠️</span><p>Could not load booking.</p></div>';
    return;
  }

  const destLat = booking.latitude  || 28.6139;
  const destLng = booking.longitude || 77.2090;

  initMap('tracking-map', destLat, destLng);
  updateStatusUI(booking.status);

  // Render helper card if assigned
  if (booking.helperId) {
    renderHelperCard(booking);
  }

  // Try to get latest known location (this endpoint returns raw JSON, not ApiResponse)
  try {
    const token = auth.token();
    const res = await fetch('/api/v1/tracking/' + bookingId + '/latest', {
      headers: token ? { 'Authorization': 'Bearer ' + token } : {}
    });
    if (res.ok) {
      const latest = await res.json();
      if (latest && latest.lat) {
        moveHelper(latest.lat, latest.lng);
        if (latest.etaMinutes) startEtaCountdown(latest.etaMinutes);
      }
    }
  } catch { /* no location yet */ }

  // WebSocket subscriptions
  const token = auth.token();
  connectWs(token, () => {
    if (!stompClient || !stompClient.connected) return;

    stompClient.subscribe('/topic/booking/' + bookingId + '/location', (msg) => {
      const loc = JSON.parse(msg.body);
      moveHelper(loc.lat, loc.lng);
      if (loc.etaMinutes != null) {
        startEtaCountdown(loc.etaMinutes);
        const etaEl = document.getElementById('tracking-eta');
        if (etaEl) etaEl.textContent = Math.round(loc.etaMinutes) + ' min';
      }
      const detail = document.getElementById('tracking-detail');
      if (detail) {
        detail.textContent = 'Last update: ' + new Date(loc.timestamp || Date.now()).toLocaleTimeString();
      }
    });

    stompClient.subscribe('/topic/booking/' + bookingId + '/status', (msg) => {
      const event = JSON.parse(msg.body);
      updateStatusUI(event.status);
      if (event.status === 'COMPLETED') {
        const detail = document.getElementById('tracking-detail');
        if (detail) detail.textContent = 'Service completed! Thank you for choosing HomeCare.';
        if (etaTimer) { clearInterval(etaTimer); etaTimer = null; }
        const cd = document.getElementById('tracking-eta-countdown');
        if (cd) cd.textContent = '';
        setTimeout(cleanup, 5000);
      }
    });

    updateStatusUI(booking.status);
    const detail = document.getElementById('tracking-detail');
    if (detail) detail.textContent = 'Connected — receiving live updates';
  });
}

function renderHelperCard(booking) {
  const slot = document.getElementById('helper-card-slot');
  if (!slot) return;
  slot.innerHTML = `
    <div class="tracking-helper-card">
      <div class="thc-avatar">🧑‍🔧</div>
      <div class="thc-info">
        <div class="thc-name" id="thc-name-val"></div>
        <div class="thc-rating">
          ${booking.rating ? '⭐ ' + booking.rating + '/5' : ''}
        </div>
      </div>
      <div class="thc-actions">
        <a class="icon-btn" href="tel:" title="Call helper">📞</a>
        <button class="icon-btn" title="Help" id="btn-help-chat">💬</button>
      </div>
    </div>
  `;
  document.getElementById('thc-name-val').textContent = booking.helperName || 'Your helper';
  const helpBtn = document.getElementById('btn-help-chat');
  if (helpBtn) helpBtn.addEventListener('click', () => toast('Chat coming soon', 'info'));
}
