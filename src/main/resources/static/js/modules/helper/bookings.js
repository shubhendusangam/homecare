/**
 * helper/bookings.js — Helper's bookings with Pending / Upcoming tabs.
 *
 * - Pending: PENDING_ASSIGNMENT bookings awaiting acceptance
 *   [Accept] [Decline] buttons
 *   15-second auto-dismiss if helper doesn't respond
 * - Upcoming: ASSIGNED bookings sorted by scheduledAt
 *   [Start Journey] button
 */
import { api }    from '../../api.js';
import { toast }  from '../../toast.js';
import { router } from '../../router.js';

const SERVICE_ICON = { CLEANING: '🧹', COOKING: '🍳', BABYSITTING: '👶', ELDERLY_HELP: '🧓' };
const autoDismissTimers = [];

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

function cleanup() {
  autoDismissTimers.forEach(t => clearTimeout(t));
  autoDismissTimers.length = 0;
}

export async function render(container) {
  router.onTeardown(cleanup);

  container.innerHTML = `
    <div class="section-header"><h2>My Bookings</h2></div>
    <nav class="sub-nav">
      <a href="#/helper/home"     class="sub-nav-btn">Dashboard</a>
      <a href="#/helper/bookings" class="sub-nav-btn active">Bookings</a>
      <a href="#/helper/earnings" class="sub-nav-btn">Earnings</a>
      <a href="#/helper/profile"  class="sub-nav-btn">Profile</a>
    </nav>

    <div class="toggle-row" id="booking-tabs">
      <button class="toggle-btn active" data-tab="pending">Pending</button>
      <button class="toggle-btn" data-tab="upcoming">Upcoming</button>
    </div>

    <div id="bookings-pending"></div>
    <div id="bookings-upcoming" class="hidden"></div>
  `;

  // Tab switching
  const tabBtns = container.querySelectorAll('#booking-tabs .toggle-btn');
  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      tabBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      const tab = btn.dataset.tab;
      document.getElementById('bookings-pending').classList.toggle('hidden', tab !== 'pending');
      document.getElementById('bookings-upcoming').classList.toggle('hidden', tab !== 'upcoming');
    });
  });

  loadPending();
  loadUpcoming();
}

async function loadPending() {
  const slot = document.getElementById('bookings-pending');
  if (!slot) return;

  slot.innerHTML = '<div class="text-center text-muted mt-2"><span class="spinner"></span></div>';

  try {
    const bookings = await api.get('/helpers/bookings/pending');
    if (!bookings || !bookings.length) {
      slot.innerHTML = `
        <div class="empty-state">
          <span class="empty-icon">📭</span>
          <p>No pending bookings right now.</p>
        </div>
      `;
      return;
    }

    slot.innerHTML = bookings.map((b, i) => {
      const icon = SERVICE_ICON[b.serviceType] || '📦';
      const price = b.totalPrice ? '₹' + b.totalPrice : '';
      const dur = b.durationHours ? b.durationHours + 'h' : '';
      const area = b.addressLine ? b.addressLine.split(',').slice(-2).join(',').trim() : 'Nearby';
      return `
        <div class="card mb-2 pending-card" id="pending-${b.id}" data-idx="${i}">
          <div style="display:flex;align-items:center;gap:.6rem;margin-bottom:.5rem">
            <span style="font-size:1.5rem">${icon}</span>
            <div style="flex:1">
              <div style="font-weight:600;font-size:.9rem">${esc(b.serviceType)}</div>
              <div class="text-muted" style="font-size:.8rem">📍 ${esc(area)}</div>
            </div>
            <div style="text-align:right">
              <div style="font-weight:600;color:var(--primary)">${esc(price)}</div>
              <div class="text-muted" style="font-size:.78rem">${esc(dur)}</div>
            </div>
          </div>
          <div class="pending-timer" id="timer-${b.id}"></div>
          <div style="display:flex;gap:.5rem;margin-top:.5rem">
            <button class="btn btn-primary btn-sm" style="flex:1" data-accept="${b.id}">✓ Accept</button>
            <button class="btn btn-outline btn-sm" style="flex:1" data-decline="${b.id}">✗ Decline</button>
          </div>
        </div>
      `;
    }).join('');

    // Auto-dismiss timers (15s)
    bookings.forEach(b => {
      let remaining = 15;
      const timerEl = document.getElementById('timer-' + b.id);
      if (timerEl) {
        timerEl.textContent = remaining + 's remaining';
        timerEl.style.cssText = 'font-size:.75rem;color:var(--toast-warn);text-align:center';
      }
      const timer = setInterval(() => {
        remaining--;
        if (timerEl) timerEl.textContent = remaining + 's remaining';
        if (remaining <= 0) {
          clearInterval(timer);
          const card = document.getElementById('pending-' + b.id);
          if (card) {
            card.style.opacity = '0.4';
            card.style.pointerEvents = 'none';
            if (timerEl) timerEl.textContent = 'Expired — returned to pool';
          }
        }
      }, 1000);
      autoDismissTimers.push(timer);
    });

    // Accept/Decline handlers
    slot.querySelectorAll('[data-accept]').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.accept;
        btn.disabled = true;
        try {
          await api.patch('/helpers/bookings/' + id + '/accept');
          toast('Booking accepted!', 'success');
          const card = document.getElementById('pending-' + id);
          if (card) card.remove();
          loadUpcoming(); // refresh upcoming
        } catch {
          btn.disabled = false;
        }
      });
    });

    slot.querySelectorAll('[data-decline]').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.decline;
        btn.disabled = true;
        try {
          await api.patch('/helpers/bookings/' + id + '/reject');
          toast('Booking declined', 'info');
          const card = document.getElementById('pending-' + id);
          if (card) card.remove();
        } catch {
          btn.disabled = false;
        }
      });
    });

  } catch {
    slot.innerHTML = '<div class="empty-state"><p>Could not load pending bookings.</p></div>';
  }
}

