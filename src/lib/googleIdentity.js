// Google Identity Services loader. The script is fetched at runtime from Google only when the login page opens.
import { GOOGLE_CLIENT_ID } from './env.js';

let loading = null;
function loadScript() {
  if (window.google && window.google.accounts && window.google.accounts.id) return Promise.resolve();
  if (!loading) {
    loading = new Promise((resolve, reject) => {
      const s = document.createElement('script');
      s.src = 'https://accounts.google.com/gsi/client';
      s.async = true;
      s.onload = () => resolve();
      s.onerror = () => { loading = null; reject(new Error('gsi')); };
      document.head.appendChild(s);
    });
  }
  return loading;
}

/** Renders Google's own sign-in button into `el`; `onCredential(idToken)` fires after the user picks an account. */
export async function renderGoogleButton(el, onCredential, width) {
  await loadScript();
  const id = window.google.accounts.id;
  id.initialize({ client_id: GOOGLE_CLIENT_ID, callback: r => r && r.credential && onCredential(r.credential), ux_mode: 'popup', auto_select: false });
  id.renderButton(el, { type: 'standard', theme: 'outline', size: 'large', text: 'signin_with', shape: 'rectangular', locale: 'zh-TW', width: Math.min(400, Math.max(200, Math.round(width || 300))) });
}
