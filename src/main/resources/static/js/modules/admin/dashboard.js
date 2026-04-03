/**
 * admin/dashboard.js — Admin dashboard with KPIs, live map, charts.
 *
 * - KPI strip: Today bookings, Active now, Online helpers, Today revenue, Pending verifications
 *   Auto-refreshes every 30s
 * - Live bookings map (Leaflet): customer blue, helper red, dashed line
 * - Charts (Chart.js from CDN): doughnut + line chart
 * - Top 5 helpers by rating (table)
 * - Recent cancellations (table with reason)
 */
import { api }    from '../../api.js';
import { auth }   from '../../auth.js';
import { router } from '../../router.js';

let kpiInterval = null;
let map = null;
let stompClient = null;
let chartLoaded = false;
let bookingsChart = null;
let revenueChart = null;
const markers = {};

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

function cleanup() {
  if (kpiInterval) { clearInterval(kpiInterval); kpiInterval = null; }
  if (stompClient) { try { stompClient.deactivate(); } catch {} stompClient = null; }
  if (map) { map.remove(); map = null; }
  if (bookingsChart) { bookingsChart.destroy(); bookingsChart = null; }
  if (revenueChart) { revenueChart.destroy(); revenueChart = null; }
  Object.keys(markers).forEach(k => delete markers[k]);
}

async function loadChartJs() {
  if (chartLoaded || typeof Chart !== 'undefined') { chartLoaded = true; return; }
  return new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.min.js';
    script.onload = () => { chartLoaded = true; resolve(); };
    script.onerror = reject;
    document.head.appendChild(script);
  });
}

export async function render(container) {
  cleanup();
  router.onTeardown(cleanup);

  container.innerHTML = `
    <div class="section-header">
      <h2>Admin Dashboard</h2>
    </div>
    <nav class="sub-nav">
      <a href="#/admin/dashboard" class="sub-nav-btn active">Overview</a>
      <a href="#/admin/bookings"  class="sub-nav-btn">Bookings</a>
      <a href="#/admin/users"     class="sub-nav-btn">Users</a>
      <a href="#/admin/helpers"   class="sub-nav-btn">Helpers</a>
      <a href="#/admin/payments"  class="sub-nav-btn">Payments</a>
      <a href="#/admin/disputes"  class="sub-nav-btn">Disputes</a>
      <a href="#/admin/referrals" class="sub-nav-btn">Referrals</a>
      <a href="#/admin/subscriptions" class="sub-nav-btn">Subs</a>
      <a href="#/admin/config"    class="sub-nav-btn">Config</a>
    </nav>

    <!-- KPI Strip -->
    <div class="stats-grid stats-grid-5" id="kpi-strip">
      <div class="stat-card"><div class="stat-value" id="kpi-bookings">—</div><div class="stat-label">Today Bookings</div></div>
      <div class="stat-card"><div class="stat-value" id="kpi-active">—</div><div class="stat-label">Active Now</div></div>
      <div class="stat-card"><div class="stat-value" id="kpi-online">—</div><div class="stat-label">Online Helpers</div></div>
      <div class="stat-card"><div class="stat-value" id="kpi-revenue">—</div><div class="stat-label">Today Revenue</div></div>
      <div class="stat-card"><div class="stat-value" id="kpi-pending">—</div><div class="stat-label">Pending Verify</div></div>
    </div>

    <!-- Secondary KPIs -->
    <div class="stats-grid" style="margin-bottom:1.5rem">
      <div class="stat-card" style="cursor:pointer" onclick="location.hash='#/admin/disputes'"><div class="stat-value" id="kpi-disputes">—</div><div class="stat-label">⚖️ Open Disputes</div></div>
      <div class="stat-card" style="cursor:pointer" onclick="location.hash='#/admin/referrals'"><div class="stat-value" id="kpi-referrals">—</div><div class="stat-label">🎁 Referral Signups</div></div>
      <div class="stat-card" style="cursor:pointer" onclick="location.hash='#/admin/subscriptions'"><div class="stat-value" id="kpi-subs">—</div><div class="stat-label">📋 Active Subs</div></div>
      <div class="stat-card"><div class="stat-value" id="kpi-chat">—</div><div class="stat-label">💬 Messages Today</div></div>
    </div>

    <!-- Live Map -->
    <div class="card mb-2">
      <h3 style="font-size:.9rem;margin-bottom:.5rem">🗺️ Live Bookings Map</h3>
      <div id="admin-map" class="map-container" style="height:350px"></div>
    </div>

    <!-- Charts Row -->
    <div class="charts-row">
      <div class="card chart-card">
        <h3 style="font-size:.9rem;margin-bottom:.5rem">Bookings by Service</h3>
        <canvas id="chart-bookings-service"></canvas>
      </div>
      <div class="card chart-card">
        <h3 style="font-size:.9rem;margin-bottom:.5rem">Revenue Last 7 Days</h3>
        <canvas id="chart-revenue"></canvas>
      </div>
    </div>

    <!-- Top Helpers + Cancellations -->
    <div class="charts-row mt-2">
      <div class="card chart-card">
        <h3 style="font-size:.9rem;margin-bottom:.5rem">⭐ Top 5 Helpers by Rating</h3>
        <div id="top-helpers-table"></div>
      </div>
      <div class="card chart-card">
        <h3 style="font-size:.9rem;margin-bottom:.5rem">❌ Recent Cancellations</h3>
        <div id="cancel-table"></div>
      </div>
    </div>
  `;

  // Init map
  initMap();

  // Load KPIs immediately, then every 30s
  await refreshKPIs();
  kpiInterval = setInterval(refreshKPIs, 30_000);

  // Load charts
  try {
    await loadChartJs();
    renderCharts();
  } catch {
    // Chart.js failed to load
  }

  // Load tables
  loadTopHelpers();
  loadCancellations();

  // WebSocket for live booking updates
  connectAdminWs();
}

