// Envelope + auth helpers shared by the MSW handlers. Response shapes follow specs/api.yml ManageResponse / ErrorResponse.
import { http, HttpResponse } from 'msw';
import { API_BASE_URL } from '../../lib/api/client.js';
import { OPERATIONS } from '../../lib/api/operations.js';
import { getDb, persist } from '../db.js';

// Standard / custom error codes -> HTTP status and default message (api.yml responses + x-custom-error-codes).
const ERRORS = {
  400001: [400, '缺少必填參數'], 400007: [400, '參數超出範圍'], 400008: [400, '參數長度不符'], 400009: [400, '參數型別錯誤'],
  400012: [400, 'Enum 值無效'], 400013: [400, '違反業務規則'],
  401001: [401, 'Token 無效'], 401002: [401, 'Token 已過期'], 401003: [401, '未帶 Token'],
  403001: [403, '權限不足'], 403002: [403, '無權存取此資源'], 404001: [404, '指定資源不存在'],
  acc001: [401, '登入驗證失敗'], acc002: [400, '暱稱含禁用字'], acc003: [409, 'guest 身分已作廢，請重新產生裝置識別'], acc004: [400, '尚未支援的登入方式'],
  gam001: [403, 'silver 尚未解鎖'], gam002: [422, 'buff 庫存不足'], gam003: [422, '本局成績無效'], gam004: [422, '本局已逾時放棄'],
  shp001: [422, '金幣餘額不足'], shp002: [403, 'silver 尚未解鎖，無法購買'],
  eco001: [422, '今日廣告獎勵已達上限'], eco002: [422, '廣告獎勵冷卻中'], eco003: [403, '廣告獎勵尚未開放']
};

export const ok = (data, status = 200) =>
  HttpResponse.json({ status: 'success', code: status === 201 ? '201000' : '200000', message: '成功', data }, { status });

export function fail(code) {
  const [status, message] = ERRORS[code] || [500, '系統錯誤'];
  return HttpResponse.json({ status: 'error', code, message, data: {} }, { status });
}

/** Resolves the bearer token to an active player; returns { player } or { error: Response }. */
export function authenticate(request, { optional } = {}) {
  const h = request.headers.get('Authorization');
  if (!h) return optional ? { player: null } : { error: fail('401003') };
  const t = getDb().tokens[h.replace(/^Bearer\s+/, '')];
  if (!t) return { error: fail('401001') };
  if (t.expired) return { error: fail('401002') };
  const p = getDb().players[t.playerId];
  if (!p || p.isVoided) return { error: fail('401001') };
  return { player: p };
}

/** Registers a handler for an operation from the registry, honouring window.__mockDb.failNext(). */
export function route(operationId, resolver) {
  const op = OPERATIONS[operationId];
  const url = API_BASE_URL + op.path.replace(/\{(\w+)\}/g, ':$1');
  const method = op.method.toLowerCase();
  const h = http[method](url, async info => {
    const db = getDb();
    const forced = db.fail[operationId];
    if (forced) {
      if (--forced.left <= 0) delete db.fail[operationId];
      persist();
      if (forced.how === 'network') return HttpResponse.error();
      // a 200 success envelope whose data breaks the operation's zod schema (wrong types, missing fields)
      if (forced.how === 'schema') return ok({ playerId: 987654321, nickname: 'LEAK-CHECK@example.com' });
      return fail(forced.how);
    }
    let body;
    if (op.request) { try { body = await info.request.clone().json(); } catch (_) { body = undefined; } }
    return resolver({ ...info, body, url: new URL(info.request.url) });
  });
  h.operationId = operationId;
  return h;
}

export const blank = v => v === undefined || v === null || (typeof v === 'string' && v.trim() === '');
