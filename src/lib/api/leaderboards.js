// Leaderboards tag (specs/api.yml).
import { apiRequest } from './client.js';

export const getLeaderboard = ({ tier, boardVersion }) => apiRequest('getLeaderboard', { query: { tier, boardVersion } });
