// Players tag (specs/api.yml).
import { apiRequest } from './client.js';

export const getMyProgress = () => apiRequest('getMyProgress');
export const updateMyNickname = ({ nickname }) => apiRequest('updateMyNickname', { body: { nickname } });
export const getMyWallet = () => apiRequest('getMyWallet');
export const listMyCoinLedgerEntries = ({ page } = {}) => apiRequest('listMyCoinLedgerEntries', { query: { page } });
export const createTopupOrder = ({ packageId }) => apiRequest('createTopupOrder', { body: { packageId } });
export const getTopupOrder = ({ topupOrderId }) => apiRequest('getTopupOrder', { pathParams: { topupOrderId } });
export const listMyBuffInventories = () => apiRequest('listMyBuffInventories');
export const claimAdReward = ({ deviceId }) => apiRequest('claimAdReward', { body: { deviceId } });
