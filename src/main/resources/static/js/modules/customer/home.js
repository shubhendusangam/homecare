/**
 * customer/home.js — Customer home screen.
 *
 * - Greeting with customer name
 * - Service selector cards (4 services)
 * - Active booking banner (if any is ASSIGNED / HELPER_EN_ROUTE / IN_PROGRESS)
 * - Quick re-book: last 3 completed bookings as chips
 * - Upcoming scheduled bookings (next 3)
 */
import { auth }   from '../../auth.js';
import { api }    from '../../api.js';
import { router } from '../../router.js';

const SERVICES = [
  { type: 'CLEANING',     icon: '🧹', name: 'Cleaning',     price: '₹299' },
  { type: 'COOKING',      icon: '🍳', name: 'Cooking',      price: '₹399' },
  { type: 'BABYSITTING',  icon: '👶', name: 'Babysitting',  price: '₹499' },
  { type: 'ELDERLY_HELP', icon: '🧓', name: 'Elderly Help', price: '₹449' },
];

const SERVICE_ICON = { CLEANING: '🧹', COOKING: '🍳', BABYSITTING: '👶', ELDERLY_HELP: '🧓' };

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

export async function render(container) {
  const user = auth.user();

  container.innerHTML = `
    <div class="section-header">
      <h2 id="home-greeting"></h2>
    </div>

    <nav class="sub-nav">
      <a href="#/customer/home"    class="sub-nav-btn active">Home</a>
      <a href="#/customer/history" class="sub-nav-btn">My Bookings</a>
      <a href="#/customer/wallet"  class="sub-nav-btn">Wallet</a>
      <a href="#/customer/profile" class="sub-nav-btn">Profile</a>
    </nav>

    <!-- Active booking banner -->
    <div id="active-banner-slot"></div>

    <!-- Service cards -->
    <h3 class="mb-2" style="font-size:.95rem">Book a Service</h3>
    <div class="services-grid" id="service-grid"></div>

    <!-- Quick re-book chips -->
    <div id="rebook-section" class="mt-3 hidden">
      <h3 style="font-size:.95rem;margin-bottom:.5rem">Quick Re-book</h3>
      <div class="chip-row" id="rebook-chips"></div>
    </div>

    <!-- Upcoming scheduled -->
    <div id="upcoming-section" class="mt-3 hidden">
      <h3 style="font-size:.95rem;margin-bottom:.5rem">Upcoming</h3>
      <ul class="data-list" id="upcoming-list"></ul>
    </div>
  `;

  // Greeting (XSS-safe)
  document.getElementById('home-greeting').textContent =
    'Hi, ' + (user?.name || 'there') + ' 👋';

  // Service cards
  const grid = document.getElementById('service-grid');
  grid.innerHTML = SERVICES.map(s => `
    <div class="service-card" data-type="${s.type}">
      <span class="service-icon">${s.icon}</span>
      <h3>${s.name}</h3>
      <p>From ${s.price}</p>
      <small class="text-muted" id="avail-${s.type}"></small>
    </div>
  `).join('');

  grid.querySelectorAll('.service-card').forEach(card => {
    card.addEventListener('click', () => {
      router.navigate('/customer/book?service=' + card.dataset.type);
    });
  });

  // Load bookings in parallel (don't block render)
  loadActiveAndRecent();
}

async function loadActiveAndRecent() {
  try {
    const resp = await api.get('/bookings?page=0&size=20');
    const bookings = resp?.content || [];

    // Active bookings (not finished)
    const activeStatuses = ['ASSIGNED', 'HELPER_EN_ROUTE', 'IN_PROGRESS', 'PENDING_ASSIGNMENT'];
    const active = bookings.filter(b => activeStatuses.includes(b.status));
    renderActiveBanner(active);

    // Quick re-book — last 3 completed
    const completed = bookings
      .filter(b => b.status === 'COMPLETED')
      .slice(0, 3);
    renderRebookChips(completed);

    // Upcoming scheduled
    const upcoming = bookings
      .filter(b => b.bookingType === 'SCHEDULED' && b.status === 'PENDING_ASSIGNMENT' && b.scheduledAt)
      .sort((a, b) => new Date(a.scheduledAt) - new Date(b.scheduledAt))
      .slice(0, 3);
    renderUpcoming(upcoming);
  } catch {
    // Silent — home still works without this data
  }
}

function renderActiveBanner(active) {
  const slot = document.getElementById('active-banner-slot');
  if (!active.length) { slot.innerHTML = ''; return; }
  const b = active[0]; // most relevant active booking
  const icon = SERVICE_ICON[b.serviceType] || '📦';
  const statusText = (b.status || '').replace(/_/g, ' ');
  slot.innerHTML = `
    <div class="active-banner" id="active-banner">
      <div class="ab-left">
        <span class="ab-icon">${icon}</span>
        <div>
          <div class="ab-title">${esc(b.serviceType)} — ${esc(statusText)}</div>
          <div class="ab-sub">${b.helperName ? esc(b.helperName) : 'Finding helper…'}</div>
        </div>
      </div>
      <span class="ab-arrow">›</span>
    </div>
  `;
  document.getElementById('active-banner').addEventListener('click', () => {
    router.navigate('/customer/tracking/' + b.id);
  });
}

function renderRebookChips(completed) {
  if (!completed.length) return;
  const sec = document.getElementById('rebook-section');
  const row = document.getElementById('rebook-chips');
  sec.classList.remove('hidden');
  row.innerHTML = completed.map(b => {
    const icon = SERVICE_ICON[b.serviceType] || '📦';
    return `<span class="chip" data-type="${esc(b.serviceType)}" data-dur="${b.durationHours}">${icon} ${esc(b.serviceType)} · ${b.durationHours}h</span>`;
  }).join('');
  row.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', () => {
      router.navigate('/customer/book?service=' + chip.dataset.type + '&duration=' + chip.dataset.dur);
    });
  });
}

function renderUpcoming(upcoming) {
  if (!upcoming.length) return;
  const sec = document.getElementById('upcoming-section');
  const list = document.getElementById('upcoming-list');
  sec.classList.remove('hidden');
  list.innerHTML = upcoming.map(b => {
    const icon = SERVICE_ICON[b.serviceType] || '📦';
    const dt = new Date(b.scheduledAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
    return `<li class="data-list-item">
      <div><span style="margin-right:.4rem">${icon}</span> ${esc(b.serviceType)}</div>
      <small class="text-muted">${esc(dt)}</small>
    </li>`;
  }).join('');
}
