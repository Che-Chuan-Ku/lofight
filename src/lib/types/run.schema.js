// Runs tag: create + settle (specs/api.yml).
import { z } from 'zod';
import { TierSchema, BuffItemSchema, RunStateSchema, DeathCauseSchema, DateTimeSchema } from './common.schema.js';

export const RunCreateRequestSchema = z.object({
  tier: TierSchema,
  buffItems: z.array(BuffItemSchema).max(1).optional()
});

export const BossKillSchema = z.object({
  bossSeq: z.number().int(),
  killedAtSecond: z.number().int().min(0)
});

export const RunSettleRequestSchema = z.object({
  gameSeconds: z.number().int().min(0),
  isVictory: z.boolean(),
  bossKills: z.array(BossKillSchema),
  deathCause: DeathCauseSchema.optional(),
  deathBossSeq: z.number().int().nullable().optional()
});

export const RunDetailResponseSchema = z.object({
  runId: z.string(),
  tier: TierSchema,
  equippedBuff: BuffItemSchema.nullable().optional(),
  boardVersion: z.string(),
  startedAt: DateTimeSchema,
  runState: RunStateSchema
});

export const BossRewardSchema = z.object({
  bossSeq: z.number().int(),
  coins: z.number().int()
});

export const RunSettlementDetailResponseSchema = z.object({
  runId: z.string(),
  runState: RunStateSchema,
  coinsAwarded: z.number().int(),
  walletCoins: z.number().int(),
  isSilverNewlyUnlocked: z.boolean(),
  bossRewards: z.array(BossRewardSchema)
});
