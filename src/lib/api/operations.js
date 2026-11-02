// Operation registry: one entry per specs/api.yml operation (method, path, auth, params, request/response schemas).
// The API functions, the MSW handlers and tests/contract/check-api-contract.mjs all read this table,
// so a drift from api.yml shows up in one place.
import * as T from '../types/index.js';

export const OPERATIONS = {
  issueGuestSession: { method: 'POST', path: '/sessions/actions/issue-guest', auth: 'none', request: T.GuestSessionCreateRequestSchema, response: T.SessionDetailResponseSchema, errorCodes: ['400001', 'acc003'] },
  loginGoogle: { method: 'POST', path: '/sessions/actions/login-google', auth: 'optional', request: T.GoogleLoginRequestSchema, response: T.SessionDetailResponseSchema, errorCodes: ['400001', 'acc001', '401001'] },
  refreshSession: { method: 'POST', path: '/sessions/actions/refresh', auth: 'none', request: T.SessionRefreshRequestSchema, response: T.SessionDetailResponseSchema, errorCodes: ['400001', '401001', '401002'] },
  logoutSession: { method: 'POST', path: '/sessions/actions/logout', auth: 'required', request: T.SessionLogoutRequestSchema, response: T.LogoutDataSchema, errorCodes: ['400001'] },
  getMergePreview: { method: 'GET', path: '/merge-previews', auth: 'required', query: ['loginProvider'], headers: ['X-Provider-Token'], response: T.MergePreviewDetailResponseSchema, errorCodes: ['400001', '400012', 'acc001', 'acc004', '403001'] },
  getMyProgress: { method: 'GET', path: '/players/me', auth: 'required', response: T.PlayerDetailResponseSchema, errorCodes: [] },
  updateMyNickname: { method: 'PATCH', path: '/players/me', auth: 'required', request: T.PlayerUpdateRequestSchema, response: T.PlayerDetailResponseSchema, errorCodes: ['400001', '400008', 'acc002'] },
  getMyWallet: { method: 'GET', path: '/players/me/wallet', auth: 'required', response: T.WalletDetailResponseSchema, errorCodes: [] },
  listMyCoinLedgerEntries: { method: 'GET', path: '/players/me/coin-ledger-entries', auth: 'required', query: ['page'], response: T.CoinLedgerPageDataSchema, errorCodes: ['400007', '400009'] },
  listMyBuffInventories: { method: 'GET', path: '/players/me/buff-inventories', auth: 'required', response: T.BuffInventoryItemsDataSchema, errorCodes: [] },
  listShopItems: { method: 'GET', path: '/shop-items', auth: 'required', response: T.ShopItemsDataSchema, errorCodes: [] },
  purchaseShopItem: { method: 'POST', path: '/shop-items/{shopItemId}/actions/purchase', auth: 'required', pathParams: ['shopItemId'], response: T.BuffPurchaseDetailResponseSchema, errorCodes: ['404001', 'shp001', 'shp002'] },
  createRun: { method: 'POST', path: '/runs', auth: 'required', request: T.RunCreateRequestSchema, response: T.RunDetailResponseSchema, successStatus: 201, errorCodes: ['400001', '400007', '400012', '400013', 'gam001', 'gam002'] },
  settleRun: { method: 'POST', path: '/runs/{runId}/actions/settle', auth: 'required', pathParams: ['runId'], request: T.RunSettleRequestSchema, response: T.RunSettlementDetailResponseSchema, errorCodes: ['400001', '403002', '404001', 'gam003', 'gam004'] },
  getLeaderboard: { method: 'GET', path: '/leaderboards', auth: 'required', query: ['tier', 'boardVersion'], response: T.LeaderboardDetailResponseSchema, errorCodes: ['400001', '400012'] },
  // 400009 is not listed in api.yml x-error-codes but EventStatisticsService throws it; the client and mock handle it.
  getEventStatistics: { method: 'GET', path: '/internal/ops/event-statistics', auth: 'none', query: ['from', 'to'], response: T.EventStatisticsDetailResponseSchema, errorCodes: ['400001', '400007', '400009'] }
};

// Deferred in api.yml (x-deferred: true, BDY-01=B): contract kept, no client function or handler this round.
export const DEFERRED_OPERATIONS = ['loginApple'];
