/**
 * customer/wallet.js — Wallet balance, top-up (Razorpay mock), transaction history.
 */
import { api }  from '../../api.js';
import { toast } from '../../toast.js';

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
function fmtCur(n) { return '₹' + Number(n || 0).toFixed(2); }
function fmtDate(iso) { return iso ? new Date(iso).toLocaleString(undefined, { dateStyle: 'short', timeStyle: 'short' }) : '—'; }
function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

const TXN_ICON = {
  CREDIT_TOPUP:       '💰',
  DEBIT_BOOKING:      '📤',
  DEBIT_SUBSCRIPTION: '📋',
  REFUND:             '↩️',
  CREDIT_EARNING:     '💵',
  CREDIT_REFERRAL:    '🎁',
};

let txnPage = 0;
const TXN_SIZE = 10;

export async function render(container) {
  txnPage = 0;
  container.innerHTML = `
    <div class="section-header"><h2>My Wallet</h2></div>
    <nav class="sub-nav">
      <a href="#/customer/home"    class="sub-nav-btn">Home</a>
      <a href="#/customer/history" class="sub-nav-btn">My Bookings</a>
      <a href="#/customer/wallet"  class="sub-nav-btn active">Wallet</a>
    </nav>

    <!-- Balance card (skeleton) -->
    <div id="balance-card">
      <div class="wallet-balance-card">
        <div class="wb-label">Available Balance</div>
        <div class="wb-amount" id="wb-amount">…</div>
        <div class="wb-held" id="wb-held"></div>
      </div>
    </div>

    <!-- Add money -->
    <div class="card mb-2">
      <h3 style="font-size:.95rem;margin-bottom:.5rem">Add Money</h3>
      <div class="amount-btns" id="amt-btns">
        <button class="amount-btn" data-a="100">₹100</button>
        <button class="amount-btn" data-a="500">₹500</button>
        <button class="amount-btn active" data-a="1000">₹1000</button>
        <button class="amount-btn" data-a="2000">₹2000</button>
      </div>
      <div style="display:flex;gap:.5rem;align-items:center">
        <input type="number" class="form-control" id="custom-amount" placeholder="Custom ₹" min="1" style="flex:1" />
        <button class="btn btn-primary" id="btn-topup">Add</button>
      </div>
    </div>

    <!-- Transaction history -->
    <div class="card">
      <h3 style="font-size:.95rem;margin-bottom:.75rem">Transaction History</h3>
      <div id="txn-list">
        <div class="skeleton skeleton-line"></div>
        <div class="skeleton skeleton-line medium"></div>
        <div class="skeleton skeleton-line short"></div>
      </div>
      <div id="txn-pagination"></div>
    </div>
  `;

  // Load balance + transactions in parallel
  loadBalance();
  loadTransactions();

  // Amount button selection
  let selectedAmount = 1000;
  const amtBtns = document.getElementById('amt-btns');
  const customInput = document.getElementById('custom-amount');

  amtBtns.addEventListener('click', e => {
    const btn = e.target.closest('.amount-btn');
    if (!btn) return;
    selectedAmount = parseInt(btn.dataset.a, 10);
    amtBtns.querySelectorAll('.amount-btn').forEach(b => b.classList.toggle('active', b === btn));
    customInput.value = '';
  });

  customInput.addEventListener('input', () => {
    const v = parseInt(customInput.value, 10);
    if (v > 0) {
      selectedAmount = v;
      amtBtns.querySelectorAll('.amount-btn').forEach(b => b.classList.remove('active'));
    }
  });

  // Top-up button
  document.getElementById('btn-topup').addEventListener('click', async () => {
    const amount = parseInt(customInput.value, 10) || selectedAmount;
    if (!amount || amount < 1) { toast('Enter a valid amount', 'warn'); return; }
    await initiateTopup(amount);
  });
}

async function loadBalance() {
  try {
    const w = await api.get('/wallet');
    document.getElementById('wb-amount').textContent = fmtCur(w.availableBalance || w.balance);
    const held = parseFloat(w.heldAmount || 0);
    const heldEl = document.getElementById('wb-held');
    if (heldEl) heldEl.textContent = held > 0 ? `${fmtCur(held)} reserved for active booking` : '';
  } catch {
    document.getElementById('wb-amount').textContent = '—';
  }
}

