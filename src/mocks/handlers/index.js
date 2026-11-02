import { sessionHandlers } from './sessions.js';
import { playerHandlers } from './players.js';
import { shopHandlers } from './shop.js';
import { runHandlers } from './runs.js';
import { leaderboardHandlers } from './leaderboards.js';
import { opsHandlers } from './ops.js';

export const handlers = [...sessionHandlers, ...playerHandlers, ...shopHandlers, ...runHandlers, ...leaderboardHandlers, ...opsHandlers];
