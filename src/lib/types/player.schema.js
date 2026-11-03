// Players tag: progress, nickname, wallet, ledger, buff inventory (specs/api.yml).
import { z } from 'zod';
import { PlayerKindSchema, TierSchema, LedgerSourceSchema, BuffItemSchema, BuffQuantitySchema, DateTimeSchema } from './common.schema.js';

export const PlayerDetailResponseSchema = z.object({
  playerId: z.string(),
  nickname: z.string(),
  playerKind: PlayerKindSchema,
  isSilverUnlocked: z.boolean(),
  silverUnlockBossSeq: z.number().int().nullable().optional(),
  bronzeBestBossesKilled: z.number().int().nullable().optional()
});

export const PlayerUpdateRequestSchema = z.object({
  nickname: z.string().max(12)
});

// D38 season reward: one item per board (BRONZE before SILVER).
export const SeasonRewardItemSchema = z.object({
  tier: TierSchema,
  rank: z.number().int().min(1).max(5),
  coins: z.number().int().min(1)
});

export const LatestSeasonRewardSchema = z.object({
  boardVersion: z.string().max(16),
  awardedAt: DateTimeSchema,
  items: z.array(SeasonRewardItemSchema).min(1).max(2)
});

export const WalletDetailResponseSchema = z.object({
  coins: z.number().int().min(0),
  // null for guests and players who never received a season reward
  latestSeasonReward: LatestSeasonRewardSchema.nullable().optional()
});

export const CoinLedgerEntryListResponseSchema = z.object({
  ledgerEntryId: z.string(),
  ledgerSource: LedgerSourceSchema,
  coinDelta: z.number().int(),
  coinsAfter: z.number().int(),
  bossSeq: z.number().int().nullable().optional(),
  buffItem: BuffItemSchema.nullable().optional(),
  runId: z.string().nullable().optional(),
  seasonBoardVersion: z.string().max(16).nullable().optional(),
  seasonTier: TierSchema.nullable().optional(),
  seasonRank: z.number().int().min(1).max(5).nullable().optional(),
  occurredAt: DateTimeSchema
});

// listMyCoinLedgerEntries data = ManagePageResponse + { page, hasNext, items: CoinLedgerEntryListResponse[] }
export const CoinLedgerPageDataSchema = z.object({
  items: z.array(CoinLedgerEntryListResponseSchema),
  totalCount: z.number().int(),
  page: z.number().int(),
  hasNext: z.boolean()
});

export const BuffInventoryListResponseSchema = BuffQuantitySchema;

// listMyBuffInventories data = { items: BuffInventoryListResponse[] }
export const BuffInventoryItemsDataSchema = z.object({
  items: z.array(BuffInventoryListResponseSchema)
});

// claimAdReward (POST /players/me/ad-rewards): request = the local device id, response = AdRewardDetailResponse (201)
export const AdRewardCreateRequestSchema = z.object({
  deviceId: z.string().min(1).max(64)
});

export const AdRewardDetailResponseSchema = z.object({
  adRewardId: z.string(),
  coinsAwarded: z.number().int(),
  walletCoins: z.number().int().min(0),
  remainingCount: z.number().int().min(0),
  nextClaimableAt: DateTimeSchema
});