async function loadTransactions() {
  const listEl = document.getElementById('txn-list');
  const pagEl  = document.getElementById('txn-pagination');
  try {
    const resp = await api.get(`/wallet/transactions?page=${txnPage}&size=${TXN_SIZE}`);
    const txns = resp.content || [];
    const totalPages = resp.totalPages || 1;

    if (!txns.length && txnPage === 0) {
      listEl.innerHTML = '<p class="text-muted text-center">No transactions yet.</p>';
      pagEl.innerHTML = '';
      return;
    }

    listEl.innerHTML = txns.map(t => {
      const isCredit = t.type === 'CREDIT_TOPUP' || t.type === 'REFUND' || t.type === 'CREDIT_EARNING' || t.type === 'CREDIT_REFERRAL';
      const icon = TXN_ICON[t.type] || '💳';
      return `
        <div class="txn-item">
          <div class="txn-icon">${icon}</div>
          <div class="txn-info">
            <div class="txn-desc">${esc(t.description || t.type)}</div>
            <div class="txn-date">${fmtDate(t.processedAt || t.createdAt)}</div>
          </div>
          <div class="txn-amount ${isCredit ? 'credit' : 'debit'}">${isCredit ? '+' : '−'}${fmtCur(t.amount)}</div>
        </div>`;
    }).join('');

    if (totalPages > 1) {
      let html = '<div class="pagination">';
      html += `<button class="page-btn" ${txnPage === 0 ? 'disabled' : ''} data-p="${txnPage - 1}">‹</button>`;
      html += `<span class="page-info">Page ${txnPage + 1} of ${totalPages}</span>`;
      html += `<button class="page-btn" ${txnPage >= totalPages - 1 ? 'disabled' : ''} data-p="${txnPage + 1}">›</button>`;
      html += '</div>';
      pagEl.innerHTML = html;
      pagEl.querySelectorAll('.page-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const p = parseInt(btn.dataset.p, 10);
          if (isNaN(p) || p < 0 || p >= totalPages) return;
          txnPage = p;
          loadTransactions();
        });
      });
    } else {
      pagEl.innerHTML = '';
    }
  } catch {
    listEl.innerHTML = '<p class="text-muted text-center">Failed to load transactions.</p>';
    pagEl.innerHTML = '';
  }
}

async function initiateTopup(amount) {
  const btn = document.getElementById('btn-topup');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>';

  try {
    const order = await api.post('/wallet/topup/initiate', { amount });
    await openRazorpay(order);
    toast('Wallet credited successfully!', 'success');
    loadBalance();
    loadTransactions();
  } catch (err) {
    // Show feedback for user-cancelled payments (not already toasted by api.js)
    if (err && err.message === 'Payment cancelled') {
      toast('Payment cancelled', 'info');
    }
  } finally {
    btn.disabled = false;
    btn.textContent = 'Add';
  }
}

async function openRazorpay(order) {
  // Dev mock: simulate Razorpay success after 1s
  if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
    await sleep(1000);
    await api.post('/wallet/topup/verify', {
      razorpayOrderId:   order.razorpayOrderId,
      razorpayPaymentId: 'pay_mock_' + Date.now(),
      razorpaySignature: 'mock_signature',
    });
    return;
  }

  // Production: Razorpay checkout
  return new Promise((resolve, reject) => {
    if (typeof Razorpay === 'undefined') {
      toast('Razorpay not loaded', 'error');
      reject(new Error('Razorpay not loaded'));
      return;
    }
    const rzp = new Razorpay({
      key: order.key,
      amount: order.amount,
      currency: order.currency || 'INR',
      order_id: order.razorpayOrderId,
      name: 'HomeCare',
      description: 'Wallet Top-up',
      handler: async (response) => {
        try {
          await api.post('/wallet/topup/verify', {
            razorpayOrderId:   response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          });
          resolve();
        } catch (err) { reject(err); }
      },
      modal: { ondismiss: () => reject(new Error('Payment cancelled')) },
    });
    rzp.open();
  });
}
