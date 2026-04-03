/**
 * customer/profile.js — Full customer profile with edit, address, links.
 */
import { auth } from '../../auth.js';
import { api }  from '../../api.js';
import { toast } from '../../toast.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

export async function render(container) {
  const user = auth.user();
  container.innerHTML = `
    <div class="section-header"><h2>My Profile</h2></div>
    <nav class="sub-nav">
      <a href="#/customer/home" class="sub-nav-btn">Home</a>
      <a href="#/customer/history" class="sub-nav-btn">Bookings</a>
      <a href="#/customer/wallet" class="sub-nav-btn">Wallet</a>
      <a href="#/customer/profile" class="sub-nav-btn active">Profile</a>
    </nav>

    <div class="profile-avatar">👤</div>

    <!-- Profile Form -->
    <div class="card mb-2">
      <div class="profile-section">
        <h3>Personal Info</h3>
        <div class="form-group">
          <label for="p-name">Name</label>
          <input type="text" id="p-name" class="form-control" value="" />
        </div>
        <div class="form-group">
          <label for="p-email">Email</label>
          <input type="email" id="p-email" class="form-control" value="" disabled />
        </div>
        <div class="form-group">
          <label for="p-phone">Phone</label>
          <input type="tel" id="p-phone" class="form-control" value="" />
        </div>
      </div>
      <div class="profile-section">
        <h3>Saved Address</h3>
        <div class="form-group">
          <label for="p-address">Address</label>
          <input type="text" id="p-address" class="form-control" placeholder="Enter your address" />
        </div>
      </div>
      <button class="btn btn-primary btn-block" id="btn-save-profile">Save Changes</button>
    </div>

    <!-- Quick Links -->
    <div class="card mb-2">
      <div class="profile-section" style="margin-bottom:0">
        <h3>Quick Links</h3>
        <div class="profile-links">
          <a href="#/customer/referrals" class="profile-link"><span class="pl-icon">🎁</span> Refer & Earn <span class="pl-arrow">›</span></a>
          <a href="#/customer/favourites" class="profile-link"><span class="pl-icon">❤️</span> Favourite Helpers <span class="pl-arrow">›</span></a>
          <a href="#/customer/subscriptions" class="profile-link"><span class="pl-icon">📋</span> Subscriptions <span class="pl-arrow">›</span></a>
          <a href="#/customer/disputes" class="profile-link"><span class="pl-icon">⚖️</span> My Disputes <span class="pl-arrow">›</span></a>
          <a href="#/customer/notifications" class="profile-link"><span class="pl-icon">🔔</span> Notifications <span class="pl-arrow">›</span></a>
        </div>
      </div>
    </div>
  `;

  // Populate fields
  document.getElementById('p-name').value = user?.name || '';
  document.getElementById('p-email').value = user?.email || '';

  // Load full profile from API
  try {
    const profile = await api.get('/customers/me');
    if (profile) {
      document.getElementById('p-phone').value = profile.phone || '';
      document.getElementById('p-address').value = profile.addressLine || '';
    }
  } catch { /* use defaults */ }

  // Save
  document.getElementById('btn-save-profile').addEventListener('click', async () => {
    const btn = document.getElementById('btn-save-profile');
    btn.disabled = true; btn.textContent = 'Saving…';
    try {
      await api.patch('/customers/me', {
        name: document.getElementById('p-name').value.trim(),
        phone: document.getElementById('p-phone').value.trim(),
        addressLine: document.getElementById('p-address').value.trim(),
      });
      toast('Profile updated!', 'success');
      // Update local auth user
      const u = auth.user();
      u.name = document.getElementById('p-name').value.trim();
      localStorage.setItem('hc_user', JSON.stringify(u));
    } catch { /* api toasts */ }
    finally { btn.disabled = false; btn.textContent = 'Save Changes'; }
  });
}
