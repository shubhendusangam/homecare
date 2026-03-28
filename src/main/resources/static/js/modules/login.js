/**
 * login.js — Login screen module.
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
        <h2>Welcome to HomeCare</h2>
        <p class="subtitle">Professional home services at your doorstep</p>

        <form id="login-form">
          <div class="form-group">
            <label for="login-email">Email</label>
            <input type="email" id="login-email" class="form-control"
                   placeholder="you@example.com" required autocomplete="email" />
          </div>
          <div class="form-group">
            <label for="login-password">Password</label>
            <input type="password" id="login-password" class="form-control"
                   placeholder="••••••••" required autocomplete="current-password" />
          </div>
          <button type="submit" id="login-submit" class="btn btn-primary btn-block">
            Sign In
          </button>
        </form>

        <div class="form-footer">
          <p>Don't have an account? <a href="#/register">Register</a></p>
          <p class="mt-1"><a href="#/forgot-password">Forgot password?</a></p>
        </div>
      </div>
    </div>
  `;

  const form   = document.getElementById('login-form');
  const submit = document.getElementById('login-submit');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email    = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;

    if (!email || !password) {
      toast('Please fill in all fields', 'warn');
      return;
    }

    submit.disabled = true;
    submit.textContent = 'Signing in…';

    try {
      const data = await api.post('/auth/login', { email, password });

      // data = { accessToken, refreshToken, role, userId, name, email }
      auth.save(data.accessToken, data.role, {
        userId: data.userId,
        name:   data.name,
        email:  data.email,
      });

      toast(`Welcome back, ${data.name}!`, 'success');
      router.navigate(router._defaultRoute());
    } catch (err) {
      // api.js already toasts the error
      submit.disabled = false;
      submit.textContent = 'Sign In';
    }
  });
}

