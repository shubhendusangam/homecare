/**
 * helper/earnings.js — Earnings summary with tabs and transaction history.
 *
 * - Total earnings: all time / this month / this week tabs
 * - Platform fee note: "15% platform fee deducted"
 * - Transaction list: each booking completed → +₹XXX
 * - Withdrawal button (stub)
 */
import { api }  from '../../api.js';
import { toast } from '../../toast.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

export async function render(container) {
  container.innerHTML = `
    <div class="section-header"><h2>Earnings</h2></div>
    <nav class="sub-nav">
      <a href="#/helper/home"     class="sub-nav-btn">Dashboard</a>
      <a href="#/helper/bookings" class="sub-nav-btn">Bookings</a>
      <a href="#/helper/earnings" class="sub-nav-btn active">Earnings</a>
      <a href="#/helper/profile"  class="sub-nav-btn">Profile</a>
    </nav>
    <div class="text-center mt-2"><span class="spinner"></span></div>
  `;

  try {
    const data = await api.get('/helpers/earnings');
    if (!data) throw new Error('No data');

    const totalEarnings = parseFloat(data.totalEarnings) || 0;
    const availableBalance = parseFloat(data.availableBalance) || 0;
    const txns = data.recentTransactions || [];
    const totalJobs = data.totalCompletedJobs || 0;

    container.innerHTML = `
      <div class="section-header"><h2>Earnings</h2></div>
      <nav class="sub-nav">
        <a href="#/helper/home"     class="sub-nav-btn">Dashboard</a>
        <a href="#/helper/bookings" class="sub-nav-btn">Bookings</a>
        <a href="#/helper/earnings" class="sub-nav-btn active">Earnings</a>
        <a href="#/helper/profile"  class="sub-nav-btn">Profile</a>
      </nav>

      <!-- Earnings Card -->
      <div class="wallet-balance-card">
        <div class="wb-label">Total Earnings</div>
        <div class="wb-amount">₹${totalEarnings.toLocaleString('en-IN')}</div>
        <div class="wb-held">Available: ₹${availableBalance.toLocaleString('en-IN')}</div>
        <div class="wb-held">${totalJobs} jobs completed</div>
      </div>

      <!-- Period Tabs -->
      <div class="toggle-row" id="earnings-tabs">
        <button class="toggle-btn active" data-tab="all">All Time</button>
        <button class="toggle-btn" data-tab="month">This Month</button>
        <button class="toggle-btn" data-tab="week">This Week</button>
      </div>

      <!-- Fee Note -->
      <div class="card mb-2" style="background:var(--status-pending-bg);border-color:var(--status-pending-fg)">
        <p style="font-size:.82rem;color:var(--status-pending-fg);margin:0">
          ℹ️ 15% platform fee is deducted from each booking
        </p>
      </div>

      <!-- Transactions -->
      <div class="card">
        <h3 style="font-size:.9rem;margin-bottom:.75rem">Transaction History</h3>
        <div id="txn-list">
          ${txns.length ? renderTransactions(txns, 'all') : '<p class="text-muted text-center">No transactions yet.</p>'}
        </div>
      </div>

      <!-- Withdrawal -->
      <div class="mt-2 text-center">
        <button class="btn btn-outline" id="btn-withdraw">🏦 Withdraw to Bank</button>
      </div>
    `;

    // Tab switching (client-side filter)
    const tabBtns = container.querySelectorAll('#earnings-tabs .toggle-btn');
    tabBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        tabBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        const period = btn.dataset.tab;
        const listEl = document.getElementById('txn-list');
        if (listEl) {
          const filtered = filterByPeriod(txns, period);
          listEl.innerHTML = filtered.length
            ? renderTransactions(filtered, period)
            : '<p class="text-muted text-center">No transactions in this period.</p>';
        }
      });
    });

    // Withdraw button
    const withdrawBtn = document.getElementById('btn-withdraw');
    if (withdrawBtn) {
      withdrawBtn.addEventListener('click', () => {
        showWithdrawModal(availableBalance);
      });
    }

  } catch {
    container.innerHTML = `
      <div class="section-header"><h2>Earnings</h2></div>
      <div class="empty-state">
        <span class="empty-icon">💰</span>
        <p>Could not load earnings data.</p>
      </div>
    `;
  }
}