async function refreshKPIs() {
  try {
    const d = await api.get('/admin/dashboard');
    if (!d) return;
    const el = (id) => document.getElementById(id);
    const kpiBookings = el('kpi-bookings');
    const kpiActive = el('kpi-active');
    const kpiOnline = el('kpi-online');
    const kpiRevenue = el('kpi-revenue');
    const kpiPending = el('kpi-pending');

    if (kpiBookings) kpiBookings.textContent = d.todayBookings || 0;
    if (kpiActive) kpiActive.textContent = d.activeBookings || 0;
    if (kpiOnline) kpiOnline.textContent = d.onlineHelpers || 0;
    if (kpiRevenue) kpiRevenue.textContent = '₹' + (parseFloat(d.todayRevenue) || 0).toLocaleString('en-IN');
    if (kpiPending) kpiPending.textContent = d.pendingVerifications || 0;

    // Secondary KPIs
    const kpiDisputes = el('kpi-disputes');
    const kpiReferrals = el('kpi-referrals');
    const kpiSubs = el('kpi-subs');
    const kpiChat = el('kpi-chat');
    if (kpiDisputes) kpiDisputes.textContent = d.openDisputes || 0;
    if (kpiReferrals) kpiReferrals.textContent = d.referralSignups || 0;
    if (kpiSubs) kpiSubs.textContent = d.activeSubscriptions || 0;
    if (kpiChat) kpiChat.textContent = d.messagesToday || 0;

    // Update charts data if available
    if (d.bookingsByService && bookingsChart) {
      updateBookingsChart(d.bookingsByService);
    }
    if (d.revenueChart && revenueChart) {
      updateRevenueChart(d.revenueChart);
    }
  } catch {
    // Silent — KPIs continue from last known state
  }
}

function initMap() {
  const mapEl = document.getElementById('admin-map');
  if (!mapEl || typeof L === 'undefined') return;

  map = L.map(mapEl).setView([20.5937, 78.9629], 5); // Center on India
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap', maxZoom: 19,
  }).addTo(map);
}

function connectAdminWs() {
  const token = auth.token();
  if (!token || typeof SockJS === 'undefined' || typeof StompJs === 'undefined') return;

  const socket = new SockJS('/ws');
  stompClient = new StompJs.Client({
    webSocketFactory: () => socket,
    connectHeaders: { 'Authorization': 'Bearer ' + token },
    reconnectDelay: 5000,
    debug: () => {},
    onConnect: () => {
      if (!stompClient || !stompClient.connected) return;
      stompClient.subscribe('/topic/admin/bookings', (msg) => {
        try {
          const data = JSON.parse(msg.body);
          updateMapMarker(data);
        } catch { /* ignore bad messages */ }
      });
    },
    onStompError: () => {},
  });
  stompClient.activate();
}

function updateMapMarker(data) {
  if (!map) return;
  const key = data.bookingId || data.helperId;
  if (!key) return;

  // Remove old marker
  if (markers[key]) {
    map.removeLayer(markers[key].helper);
    if (markers[key].line) map.removeLayer(markers[key].line);
  }

  const helperIcon = L.divIcon({
    className: 'helper-marker',
    html: '<div class="marker-pin" style="color:#dc2626;font-size:1.5rem">🔴</div>',
    iconSize: [24, 24], iconAnchor: [12, 24],
  });

  const helperMarker = L.marker([data.lat, data.lng], { icon: helperIcon }).addTo(map);
  helperMarker.bindPopup('Helper: ' + (data.helperId || ''));

  markers[key] = { helper: helperMarker };

  // If customer location is in data, draw line
  if (data.customerLat && data.customerLng) {
    const custIcon = L.divIcon({
      className: 'dest-marker',
      html: '<div class="marker-pin" style="color:#2563eb;font-size:1.5rem">🔵</div>',
      iconSize: [24, 24], iconAnchor: [12, 24],
    });
    const custMarker = L.marker([data.customerLat, data.customerLng], { icon: custIcon }).addTo(map);
    const line = L.polyline(
      [[data.lat, data.lng], [data.customerLat, data.customerLng]],
      { color: '#4f46e5', weight: 2, dashArray: '8,8', opacity: 0.6 }
    ).addTo(map);
    markers[key].customer = custMarker;
    markers[key].line = line;
  }
}

