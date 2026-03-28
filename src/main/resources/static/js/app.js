/**
 * app.js — Main entry point for the HomeCare SPA.
 *
 * Initialises the router, wires up global event listeners (logout button,
 * tab clicks, resize handler for sidebar/bottom-bar switching).
 */
import { auth }   from './auth.js';
import { router } from './router.js';
import { toast }  from './toast.js';

// ─── Tab click handler ──────────────────────────────────────────────
document.getElementById('tab-nav').addEventListener('click', (e) => {
  const btn = e.target.closest('.tab-btn');
  if (!btn) return;

  const tab = btn.dataset.tab;
  // Navigate to the tab's default route
  const defaults = {
    customer: '/customer/home',
    helper:   '/helper/home',
    admin:    '/admin/dashboard',
  };

  if (defaults[tab]) {
    router.navigate(defaults[tab]);
  }
});

// ─── Logout button ──────────────────────────────────────────────────
document.getElementById('btn-logout').addEventListener('click', () => {
  toast('Logged out successfully', 'success');
  auth.logout();
});

// ─── Responsive sidebar ↔ bottom-bar switch ─────────────────────────
let resizeTimer;
window.addEventListener('resize', () => {
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(() => {
    if (auth.isLoggedIn()) {
      if (window.innerWidth >= 768) {
        document.body.classList.add('has-sidebar');
      } else {
        document.body.classList.remove('has-sidebar');
      }
    }
  }, 100);
});

// ─── Boot ───────────────────────────────────────────────────────────
router.init();