async function loadUpcoming() {
  const slot = document.getElementById('bookings-upcoming');
  if (!slot) return;

  slot.innerHTML = '<div class="text-center text-muted mt-2"><span class="spinner"></span></div>';

  try {
    const resp = await api.get('/helpers/bookings?page=0&size=20');
    const bookings = (resp?.content || [])
      .filter(b => b.status === 'ASSIGNED' || b.status === 'HELPER_EN_ROUTE')
      .sort((a, b) => new Date(a.scheduledAt || a.createdAt) - new Date(b.scheduledAt || b.createdAt));

    if (!bookings.length) {
      slot.innerHTML = `
        <div class="empty-state">
          <span class="empty-icon">📅</span>
          <p>No upcoming bookings.</p>
        </div>
      `;
      return;
    }

    slot.innerHTML = bookings.map(b => {
      const icon = SERVICE_ICON[b.serviceType] || '📦';
      const dt = b.scheduledAt
        ? new Date(b.scheduledAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
        : 'ASAP';
      const statusText = (b.status || '').replace(/_/g, ' ');
      const canStart = b.status === 'ASSIGNED';
      return `
        <div class="card mb-2">
          <div style="display:flex;align-items:center;gap:.6rem">
            <span style="font-size:1.5rem">${icon}</span>
            <div style="flex:1">
              <div style="font-weight:600;font-size:.9rem">${esc(b.serviceType)}</div>
              <div class="text-muted" style="font-size:.8rem">📍 ${esc(b.addressLine || '')}</div>
              <div class="text-muted" style="font-size:.78rem">🕐 ${esc(dt)}</div>
            </div>
            <span class="badge badge-assigned">${esc(statusText)}</span>
          </div>
          ${canStart ? `<div class="mt-1"><button class="btn btn-primary btn-sm btn-block" data-start="${b.id}">Start Journey 🚗</button></div>` : ''}
        </div>
      `;
    }).join('');

    slot.querySelectorAll('[data-start]').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.start;
        btn.disabled = true;
        try {
          await api.patch('/helpers/bookings/' + id + '/start-travel');
          toast('Journey started!', 'success');
          router.navigate('/helper/active/' + id);
        } catch {
          btn.disabled = false;
        }
      });
    });

  } catch {
    slot.innerHTML = '<div class="empty-state"><p>Could not load upcoming bookings.</p></div>';
  }
}
