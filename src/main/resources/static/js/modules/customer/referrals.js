/**
 * customer/referrals.js — Refer & Earn: show code, stats, history.
 */
import { api }  from '../../api.js';
import { toast } from '../../toast.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

export async function render(container) {
  container.innerHTML = `
    <div class="section-header"><h2>🎁 Refer & Earn</h2></div>
    <nav class="sub-nav">
      <a href="#/customer/home" class="sub-nav-btn">Home</a>
      <a href="#/customer/referrals" class="sub-nav-btn active">Referrals</a>
      <a href="#/customer/wallet" class="sub-nav-btn">Wallet</a>
    </nav>

    <!-- Referral Code Card -->
    <div class="referral-card" id="code-card">
      <div class="rc-title">Share your code & earn ₹100!</div>
      <div class="rc-sub">Your friend gets ₹50 on signup. You get ₹100 when they complete their first booking.</div>
      <div class="referral-code-box">
        <span id="ref-code"><span class="spinner" style="width:18px;height:18px"></span></span>
        <button class="copy-btn" id="btn-copy" aria-label="Copy referral code">📋 Copy</button>
      </div>
    </div>

    <!-- Stats -->
    <div class="stats-grid" id="ref-stats">
      <div class="stat-card"><div class="stat-value" id="stat-total">—</div><div class="stat-label">Referrals Made</div></div>
      <div class="stat-card"><div class="stat-value" id="stat-success">—</div><div class="stat-label">Successful</div></div>
      <div class="stat-card"><div class="stat-value" id="stat-earned">—</div><div class="stat-label">Credits Earned</div></div>
      <div class="stat-card"><div class="stat-value" id="stat-rate">—</div><div class="stat-label">Conversion %</div></div>
    </div>

    <!-- How it works -->
    <div class="card mb-2">
      <h3 style="font-size:.95rem;margin-bottom:.5rem">How it works</h3>
      <div class="timeline">
        <div class="timeline-item active"><div class="tl-label">1. Share your code</div><div class="tl-time">Send to friends via WhatsApp, SMS, or email</div></div>
        <div class="timeline-item active"><div class="tl-label">2. Friend signs up</div><div class="tl-time">They enter your code during registration → get ₹50 credit</div></div>
        <div class="timeline-item active"><div class="tl-label">3. First booking done</div><div class="tl-time">When they complete their first booking → you get ₹100 credit</div></div>
      </div>
    </div>

    <!-- History -->
    <div class="card">
      <h3 style="font-size:.95rem;margin-bottom:.75rem">Referral History</h3>
      <div id="ref-history"><div class="text-center text-muted"><span class="spinner"></span></div></div>
    </div>
  `;

  // Load code
  try {
    const code = await api.get('/referrals/my-code');
    document.getElementById('ref-code').textContent = code.code || '—';
  } catch {
    document.getElementById('ref-code').textContent = 'Error';
  }

  // Copy button
  document.getElementById('btn-copy').addEventListener('click', () => {
    const code = document.getElementById('ref-code').textContent;
    navigator.clipboard.writeText(code).then(() => toast('Code copied!', 'success')).catch(() => toast('Copy failed', 'error'));
  });

  // Load stats
  try {
    const stats = await api.get('/referrals/stats');
    document.getElementById('stat-total').textContent = stats.totalReferrals || 0;
    document.getElementById('stat-success').textContent = stats.successfulReferrals || 0;
    document.getElementById('stat-earned').textContent = '₹' + (stats.totalCreditsEarned || 0);
    const rate = stats.totalReferrals > 0 ? Math.round((stats.successfulReferrals / stats.totalReferrals) * 100) : 0;
    document.getElementById('stat-rate').textContent = rate + '%';
  } catch {}

  // Load history
  try {
    const resp = await api.get('/referrals/history?page=0&size=20');
    const items = resp.content || [];
    const el = document.getElementById('ref-history');
    if (!items.length) { el.innerHTML = '<p class="text-muted text-center">No referrals yet. Share your code!</p>'; return; }
    el.innerHTML = items.map(r => {
      const badge = r.status === 'CREDIT_ISSUED' ? 'badge-completed' : r.status === 'EXPIRED' ? 'badge-cancelled' : 'badge-pending';
      return `<div class="txn-item">
        <div class="txn-icon">👤</div>
        <div class="txn-info"><div class="txn-desc">${esc(r.refereeName || 'User')}</div><div class="txn-date">${r.createdAt ? new Date(r.createdAt).toLocaleDateString() : ''}</div></div>
        <span class="badge ${badge}">${esc(r.status)}</span>
      </div>`;
    }).join('');
  } catch { document.getElementById('ref-history').innerHTML = '<p class="text-muted text-center">Could not load history</p>'; }
}

