/**
 * customer/favourites.js — Favourite helpers list.
 */
import { api } from '../../api.js';
import { toast } from '../../toast.js';
import { router } from '../../router.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

export async function render(container) {
  container.innerHTML = `
    <div class="section-header"><h2>❤️ My Favourite Helpers</h2></div>
    <nav class="sub-nav">
      <a href="#/customer/home" class="sub-nav-btn">Home</a>
      <a href="#/customer/favourites" class="sub-nav-btn active">Favourites</a>
      <a href="#/customer/profile" class="sub-nav-btn">Profile</a>
    </nav>
    <div id="fav-list"><div class="text-center mt-2"><span class="spinner"></span></div></div>
  `;
  loadFavs();
}

async function loadFavs() {
  const el = document.getElementById('fav-list');
  try {
    const favs = await api.get('/favourites');
    if (!favs || !favs.length) {
      el.innerHTML = `<div class="empty-state"><span class="empty-icon">❤️</span><p>No favourite helpers yet.</p><p class="text-muted" style="font-size:.82rem">After a booking, tap the heart on the helper's card to save them.</p></div>`;
      return;
    }
    el.innerHTML = favs.map(f => `
      <div class="card mb-2">
        <div style="display:flex;align-items:center;gap:.75rem">
          <div class="profile-avatar" style="width:48px;height:48px;font-size:1.3rem;margin:0">🧑‍🔧</div>
          <div style="flex:1">
            <div style="font-weight:600">${esc(f.helperName || 'Helper')}</div>
            ${f.nickname ? `<div class="text-muted" style="font-size:.78rem">aka "${esc(f.nickname)}"</div>` : ''}
            <div class="text-muted" style="font-size:.78rem">⭐ ${f.rating || '—'} · ${f.totalBookingsTogether || 0} bookings together</div>
          </div>
          <div style="display:flex;gap:.4rem;align-items:center">
            <button class="btn btn-primary btn-sm" data-book="${f.helperId}">Book</button>
            <button class="fav-btn active" data-rm="${f.helperId}" aria-label="Remove favourite">❤️</button>
          </div>
        </div>
      </div>`).join('');
    el.querySelectorAll('[data-book]').forEach(btn => {
      btn.addEventListener('click', () => router.navigate('/customer/book?requestedHelper=' + btn.dataset.book));
    });
    el.querySelectorAll('[data-rm]').forEach(btn => {
      btn.addEventListener('click', async () => {
        if (!confirm('Remove from favourites?')) return;
        try { await api.delete('/favourites/' + btn.dataset.rm); toast('Removed', 'info'); loadFavs(); } catch {}
      });
    });
  } catch { el.innerHTML = '<div class="empty-state"><p>Could not load favourites. <button class="btn btn-outline btn-sm" onclick="location.reload()">Retry</button></p></div>'; }
}

