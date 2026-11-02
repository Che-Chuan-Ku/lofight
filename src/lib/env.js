// Build-time switches. MOCK_API is only ever true in `vite` dev mode, so production bundles drop the MSW code.
export const MOCK_API = import.meta.env.DEV && import.meta.env.VITE_API_MOCK === 'true';
export const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';
// Login entries are shown only when a Google sign-in can actually happen.
export const LOGIN_AVAILABLE = MOCK_API || !!GOOGLE_CLIENT_ID;
