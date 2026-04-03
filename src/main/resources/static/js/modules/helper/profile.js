/**
 * helper/profile.js — Full helper profile: edit info, skills, availability schedule.
 */
import { auth } from '../../auth.js';
import { api }  from '../../api.js';
import { toast } from '../../toast.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
const SKILLS = ['CLEANING', 'COOKING', 'BABYSITTING', 'ELDERLY_HELP'];
const SKILL_ICONS = { CLEANING: '🧹', COOKING: '🍳', BABYSITTING: '👶', ELDERLY_HELP: '🧓' };
const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
const HOURS = Array.from({ length: 13 }, (_, i) => i + 7); // 7 AM to 7 PM

export async function render(container) {
  const user = auth.user();
  container.innerHTML = `
    <div class="section-header"><h2>My Profile</h2></div>
    <nav class="sub-nav">
      <a href="#/helper/home" class="sub-nav-btn">Dashboard</a>
      <a href="#/helper/bookings" class="sub-nav-btn">Bookings</a>
      <a href="#/helper/earnings" class="sub-nav-btn">Earnings</a>
      <a href="#/helper/profile" class="sub-nav-btn active">Profile</a>
    </nav>

    <div class="profile-avatar">🧑‍🔧</div>

    <!-- Basic Info -->
    <div class="card mb-2">
      <div class="profile-section">
        <h3>Personal Info</h3>
        <p><strong>Name:</strong> <span id="hp-name"></span></p>
        <p><strong>Email:</strong> <span id="hp-email"></span></p>
        <p><strong>Rating:</strong> <span id="hp-rating"></span></p>
        <p><strong>Jobs Completed:</strong> <span id="hp-jobs"></span></p>
      </div>
    </div>

    <!-- Skills -->
    <div class="card mb-2">
      <div class="profile-section">
        <h3>Skills</h3>
        <div class="skill-tags" id="skill-tags">
          ${SKILLS.map(s => `<span class="skill-tag" data-s="${s}">${SKILL_ICONS[s]} ${s.replace(/_/g, ' ')}</span>`).join('')}
        </div>
        <button class="btn btn-primary btn-sm mt-1" id="btn-save-skills">Save Skills</button>
      </div>
    </div>

    <!-- Weekly Availability -->
    <div class="card mb-2">
      <div class="profile-section">
        <h3>Weekly Availability</h3>
        <p class="text-muted" style="font-size:.78rem;margin-bottom:.5rem">Click cells to toggle your available hours</p>
        <div style="overflow-x:auto">
          <table class="admin-table" id="avail-table" style="min-width:500px">
            <thead><tr><th>Hour</th>${DAYS.map(d => `<th>${d}</th>`).join('')}</tr></thead>
            <tbody>
              ${HOURS.map(h => `<tr>
                <td style="font-size:.75rem;font-weight:600">${h}:00</td>
                ${DAYS.map(d => `<td><div class="avail-slot" data-day="${d}" data-hour="${h}" title="${d} ${h}:00">&nbsp;</div></td>`).join('')}
              </tr>`).join('')}
            </tbody>
          </table>
        </div>
        <button class="btn btn-primary btn-sm mt-1" id="btn-save-avail">Save Availability</button>
      </div>
    </div>

    <!-- Unavailable Dates -->
    <div class="card mb-2">
      <div class="profile-section">
        <h3>Unavailable Dates</h3>
        <div class="form-group">
          <label for="unavail-date">Mark a date as unavailable</label>
          <div style="display:flex;gap:.5rem">
            <input type="date" id="unavail-date" class="form-control" style="max-width:200px" />
            <button class="btn btn-outline btn-sm" id="btn-add-unavail">Add</button>
          </div>
        </div>
        <div id="unavail-list" class="text-muted" style="font-size:.82rem"></div>
      </div>
    </div>

    <!-- Quick Links -->
    <div class="card">
      <div class="profile-section" style="margin-bottom:0">
        <h3>Quick Links</h3>
        <div class="profile-links">
          <a href="#/helper/notifications" class="profile-link"><span class="pl-icon">🔔</span> Notifications <span class="pl-arrow">›</span></a>
          <a href="#/helper/earnings" class="profile-link"><span class="pl-icon">💰</span> Earnings & Withdrawal <span class="pl-arrow">›</span></a>
        </div>
      </div>
    </div>
  `;

  document.getElementById('hp-name').textContent = user?.name || '—';
  document.getElementById('hp-email').textContent = user?.email || '—';

  // Load profile
  let currentSkills = [];
  try {
    const profile = await api.get('/helpers/me');
    if (profile) {
      document.getElementById('hp-rating').textContent = '⭐ ' + (profile.rating ? profile.rating.toFixed(1) : '0.0');
      document.getElementById('hp-jobs').textContent = profile.totalJobsCompleted || 0;
      currentSkills = profile.skills || [];
      currentSkills.forEach(s => {
        const tag = document.querySelector(`#skill-tags [data-s="${s}"]`);
        if (tag) tag.classList.add('selected');
      });
    }
  } catch {}

  // Skill tag toggles
  document.querySelectorAll('#skill-tags .skill-tag').forEach(tag => {
    tag.addEventListener('click', () => {
      tag.classList.toggle('selected');
    });
  });

  document.getElementById('btn-save-skills').addEventListener('click', async () => {
    const selected = [...document.querySelectorAll('#skill-tags .skill-tag.selected')].map(t => t.dataset.s);
    if (!selected.length) { toast('Select at least one skill', 'warn'); return; }
    const btn = document.getElementById('btn-save-skills');
    btn.disabled = true;
    try {
      await api.patch('/helpers/me', { skills: selected });
      toast('Skills updated!', 'success');
    } catch {} finally { btn.disabled = false; }
  });

  // Availability grid clicks
  document.querySelectorAll('#avail-table .avail-slot').forEach(slot => {
    slot.addEventListener('click', () => slot.classList.toggle('active'));
  });

  // Load existing availability slots
  try {
    const slots = await api.get('/helpers/me/availability');
    if (slots && slots.length) {
      slots.forEach(s => {
        const el = document.querySelector(`[data-day="${s.dayOfWeek}"][data-hour="${s.startHour}"]`);
        if (el) el.classList.add('active');
      });
    }
  } catch {}

  document.getElementById('btn-save-avail').addEventListener('click', async () => {
    const activeSlots = [...document.querySelectorAll('#avail-table .avail-slot.active')].map(s => ({
      dayOfWeek: s.dataset.day,
      startHour: parseInt(s.dataset.hour),
      endHour: parseInt(s.dataset.hour) + 1
    }));
    const btn = document.getElementById('btn-save-avail');
    btn.disabled = true;
    try {
      await api.put('/helpers/me/availability', activeSlots);
      toast('Availability saved!', 'success');
    } catch {} finally { btn.disabled = false; }
  });

  // Unavailable dates
  document.getElementById('btn-add-unavail').addEventListener('click', async () => {
    const date = document.getElementById('unavail-date').value;
    if (!date) { toast('Select a date', 'warn'); return; }
    try {
      await api.post('/helpers/me/unavailable-dates', { date });
      toast('Date marked as unavailable', 'success');
      loadUnavailDates();
    } catch {}
  });
  loadUnavailDates();
}

async function loadUnavailDates() {
  const el = document.getElementById('unavail-list');
  try {
    const dates = await api.get('/helpers/me/unavailable-dates');
    if (!dates || !dates.length) { el.textContent = 'No unavailable dates set.'; return; }
    el.innerHTML = dates.map(d => `<span class="chip" style="margin:.2rem">${d.date || d} <button class="toast-close" data-del="${d.id || d.date}" style="font-size:.7rem" aria-label="Remove date">×</button></span>`).join('');
    el.querySelectorAll('[data-del]').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        e.stopPropagation();
        try { await api.delete('/helpers/me/unavailable-dates/' + btn.dataset.del); loadUnavailDates(); } catch {}
      });
    });
  } catch { el.textContent = 'Could not load unavailable dates.'; }
}
