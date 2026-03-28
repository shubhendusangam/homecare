/**
 * router.js — Hash-based SPA router (ES module).
 *
 * Routes are defined as path patterns with optional :param segments.
 * Each route maps to a lazy-loaded module that exports { render(container, params) }.
 */
import { auth } from './auth.js';

// ─── Route definitions ──────────────────────────────────────────────
const routes = [
  // Login
  { path: '/login',                   loader: () => import('./modules/login.js'),              screen: 'screen-login',             tab: null },

  // Customer
  { path: '/customer/home',           loader: () => import('./modules/customer/home.js'),      screen: 'screen-customer-home',     tab: 'customer' },
  { path: '/customer/book',           loader: () => import('./modules/customer/book.js'),      screen: 'screen-customer-book',     tab: 'customer' },
  { path: '/customer/tracking/:id',   loader: () => import('./modules/customer/tracking.js'),  screen: 'screen-customer-tracking', tab: 'customer' },
  { path: '/customer/history',        loader: () => import('./modules/customer/history.js'),   screen: 'screen-customer-history',  tab: 'customer' },
  { path: '/customer/wallet',         loader: () => import('./modules/customer/wallet.js'),    screen: 'screen-customer-wallet',   tab: 'customer' },
  { path: '/customer/profile',        loader: () => import('./modules/customer/profile.js'),   screen: 'screen-customer-profile',  tab: 'customer' },

  // Helper
  { path: '/helper/home',             loader: () => import('./modules/helper/home.js'),        screen: 'screen-helper-home',       tab: 'helper' },
  { path: '/helper/bookings',         loader: () => import('./modules/helper/bookings.js'),    screen: 'screen-helper-bookings',   tab: 'helper' },
  { path: '/helper/active/:id',       loader: () => import('./modules/helper/active.js'),      screen: 'screen-helper-active',     tab: 'helper' },
  { path: '/helper/earnings',         loader: () => import('./modules/helper/earnings.js'),    screen: 'screen-helper-earnings',   tab: 'helper' },
  { path: '/helper/profile',          loader: () => import('./modules/helper/profile.js'),     screen: 'screen-helper-profile',    tab: 'helper' },

  // Admin
  { path: '/admin/dashboard',         loader: () => import('./modules/admin/dashboard.js'),    screen: 'screen-admin-dashboard',   tab: 'admin' },
  { path: '/admin/bookings',          loader: () => import('./modules/admin/bookings.js'),     screen: 'screen-admin-bookings',    tab: 'admin' },
  { path: '/admin/users',             loader: () => import('./modules/admin/users.js'),        screen: 'screen-admin-users',       tab: 'admin' },
  { path: '/admin/helpers',           loader: () => import('./modules/admin/helpers.js'),      screen: 'screen-admin-helpers',     tab: 'admin' },
  { path: '/admin/payments',          loader: () => import('./modules/admin/payments.js'),     screen: 'screen-admin-payments',    tab: 'admin' },
  { path: '/admin/config',            loader: () => import('./modules/admin/config.js'),       screen: 'screen-admin-config',      tab: 'admin' },
];

// Build regex matchers once
const compiled = routes.map(r => {
  const paramNames = [];
  const pattern = r.path.replace(/:([^/]+)/g, (_, name) => {
    paramNames.push(name);
    return '([^/]+)';
  });
  return { ...r, regex: new RegExp('^' + pattern + '$'), paramNames };
});

// ─── Router ─────────────────────────────────────────────────────────
/** @type {Function[]} cleanup callbacks run before each navigation */
const _teardowns = [];

