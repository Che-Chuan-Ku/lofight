// Ops tag (specs/api.yml). Operator-only, no screen uses it (GAP-14=A); listed for contract coverage.
import { apiRequest } from './client.js';

export const getEventStatistics = ({ from, to }) => apiRequest('getEventStatistics', { query: { from, to } });
