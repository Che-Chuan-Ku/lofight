// Build-time switches. MOCK_API is only ever true in `vite` dev mode, so production bundles drop the MSW code.
export const MOCK_API = import.meta.env.DEV && import.meta.env.VITE_API_MOCK === 'true';

// Runtime page settings: the deployed page gets them at container start (backend/docker-entrypoint.sh fills
// <meta name="google-client-id"> / <meta name="adsense-publisher-id">); while a meta is empty or still the __...__
// placeholder (vite dev / preview) the value counts as unset.
function runtimeMeta(name) {
  if (typeof document === 'undefined') return '';
  const meta = document.querySelector(`meta[name="${name}"]`);
  const value = meta ? (meta.getAttribute('content') || '').trim() : '';
  // Regex instead of the literal placeholder: the entrypoint's text replacement must never touch this code.
  return /^__[A-Z_]+__$/.test(value) ? '' : value;
}

// Google client id: runtime meta first, then the build-time VITE_GOOGLE_CLIENT_ID from .env.local (dev).
export const GOOGLE_CLIENT_ID = runtimeMeta('google-client-id') || import.meta.env.VITE_GOOGLE_CLIENT_ID || '';
// Login entries are shown only when a Google sign-in can actually happen.
export const LOGIN_AVAILABLE = MOCK_API || !!GOOGLE_CLIENT_ID;

// AdSense publisher id (ca-pub-<digits>, D35). Read on every call so the shop sees the page's current value.
// The VITE_ADSENSE_PUBLISHER_ID fallback (.env.local) is dev-server only: a production bundle never carries a baked-in
// id, so a page without the runtime value loads no ad script and shows no ad card (FE-34).
const DEV_PUBLISHER_ID = import.meta.env.DEV ? import.meta.env.VITE_ADSENSE_PUBLISHER_ID || '' : '';
export function adsensePublisherId() {
  const id = runtimeMeta('adsense-publisher-id') || DEV_PUBLISHER_ID;
  return /^ca-pub-\d{6,20}$/.test(id) ? id : '';
}