export const router = {
  /** Navigate to a hash path (without the #) */
  navigate(path) {
    window.location.hash = '#' + path;
  },

  /** Register a cleanup function that runs on every route change. Returns an unsubscribe fn. */
  onTeardown(fn) {
    _teardowns.push(fn);
    return () => {
      const idx = _teardowns.indexOf(fn);
      if (idx >= 0) _teardowns.splice(idx, 1);
    };
  },

  /** Initialise: listen to hashchange and resolve the initial route. */
  init() {
    window.addEventListener('hashchange', () => this._resolve());
    this._resolve();
  },

  /** Resolve the current hash to a route and render it. */
  async _resolve() {
    // Run teardown hooks from previous screen
    while (_teardowns.length) { try { _teardowns.pop()(); } catch {} }

    const rawHash = window.location.hash.slice(1) || '/login';

    // Separate path from query string (query is part of the hash fragment)
    const qIndex = rawHash.indexOf('?');
    const path   = qIndex >= 0 ? rawHash.slice(0, qIndex) : rawHash;
    const query  = qIndex >= 0 ? this._parseQuery(rawHash.slice(qIndex + 1)) : {};

    // Auth guard — check token expiry
    if (auth.isLoggedIn() && auth.isTokenExpired()) {
      auth.logout('Session expired — please log in again');
      return;
    }

    // Not logged in → force login screen
    if (!auth.isLoggedIn() && path !== '/login') {
      this.navigate('/login');
      return;
    }

    // Logged in but on /login → redirect to role default
    if (auth.isLoggedIn() && path === '/login') {
      this.navigate(this._defaultRoute());
      return;
    }

    // Match route against path (without query string)
    let matched = null;
    let params = {};
    for (const route of compiled) {
      const m = path.match(route.regex);
      if (m) {
        matched = route;
        route.paramNames.forEach((name, i) => { params[name] = m[i + 1]; });
        break;
      }
    }

    // Merge query params into params (route params take precedence)
    params = { ...query, ...params };

    if (!matched) {
      // 404 — redirect to default
      if (auth.isLoggedIn()) {
        this.navigate(this._defaultRoute());
      } else {
        this.navigate('/login');
      }
      return;
    }

    // Update UI chrome (top bar, tabs, body classes)
    this._updateChrome(matched);

    // Hide all screens, show the target one
    this._showScreen(matched.screen, matched.tab);

    // Lazy-load and render the module
    try {
      const mod = await matched.loader();
      const container = document.getElementById(matched.screen);
      if (mod && typeof mod.render === 'function') {
        await mod.render(container, params);
      }
    } catch (err) {
      console.error('Router: failed to load module', err);
      // Don't toast for dynamic import failures of placeholder modules
    }
  },

  /** Return the default hash path for the current user's role. */
  _defaultRoute() {
    const role = auth.role();
    switch (role) {
      case 'CUSTOMER': return '/customer/home';
      case 'HELPER':   return '/helper/home';
      case 'ADMIN':    return '/admin/dashboard';
      default:         return '/login';
    }
  },

  /** Show/hide top-bar, tab-nav, set body classes. */
  _updateChrome(route) {
    const topBar = document.getElementById('top-bar');
    const tabNav = document.getElementById('tab-nav');
    const loggedIn = auth.isLoggedIn();

    // Top bar
    if (loggedIn) {
      topBar.classList.remove('hidden');
      const user = auth.user();
      document.getElementById('user-greeting').textContent = user ? `Hi, ${user.name}` : '';
    } else {
      topBar.classList.add('hidden');
    }

    // Tab nav visibility & role-based tabs
    if (loggedIn && route.tab) {
      tabNav.classList.remove('hidden');
      document.body.classList.add('has-tabs');

      const role = auth.role();
      const btns = tabNav.querySelectorAll('.tab-btn');
      btns.forEach(btn => {
        const tab = btn.dataset.tab;
        // Admin sees all tabs; others see only their own
        if (role === 'ADMIN') {
          btn.classList.remove('hidden');
        } else {
          btn.classList.toggle('hidden', tab !== role.toLowerCase());
        }
        // Set active state
        btn.classList.toggle('active', tab === route.tab);
      });

      // Desktop sidebar
      if (window.innerWidth >= 768) {
        document.body.classList.add('has-sidebar');
      } else {
        document.body.classList.remove('has-sidebar');
      }
    } else {
      tabNav.classList.add('hidden');
      document.body.classList.remove('has-tabs', 'has-sidebar');
    }
  },

  /** Hide all screens & tab containers, then show the matching one. */
  _showScreen(screenId, tabName) {
    // Hide all tabs
    document.querySelectorAll('.tab').forEach(t => t.classList.add('hidden'));
    // Hide login screen
    document.getElementById('screen-login').classList.add('hidden');
    // Hide all screens within tabs
    document.querySelectorAll('.tab .screen').forEach(s => s.classList.add('hidden'));

    if (tabName) {
      // Show the tab container
      const tabEl = document.getElementById('tab-' + tabName);
      if (tabEl) tabEl.classList.remove('hidden');
      // Show the specific screen
      const screenEl = document.getElementById(screenId);
      if (screenEl) screenEl.classList.remove('hidden');
    } else {
      // Login screen (no tab)
      const screenEl = document.getElementById(screenId);
      if (screenEl) screenEl.classList.remove('hidden');
    }
  },

  /** Parse a query string like "foo=bar&baz=1" into { foo: 'bar', baz: '1' }. */
  _parseQuery(qs) {
    const params = {};
    if (!qs) return params;
    qs.split('&').forEach(pair => {
      const [key, ...rest] = pair.split('=');
      if (key) params[decodeURIComponent(key)] = decodeURIComponent(rest.join('='));
    });
    return params;
  },
};
