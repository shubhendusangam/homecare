/**
 * admin/config.js — Admin system configuration.
 *
 * - Service pricing table: 4 rows, inline editable cells for basePrice and perHourPrice
 *   [Save] per row → PUT /api/v1/admin/service-config/{serviceType}
 * - Broadcast notification form: message to all customers or all helpers
 */
import { api }  from '../../api.js';
import { toast } from '../../toast.js';

const SERVICE_ICON = { CLEANING: '🧹', COOKING: '🍳', BABYSITTING: '👶', ELDERLY_HELP: '🧓' };

function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

export async function render(container) {
  container.innerHTML = `
    <div class="section-header"><h2>System Configuration</h2></div>
    <nav class="sub-nav">
      <a href="#/admin/dashboard" class="sub-nav-btn">Overview</a>
      <a href="#/admin/bookings"  class="sub-nav-btn">Bookings</a>
      <a href="#/admin/users"     class="sub-nav-btn">Users</a>
      <a href="#/admin/helpers"   class="sub-nav-btn">Helpers</a>
      <a href="#/admin/payments"  class="sub-nav-btn">Payments</a>
      <a href="#/admin/config"    class="sub-nav-btn active">Config</a>
    </nav>

    <!-- Service Pricing -->
    <div class="card mb-2">
      <h3 style="font-size:.95rem;margin-bottom:.75rem">💰 Service Pricing</h3>
      <div id="pricing-table-slot">
        <div class="text-center text-muted"><span class="spinner"></span></div>
      </div>
    </div>

    <!-- Broadcast Notification -->
    <div class="card mb-2">
      <h3 style="font-size:.95rem;margin-bottom:.75rem">📢 Broadcast Notification</h3>
      <div class="form-group">
        <label>Target Audience</label>
        <select class="form-control" id="broadcast-target">
          <option value="">All Users</option>
          <option value="CUSTOMER">All Customers</option>
          <option value="HELPER">All Helpers</option>
        </select>
      </div>
      <div class="form-group">
        <label>Title</label>
        <input type="text" class="form-control" id="broadcast-title" placeholder="Notification title…">
      </div>
      <div class="form-group">
        <label>Message</label>
        <textarea class="form-control" id="broadcast-body" rows="3" placeholder="Notification message…"></textarea>
      </div>
      <button class="btn btn-primary" id="btn-broadcast">Send Broadcast</button>
    </div>
  `;

  loadPricing();

  // Broadcast handler
  document.getElementById('btn-broadcast').addEventListener('click', async () => {
    const title = document.getElementById('broadcast-title')?.value.trim();
    const body = document.getElementById('broadcast-body')?.value.trim();
    const role = document.getElementById('broadcast-target')?.value || null;

    if (!title || !body) {
      toast('Please fill in title and message', 'warn');
      return;
    }

    const btn = document.getElementById('btn-broadcast');
    btn.disabled = true;
    btn.textContent = 'Sending…';

    try {
      const payload = { title, body };
      if (role) payload.role = role;
      const result = await api.post('/admin/notifications/broadcast', payload);
      toast(`Broadcast sent to ${result?.recipientCount || 0} users`, 'success');
      document.getElementById('broadcast-title').value = '';
      document.getElementById('broadcast-body').value = '';
    } catch {
      // Error shown by api module
    } finally {
      btn.disabled = false;
      btn.textContent = 'Send Broadcast';
    }
  });
}

async function loadPricing() {
  const slot = document.getElementById('pricing-table-slot');
  if (!slot) return;

  try {
    const configs = await api.get('/admin/service-config');
    if (!configs || !configs.length) {
      slot.innerHTML = '<p class="text-muted">No service configurations found.</p>';
      return;
    }

    slot.innerHTML = `
      <table class="admin-table">
        <thead>
          <tr>
            <th>Service</th>
            <th>Base Price (₹)</th>
            <th>Per Hour (₹)</th>
            <th>Active</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          ${configs.map(c => {
            const icon = SERVICE_ICON[c.serviceType] || '📦';
            return `
              <tr data-service="${c.serviceType}">
                <td>${icon} ${esc((c.name || c.serviceType || '').replace(/_/g, ' '))}</td>
                <td>
                  <input type="number" class="form-control inline-edit"
                    id="base-${c.serviceType}" value="${c.basePrice || 0}" min="0" step="1">
                </td>
                <td>
                  <input type="number" class="form-control inline-edit"
                    id="hour-${c.serviceType}" value="${c.perHourPrice || 0}" min="0" step="1">
                </td>
                <td>${c.active ? '✅' : '❌'}</td>
                <td>
                  <button class="btn btn-primary btn-sm" data-save="${c.serviceType}">Save</button>
                </td>
              </tr>
            `;
          }).join('')}
        </tbody>
      </table>
    `;

    // Save handlers per row
    slot.querySelectorAll('[data-save]').forEach(btn => {
      btn.addEventListener('click', async () => {
        const serviceType = btn.dataset.save;
        const basePrice = parseFloat(document.getElementById('base-' + serviceType)?.value) || 0;
        const perHourPrice = parseFloat(document.getElementById('hour-' + serviceType)?.value) || 0;

        if (basePrice < 0 || perHourPrice < 0) {
          toast('Prices cannot be negative', 'warn');
          return;
        }

        btn.disabled = true;
        btn.textContent = 'Saving…';

        try {
          await api.put('/admin/service-config/' + serviceType, { basePrice, perHourPrice });
          toast(`${serviceType.replace(/_/g, ' ')} pricing updated!`, 'success');
        } catch {
          // Error shown by api
        } finally {
          btn.disabled = false;
          btn.textContent = 'Save';
        }
      });
    });

  } catch {
    slot.innerHTML = '<p class="text-muted">Could not load service pricing.</p>';
  }
}
