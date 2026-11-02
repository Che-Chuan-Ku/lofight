// Runs tag (specs/api.yml).
import { apiRequest } from './client.js';

export const createRun = ({ tier, buffItems }) => apiRequest('createRun', { body: buffItems && buffItems.length ? { tier, buffItems } : { tier, buffItems: [] } });
export const settleRun = ({ runId, settlement }) => apiRequest('settleRun', { pathParams: { runId }, body: settlement });
