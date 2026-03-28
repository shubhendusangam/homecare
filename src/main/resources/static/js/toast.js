/**
 * toast.js — Toast notification system.
 * Auto-dismiss after 3s, dismissible via click.
 */

const ICONS = {
  success: '✅',
  error:   '❌',
  info:    'ℹ️',
  warn:    '⚠️',
};

/**
 * Show a toast notification.
 * @param {string} message
 * @param {'success'|'error'|'info'|'warn'} type
 * @param {number} duration  auto-dismiss in ms (default 3000)
 */
export function toast(message, type = 'info', duration = 3000) {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.innerHTML = `
    <span>${ICONS[type] || ''}</span>
    <span>${escapeHtml(message)}</span>
    <button class="toast-close" aria-label="Dismiss">&times;</button>
  `;

  let dismissed = false;
  const dismiss = () => {
    if (dismissed) return;
    dismissed = true;
    el.classList.add('toast-out');
    el.addEventListener('animationend', () => el.remove());
  };

  el.querySelector('.toast-close').addEventListener('click', (e) => {
    e.stopPropagation();
    dismiss();
  });
  el.addEventListener('click', dismiss);

  container.appendChild(el);

  if (duration > 0) {
    setTimeout(dismiss, duration);
  }
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

