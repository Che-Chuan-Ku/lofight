// 贊助 helpers (D44, D49): checkout hand-off to ECPay and the pending-order marker kept across the redirect.
import { MOCK_API } from './env.js';

const PENDING_KEY = 'dw.topup.pending';
export const TOPUP_ID_RE = /^DW[0-9A-Z]{18}$/;
const ECPAY_ACTION_PREFIXES = ['https://payment-stage.ecpay.com.tw/', 'https://payment.ecpay.com.tw/'];

/** The checkout form may only be posted to ECPay (spec 8.3); the dev mock additionally accepts its own origin stand-in. */
export function checkoutUrlAllowed(url) {
  if (typeof url !== 'string') return false;
  if (ECPAY_ACTION_PREFIXES.some(p => url.startsWith(p))) return true;
  return MOCK_API && url === location.origin + '/mock-ecpay';
}

const DONATE_HOSTS = ['p.ecpay.com.tw', 'payment.ecpay.com.tw'];

/** The streamer payment link may only be opened when it is https on an ECPay payment host (nothing else, no credentials). */
export function donateUrlAllowed(url) {
  if (typeof url !== 'string') return false;
  let u;
  try { u = new URL(url); } catch (_) { return false; }
  return u.protocol === 'https:' && DONATE_HOSTS.includes(u.hostname) && !u.username && !u.password && !u.port;
}

export const getPendingTopup = () => { try { return sessionStorage.getItem(PENDING_KEY); } catch (_) { return null; } };
export const setPendingTopup = id => { try { sessionStorage.setItem(PENDING_KEY, id); } catch (_) { /* storage blocked: the ?topup= param still carries the id */ } };
export const clearPendingTopup = () => { try { sessionStorage.removeItem(PENDING_KEY); } catch (_) { /* nothing to clear */ } };

/** Posts the server-signed fields to ECPay in this tab (no pop-up, nothing is altered). */
export function submitCheckout(checkout) {
  const f = document.createElement('form');
  f.method = 'POST'; f.action = checkout.actionUrl; f.style.display = 'none';
  for (const [k, v] of Object.entries(checkout.fields)) {
    const i = document.createElement('input'); i.type = 'hidden'; i.name = k; i.value = v; f.appendChild(i);
  }
  document.body.appendChild(f);
  // requestSubmit() fires a submit event (the dev mock listens for it); it posts like submit() when nothing cancels it
  if (typeof f.requestSubmit === 'function') f.requestSubmit(); else f.submit();
}
