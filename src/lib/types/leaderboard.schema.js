// Leaderboards tag (specs/api.yml).
import { z } from 'zod';
import { TierSchema, BuffItemSchema, DateTimeSchema } from './common.schema.js';

export const LeaderboardEntryListResponseSchema = z.object({
  rank: z.number().int(),
  playerId: z.string(),
  nickname: z.string(),
  isVictory: z.boolean(),
  gameSeconds: z.number().int(),
  equippedBuff: BuffItemSchema.nullable().optional(),
  settledAt: DateTimeSchema
});

export const LeaderboardDetailResponseSchema = z.object({
  boardVersion: z.string(),
  tier: TierSchema,
  entries: z.array(LeaderboardEntryListResponseSchema).max(15),
  myRank: LeaderboardEntryListResponseSchema.nullable().optional(),
  neighbors: z.array(LeaderboardEntryListResponseSchema).max(4)
});
