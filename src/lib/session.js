// Session store: device id, access / refresh tokens, and the recovery rules for an expired token.
// Decisions (see frontend-state/_module-state.yml):
//  - guest token expired      -> issue-guest again with the same device id (same guest, new token)
//  - member token expired     -> refresh; if the refresh token is rejected, treat it as a logout and start a new guest
//    (_interaction-rules.md:144 suggests silent handling; the UI never shows a 401)
import { configureAuth, ApiError } from './api/client.js';
import { issueGuestSession, refreshSession, logoutSession } from './api/sessions.js';
import { readJson, writeJson } from './storage.js';

const DEVICE_KEY = 'csg.deviceId';
const SESSION_KEY = 'csg.session';

function uuid() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID();
  const b = new Uint8Array(16);
  (typeof crypto !== 'undefined' ? crypto : { getRandomValues: a => a.map(() => (Math.random() * 256) | 0) }).getRandomValues(b);
  b[6] = (b[6] & 0x0f) | 0x40; b[8] = (b[8] & 0x3f) | 0x80;
  const h = [...b].map(x => x.toString(16).padStart(2, '0')).join('');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

let state = readJson(SESSION_KEY, null);
const listeners = new Set();
const emit = () => { for (const l of listeners) l(); };

function save(next) { state = next; writeJson(SESSION_KEY, next); emit(); }

function deviceId(renew) {
  let id = renew ? null : readJson(DEVICE_KEY, null);
  if (!id) { id = uuid(); writeJson(DEVICE_KEY, id); }
  return id;
}

/** The local device id (same one issue-guest uses); claimAdReward sends it for auditing (AMB-A5=a). */
export const getDeviceId = () => deviceId(false);

function fromDetail(d) {
  return {
    playerId: d.playerId, playerKind: d.playerKind, accessToken: d.accessToken,
    refreshToken: d.hasRefreshToken ? d.refreshToken || null : null
  };
}

async function issueGuest(renewDevice) {
  try {
    return fromDetail(await issueGuestSession({ deviceId: deviceId(renewDevice) }));
  } catch (err) {
    // acc003: this device id belongs to a guest that was merged into an account; start over with a new one.
    if (!renewDevice && err instanceof ApiError && err.code === 'acc003') return fromDetail(await issueGuestSession({ deviceId: deviceId(true) }));
    throw err;
  }
}

let pending = null;
function single(fn) {
  if (!pending) pending = fn().finally(() => { pending = null; });
  return pending;
}

/** Makes sure a session exists (first visit creates a guest). Resolves to the session or throws ApiError. */
export function ensureSession() {
  if (state && state.accessToken) return Promise.resolve(state);
  return single(async () => { save(await issueGuest(false)); return state; });
}

/** 401 recovery hook for the API client. Returns the new access token, or null when nothing could be done. */
export function recover() {
  return single(async () => {
    try {
      if (state && state.playerKind === 'MEMBER' && state.refreshToken) {
        try {
          save(fromDetail(await refreshSession({ refreshToken: state.refreshToken })));
          return state;
        } catch (err) {
          if (!(err instanceof ApiError) || err.kind !== 'http') throw err;
          save(await issueGuest(true));   // refresh rejected: same as a logout
          return state;
        }
      }
      save(await issueGuest(false));
      return state;
    } catch (_) {
      return null;
    }
  }).then(s => (s ? s.accessToken : null));
}

/** Stores the session returned by loginGoogle (the guest is merged and voided server side). */
export function applyLogin(detail) { save(fromDetail(detail)); }

/** Logout: revoke the refresh token, then continue as a brand-new guest on a new device id (GAP-15=A). */
export async function logout() {
  const rt = state && state.refreshToken;
  if (rt) { try { await logoutSession({ refreshToken: rt }); } catch (_) { /* token already gone: still log out locally */ } }
  save(null);
  save(await issueGuest(true));
  return state;
}

export const getSession = () => state;
export const getAccessToken = () => (state ? state.accessToken : null);
export function subscribe(fn) { listeners.add(fn); return () => listeners.delete(fn); }

configureAuth({ getAccessToken, recover });
