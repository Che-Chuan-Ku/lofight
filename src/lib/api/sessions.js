// Sessions tag (specs/api.yml). Function names = operationId.
import { apiRequest } from './client.js';

export const issueGuestSession = ({ deviceId }) => apiRequest('issueGuestSession', { body: { deviceId } });
// guestAccessToken: when present the backend merges that guest into the account in the same transaction.
export const loginGoogle = ({ idToken, guestAccessToken }) => apiRequest('loginGoogle', { body: { idToken }, token: guestAccessToken || null });
export const refreshSession = ({ refreshToken }) => apiRequest('refreshSession', { body: { refreshToken } });
export const logoutSession = ({ refreshToken }) => apiRequest('logoutSession', { body: { refreshToken } });
export const getMergePreview = ({ loginProvider, providerToken }) =>
  apiRequest('getMergePreview', { query: { loginProvider }, headers: { 'X-Provider-Token': providerToken } });
