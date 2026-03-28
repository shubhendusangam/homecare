/**
 * admin/helpers.js — Admin helper management placeholder.
 */
export function render(container) {
  container.innerHTML = `
    <div class="section-header"><h2>Helpers</h2></div>
    <nav class="sub-nav">
      <a href="#/admin/dashboard" class="sub-nav-btn">Overview</a>
      <a href="#/admin/bookings"  class="sub-nav-btn">Bookings</a>
      <a href="#/admin/users"     class="sub-nav-btn">Users</a>
      <a href="#/admin/helpers"   class="sub-nav-btn active">Helpers</a>
      <a href="#/admin/payments"  class="sub-nav-btn">Payments</a>
      <a href="#/admin/config"    class="sub-nav-btn">Config</a>
    </nav>
    <div class="empty-state">
      <span class="empty-icon">🔧</span>
      <p>Helper management coming soon.</p>
    </div>
  `;
}

