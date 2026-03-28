/**
 * auth.js — JWT token management and login/logout helpers (ES module).
 */

const TOKEN_KEY = 'hc_token';
const ROLE_KEY  = 'hc_role';
const USER_KEY  = 'hc_user';

export const auth = {
  /**
   * Persist login data.
   * @param {string} token   JWT access token
   * @param {string} role    CUSTOMER | HELPER | ADMIN
   * @param {object} user    { userId, name, email, … }
   */
  save(token, role, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(ROLE_KEY, role);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },

  token()      { return localStorage.getItem(TOKEN_KEY); },
  role()       { return localStorage.getItem(ROLE_KEY); },
  user()       { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); },
  isLoggedIn() { return !!this.token(); },

  /**
   * Clear stored credentials and redirect to login.
   * @param {string} [message]  optional toast message (e.g. "Session expired")
   */
  logout(message) {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(USER_KEY);
    // Import dynamically to avoid circular dependency
    if (message) {
      import('./toast.js').then(({ toast }) => toast(message, 'warn'));
    }
    window.location.hash = '#/login';
  },

  /**
   * Check if the stored JWT is expired by decoding the payload.
   * @returns {boolean}
   */
  isTokenExpired() {
    const token = this.token();
    if (!token) return true;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
};
