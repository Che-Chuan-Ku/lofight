// Shared HTTP client for /api/{system-code}/{api-version}. Every API function goes through apiRequest():
// envelope parsing (ManageResponse), bearer token, one silent token recovery on 401, and zod validation of `data`.
import { OPERATIONS } from './operations.js';

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api/csg/v1').replace(/\/$/, '');
const TIMEOUT_MS = 10000;

export class ApiError extends Error {
  // kind: 'http' (envelope error code), 'network' (offline / timeout / non-JSON), 'schema' (unexpected payload)
  constructor(kind, code, status, message) {
    super(message || code);
    this.name = 'ApiError';
    this.kind = kind;
    this.code = code;
    this.status = status;
  }
}

// Token plumbing is injected by lib/session.js so this module has no storage knowledge.
let tokenProvider = () => null;
let recoverHandler = null;
export function configureAuth({ getAccessToken, recover }) {
  tokenProvider = getAccessToken;
  recoverHandler = recover;
}

const RECOVERABLE_401 = new Set(['401001', '401002', '401003']);

function buildUrl(op, { pathParams, query }) {
  let path = op.path.replace(/\{(\w+)\}/g, (_, k) => encodeURIComponent(String(pathParams?.[k] ?? '')));
  const qs = new URLSearchParams();
  for (const [k, v] of Object.entries(query || {})) if (v !== undefined && v !== null && v !== '') qs.set(k, String(v));
  const s = qs.toString();
  return API_BASE_URL + path + (s ? '?' + s : '');
}

async function send(op, opts, token) {
  const headers = { Accept: 'application/json', ...(opts.headers || {}) };
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
  if (token && op.auth !== 'none') headers.Authorization = 'Bearer ' + token;
  const ctl = new AbortController();
  const timer = setTimeout(() => ctl.abort(), TIMEOUT_MS);
  let res;
  try {
    res = await fetch(buildUrl(op, opts), {
      method: op.method, headers, cache: 'no-store', signal: ctl.signal,
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined
    });
  } catch (_) {
    throw new ApiError('network', 'NETWORK', 0);
  } finally {
    clearTimeout(timer);
  }
  let json = null;
  try { json = await res.json(); } catch (_) { /* not JSON: proxy page, offline host, 5xx HTML */ }
  if (!json || typeof json !== 'object' || typeof json.status !== 'string') throw new ApiError('network', 'NETWORK', res.status);
  if (!res.ok || json.status !== 'success') throw new ApiError('http', String(json.code || res.status), res.status, json.message);
  return json.data;
}

/**
 * Calls one operation from OPERATIONS.
 * opts: { pathParams, query, headers, body, token } — `token` overrides the session token (login with a guest token).
 */
export async function apiRequest(operationId, opts = {}) {
  const op = OPERATIONS[operationId];
  if (!op) throw new Error('unknown operation ' + operationId);
  const explicit = Object.prototype.hasOwnProperty.call(opts, 'token');
  const token = explicit ? opts.token : (op.auth === 'none' ? null : tokenProvider());
  let data;
  try {
    data = await send(op, opts, token);
  } catch (err) {
    // Access token expired or revoked: recover once (refresh or a fresh guest), then replay the call.
    if (op.auth === 'required' && !explicit && recoverHandler && err.kind === 'http' && err.status === 401 && RECOVERABLE_401.has(err.code)) {
      const fresh = await recoverHandler();
      if (!fresh) throw err;
      data = await send(op, opts, fresh);
    } else throw err;
  }
  const parsed = op.response.safeParse(data);
  if (!parsed.success) {
    // A contract drift is an error, never a success: leave a trace (operationId + issue paths/codes only, no
    // response values, so no personal data reaches the console), then fail through the caller's error UI.
    console.error(`[api] ${operationId}: response does not match schema (${summarizeIssues(parsed.error.issues)})`);
    const err = new ApiError('schema', 'SCHEMA', 200, operationId + ': unexpected response');
    err.operationId = operationId;
    throw err;
  }
  return parsed.data;
}

const MAX_ISSUES = 5;
/** "path: code[ expected X]" per zod issue; values are deliberately left out. */
function summarizeIssues(issues) {
  const parts = issues.slice(0, MAX_ISSUES).map(i => {
    const path = i.path && i.path.length ? i.path.join('.') : '(root)';
    return `${path}: ${i.code}` + (typeof i.expected === 'string' ? ` expected ${i.expected}` : '');
  });
  if (issues.length > MAX_ISSUES) parts.push(`+${issues.length - MAX_ISSUES} more`);
  return parts.join('; ');
}
