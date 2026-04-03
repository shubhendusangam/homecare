/**
 * register.js — Customer & Helper registration with referral code support.
 */
import { auth }   from '../auth.js';
import { api }    from '../api.js';
import { router } from '../router.js';
import { toast }  from '../toast.js';

export function render(container) {
  container.innerHTML = `
    <div class="login-wrapper">
      <div class="login-card card">
        <span class="logo-large">🏠</span>
        <h2>Create Account</h2>
        <p class="subtitle">Join HomeCare today</p>

        <div class="toggle-row" id="role-toggle">
          <button class="toggle-btn active" data-r="CUSTOMER">Customer</button>
          <button class="toggle-btn" data-r="HELPER">Helper</button>
        </div>

        <form id="reg-form">
          <div class="form-group">
            <label for="reg-name">Full Name</label>
            <input type="text" id="reg-name" class="form-control" placeholder="John Doe" required minlength="2" maxlength="100" />
          </div>
          <div class="form-group">
            <label for="reg-email">Email</label>
            <input type="email" id="reg-email" class="form-control" placeholder="you@example.com" required autocomplete="email" />
          </div>
          <div class="form-group">
            <label for="reg-phone">Phone</label>
            <input type="tel" id="reg-phone" class="form-control" placeholder="9876543210" required minlength="10" maxlength="15" />
          </div>
          <div class="form-group">
            <label for="reg-password">Password</label>
            <input type="password" id="reg-password" class="form-control" placeholder="Min 8 chars, uppercase, number, symbol" required minlength="8" autocomplete="new-password" />
          </div>

          <!-- Helper-only: skills -->
          <div class="form-group hidden" id="skills-group">
            <label>Skills</label>
            <div class="skill-tags" id="skill-tags">
              <span class="skill-tag" data-s="CLEANING">🧹 Cleaning</span>
              <span class="skill-tag" data-s="COOKING">🍳 Cooking</span>
              <span class="skill-tag" data-s="BABYSITTING">👶 Babysitting</span>
              <span class="skill-tag" data-s="ELDERLY_HELP">🧓 Elderly Help</span>
            </div>
          </div>

          <!-- Customer-only: referral code -->
          <div class="form-group" id="referral-group">
            <label for="reg-referral">Referral Code (optional)</label>
            <input type="text" id="reg-referral" class="form-control" placeholder="e.g. PRIY4821" maxlength="20" />
          </div>

          <button type="submit" id="reg-submit" class="btn btn-primary btn-block">Create Account</button>
        </form>

        <div class="form-footer">
          <p>Already have an account? <a href="#/login">Sign In</a></p>
        </div>
      </div>
    </div>
  `;

  let role = 'CUSTOMER';
  let selectedSkills = [];

  // Role toggle
  container.querySelectorAll('#role-toggle .toggle-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      container.querySelectorAll('#role-toggle .toggle-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      role = btn.dataset.r;
      document.getElementById('skills-group').classList.toggle('hidden', role !== 'HELPER');
      document.getElementById('referral-group').classList.toggle('hidden', role !== 'CUSTOMER');
    });
  });

  // Skill tags
  container.querySelectorAll('#skill-tags .skill-tag').forEach(tag => {
    tag.addEventListener('click', () => {
      tag.classList.toggle('selected');
      const s = tag.dataset.s;
      if (selectedSkills.includes(s)) selectedSkills = selectedSkills.filter(x => x !== s);
      else selectedSkills.push(s);
    });
  });

  // Submit
  document.getElementById('reg-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('reg-name').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const phone = document.getElementById('reg-phone').value.trim();
    const password = document.getElementById('reg-password').value;
    const referralCode = document.getElementById('reg-referral').value.trim();

    if (!name || !email || !phone || !password) { toast('Please fill in all fields', 'warn'); return; }

    const submitBtn = document.getElementById('reg-submit');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating account…';

    try {
      const endpoint = role === 'HELPER' ? '/auth/register/helper' : '/auth/register/customer';
      const body = { name, email, phone, password };
      if (role === 'HELPER') body.skills = selectedSkills.length ? selectedSkills : ['CLEANING'];
      if (role === 'CUSTOMER' && referralCode) body.referralCode = referralCode;

      const data = await api.post(endpoint, body);
      auth.save(data.accessToken, data.role, { userId: data.userId, name: data.name, email: data.email });
      toast(`Welcome, ${data.name}! 🎉`, 'success');
      router.navigate(router._defaultRoute());
    } catch {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Create Account';
    }
  });
}