async function renderCharts() {
  if (typeof Chart === 'undefined') return;

  try {
    const dashboard = await api.get('/admin/dashboard');

    // Bookings by Service — Doughnut
    const serviceData = dashboard?.bookingsByService || {};
    const serviceLabels = Object.keys(serviceData);
    const serviceValues = Object.values(serviceData);
    const serviceColors = ['#4f46e5', '#059669', '#d97706', '#dc2626'];

    const ctx1 = document.getElementById('chart-bookings-service');
    if (ctx1) {
      bookingsChart = new Chart(ctx1, {
        type: 'doughnut',
        data: {
          labels: serviceLabels.map(l => l.replace(/_/g, ' ')),
          datasets: [{
            data: serviceValues,
            backgroundColor: serviceColors.slice(0, serviceLabels.length),
          }],
        },
        options: { responsive: true, plugins: { legend: { position: 'bottom' } } },
      });
    }

    // Revenue Last 7 Days — Line
    const revenueData = dashboard?.revenueChart || [];
    const revLabels = revenueData.map(r => r.date);
    const revValues = revenueData.map(r => parseFloat(r.revenue) || 0);

    const ctx2 = document.getElementById('chart-revenue');
    if (ctx2) {
      revenueChart = new Chart(ctx2, {
        type: 'line',
        data: {
          labels: revLabels,
          datasets: [{
            label: 'Revenue (₹)',
            data: revValues,
            borderColor: '#4f46e5',
            backgroundColor: 'rgba(79,70,229,.1)',
            fill: true,
            tension: 0.3,
          }],
        },
        options: { responsive: true, plugins: { legend: { display: false } } },
      });
    }
  } catch {
    // Charts fail silently
  }
}

function updateBookingsChart(data) {
  if (!bookingsChart) return;
  bookingsChart.data.labels = Object.keys(data).map(l => l.replace(/_/g, ' '));
  bookingsChart.data.datasets[0].data = Object.values(data);
  bookingsChart.update();
}

function updateRevenueChart(data) {
  if (!revenueChart) return;
  revenueChart.data.labels = data.map(r => r.date);
  revenueChart.data.datasets[0].data = data.map(r => parseFloat(r.revenue) || 0);
  revenueChart.update();
}

async function loadTopHelpers() {
  const slot = document.getElementById('top-helpers-table');
  if (!slot) return;

  try {
    const helpers = await api.get('/admin/analytics/helpers?metric=topRated&limit=5');
    if (!helpers || !helpers.length) {
      slot.innerHTML = '<p class="text-muted">No data yet.</p>';
      return;
    }

    slot.innerHTML = `
      <table class="admin-table">
        <thead><tr><th>Name</th><th>Rating</th><th>Jobs</th></tr></thead>
        <tbody>
          ${helpers.map(h => `
            <tr>
              <td>${esc(h.name)}</td>
              <td>⭐ ${h.rating ? h.rating.toFixed(1) : '—'}</td>
              <td>${h.totalJobsCompleted || 0}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  } catch {
    slot.innerHTML = '<p class="text-muted">Could not load data.</p>';
  }
}

async function loadCancellations() {
  const slot = document.getElementById('cancel-table');
  if (!slot) return;

  try {
    const resp = await api.get('/admin/bookings?status=CANCELLED&page=0&size=5');
    const bookings = resp?.content || [];

    if (!bookings.length) {
      slot.innerHTML = '<p class="text-muted">No recent cancellations.</p>';
      return;
    }

    slot.innerHTML = `
      <table class="admin-table">
        <thead><tr><th>ID</th><th>Customer</th><th>Service</th><th>Date</th></tr></thead>
        <tbody>
          ${bookings.map(b => `
            <tr>
              <td style="font-size:.75rem">${(b.id || '').substring(0, 8)}…</td>
              <td>${esc(b.customerName || '—')}</td>
              <td>${esc(b.serviceType || '—')}</td>
              <td style="font-size:.78rem">${b.createdAt ? new Date(b.createdAt).toLocaleDateString() : '—'}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  } catch {
    slot.innerHTML = '<p class="text-muted">Could not load data.</p>';
  }
}
