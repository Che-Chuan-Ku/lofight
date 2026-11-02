// Players tag: progress, nickname, wallet, ledger, buff inventory (specs/api.yml).
import { z } from 'zod';
import { PlayerKindSchema, LedgerSourceSchema, BuffItemSchema, BuffQuantitySchema, DateTimeSchema } from './common.schema.js';

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

export const WalletDetailResponseSchema = z.object({
  coins: z.number().int().min(0)
});

export const CoinLedgerEntryListResponseSchema = z.object({
  ledgerEntryId: z.string(),
  ledgerSource: LedgerSourceSchema,
  coinDelta: z.number().int(),
  coinsAfter: z.number().int(),
  bossSeq: z.number().int().nullable().optional(),
  buffItem: BuffItemSchema.nullable().optional(),
  runId: z.string().nullable().optional(),
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
