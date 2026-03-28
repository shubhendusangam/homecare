/**
 * helper/profile.js — Helper profile placeholder.
 */
import { auth } from '../../auth.js';

export function render(container) {
  const user = auth.user();
  container.innerHTML = `
    <div class="section-header"><h2>My Profile</h2></div>
    <div class="card">
      <p><strong>Name:</strong> <span id="profile-name"></span></p>
      <p><strong>Email:</strong> <span id="profile-email"></span></p>
      <p class="text-muted mt-2">Profile editing coming soon.</p>
    </div>
  `;
  document.getElementById('profile-name').textContent = user?.name || '—';
  document.getElementById('profile-email').textContent = user?.email || '—';
}

