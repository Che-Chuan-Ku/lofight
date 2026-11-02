// Players tag (specs/api.yml).
import { apiRequest } from './client.js';

export const getMyProgress = () => apiRequest('getMyProgress');
export const updateMyNickname = ({ nickname }) => apiRequest('updateMyNickname', { body: { nickname } });
export const getMyWallet = () => apiRequest('getMyWallet');
export const listMyCoinLedgerEntries = ({ page } = {}) => apiRequest('listMyCoinLedgerEntries', { query: { page } });
export const listMyBuffInventories = () => apiRequest('listMyBuffInventories');
