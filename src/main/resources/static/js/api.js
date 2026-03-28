/**
 * api.js — Centralised HTTP client for HomeCare API (ES module).
 */
import { auth } from './auth.js';
import { toast } from './toast.js';

const BASE = '/api/v1';

export const api = {
  /**
   * Generic request handler.
   * @param {'GET'|'POST'|'PUT'|'PATCH'|'DELETE'} method
   * @param {string}  path       e.g. '/bookings'
   * @param {*}       [body]     request body (object or FormData)
   * @param {boolean} [isFormData]
   * @returns {Promise<*>}       unwrapped `data` from ApiResponse
   */
  async request(method, path, body = null, isFormData = false) {
    const headers = {};

    const token = auth.token();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const opts = { method, headers };
    if (body) {
      if (!isFormData) headers['Content-Type'] = 'application/json';
      opts.body = isFormData ? body : JSON.stringify(body);
    }

    let res;
    try {
      res = await fetch(`${BASE}${path}`, opts);
    } catch (err) {
      toast('Network error — please check your connection', 'error');
      throw err;
    }

    // JWT expired / unauthorized → silent logout
    if (res.status === 401) {
      auth.logout('Session expired — please log in again');
      throw new Error('Unauthorized');
    }

    // Try to parse JSON; some endpoints may return 204 No Content
    let json;
    try {
      json = await res.json();
    } catch {
      if (res.ok) return null;
      toast('Unexpected server response', 'error');
      throw new Error(`HTTP ${res.status}`);
    }

    if (!json.success) {
      const msg = json.message || 'Something went wrong';
      toast(msg, 'error');
      const err = new Error(msg);
      err.code = json.errorCode;
      err.data = json;
      throw err;
    }

    return json.data;
  },

  get(path)            { return api.request('GET',    path); },
  post(path, body)     { return api.request('POST',   path, body); },
  put(path, body)      { return api.request('PUT',    path, body); },
  patch(path, body)    { return api.request('PATCH',  path, body); },
  delete(path)         { return api.request('DELETE', path); },
  upload(path, formData) { return api.request('POST', path, formData, true); },
};
