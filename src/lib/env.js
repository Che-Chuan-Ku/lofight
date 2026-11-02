// Build-time switches. MOCK_API is only ever true in `vite` dev mode, so production bundles drop the MSW code.
export const MOCK_API = import.meta.env.DEV && import.meta.env.VITE_API_MOCK === 'true';

// Google client id: the deployed page gets it at container start (backend/docker-entrypoint.sh fills
// <meta name="google-client-id">); while that meta is empty or still the __...__ placeholder (vite dev / preview),
// fall back to the build-time VITE_GOOGLE_CLIENT_ID from .env.local.
function runtimeClientId() {
  if (typeof document === 'undefined') return '';
  const meta = document.querySelector('meta[name="google-client-id"]');
  const value = meta ? (meta.getAttribute('content') || '').trim() : '';
  // Regex instead of the literal placeholder: the entrypoint's text replacement must never touch this code.
  return /^__[A-Z_]+__$/.test(value) ? '' : value;
}

export const GOOGLE_CLIENT_ID = runtimeClientId() || import.meta.env.VITE_GOOGLE_CLIENT_ID || '';
// Login entries are shown only when a Google sign-in can actually happen.
export const LOGIN_AVAILABLE = MOCK_API || !!GOOGLE_CLIENT_ID;