function filterByPeriod(txns, period) {
  if (period === 'all') return txns;
  const now = new Date();
  return txns.filter(t => {
    const date = new Date(t.processedAt || t.createdAt);
    if (period === 'week') {
      const weekAgo = new Date(now);
      weekAgo.setDate(weekAgo.getDate() - 7);
      return date >= weekAgo;
    }
    if (period === 'month') {
      return date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear();
    }
    return true;
  });
}

function renderTransactions(txns) {
  return txns.map(t => {
    const amount = parseFloat(t.amount) || 0;
    const isCredit = t.type === 'CREDIT' || t.type === 'HELPER_PAYOUT' || amount > 0;
    const sign = isCredit ? '+' : '-';
    const cls = isCredit ? 'credit' : 'debit';
    const icon = isCredit ? '💰' : '📤';
    const dt = new Date(t.processedAt || t.createdAt).toLocaleDateString(undefined, { dateStyle: 'medium' });
    return `
      <div class="txn-item">
        <div class="txn-icon">${icon}</div>
        <div class="txn-info">
          <div class="txn-desc">${esc(t.description || t.type || 'Transaction')}</div>
          <div class="txn-date">${esc(dt)}</div>
        </div>
        <div class="txn-amount ${cls}">${sign}₹${Math.abs(amount).toLocaleString('en-IN')}</div>
      </div>
    `;
  }).join('');
}

function showWithdrawModal(maxAmount) {
  // Create modal overlay
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.id = 'withdraw-overlay';
  overlay.innerHTML = `
    <div class="modal-card">
      <div class="modal-header"><h3>🏦 Withdraw to Bank</h3><button class="modal-close" id="wd-close">&times;</button></div>
      <div class="form-group"><label>Amount (₹)</label>
        <input type="number" class="form-control" id="wd-amount" min="1" max="${maxAmount}" value="${Math.min(maxAmount, 1000)}" />
        <small class="text-muted">Available: ₹${maxAmount.toLocaleString('en-IN')}</small>
      </div>
      <div class="form-group"><label>Account Holder Name</label>
        <input type="text" class="form-control" id="wd-name" placeholder="Name as on bank account" />
      </div>
      <div class="form-group"><label>Account Number</label>
        <input type="text" class="form-control" id="wd-acct" placeholder="Bank account number" />
      </div>
      <div class="form-group"><label>IFSC Code</label>
        <input type="text" class="form-control" id="wd-ifsc" placeholder="e.g. SBIN0001234" maxlength="11" />
      </div>
      <button class="btn btn-primary btn-block" id="wd-submit">Request Withdrawal</button>
    </div>`;
  document.body.appendChild(overlay);

  document.getElementById('wd-close').addEventListener('click', () => overlay.remove());
  overlay.addEventListener('click', e => { if (e.target === overlay) overlay.remove(); });

  document.getElementById('wd-submit').addEventListener('click', async () => {
    const amount = parseFloat(document.getElementById('wd-amount').value);
    const name = document.getElementById('wd-name').value.trim();
    const acct = document.getElementById('wd-acct').value.trim();
    const ifsc = document.getElementById('wd-ifsc').value.trim();
    if (!amount || amount < 1 || amount > maxAmount) { toast('Enter a valid amount', 'warn'); return; }
    if (!name || !acct || !ifsc) { toast('Fill in all bank details', 'warn'); return; }
    const btn = document.getElementById('wd-submit');
    btn.disabled = true; btn.textContent = 'Processing…';
    try {
      await api.post('/helpers/withdraw', { amount, accountHolderName: name, accountNumber: acct, ifscCode: ifsc });
      toast('Withdrawal requested! It will be processed within 2-3 business days.', 'success');
      overlay.remove();
    } catch {
      // If endpoint doesn't exist yet, show a graceful message
      toast('Withdrawal feature will be available soon', 'info');
      overlay.remove();
    }
  });
}

