// Shared schemas from specs/api.yml components/schemas (envelope + enums).
// Contract rule: the backend serialises null fields explicitly, so every optional field is .nullable().optional().
import { z } from 'zod';

export const DateTimeSchema = z.iso.datetime({ offset: true });

export const PlayerKindSchema = z.enum(['GUEST', 'MEMBER']);
export const LoginProviderSchema = z.enum(['GOOGLE', 'APPLE']);
export const BuffItemSchema = z.enum(['ATTACK_SPEED', 'ATTACK', 'CANNON', 'LASER']);
export const TierSchema = z.enum(['BRONZE', 'SILVER']);
export const RunStateSchema = z.enum(['OPEN', 'SETTLED', 'CHECK_FAILED', 'ABANDONED']);
export const DeathCauseSchema = z.enum(['RED_BREACH', 'BOSS_REACHED']);
export const LedgerSourceSchema = z.enum(['BOSS_REWARD', 'MERGE_IN', 'BUFF_PURCHASE', 'AD_REWARD']);
export const EventKindSchema = z.enum(['SHOP_VIEW', 'BUFF_PURCHASE', 'AD_REWARD']);
export const MergeTargetKindSchema = z.enum(['NEW', 'EXISTING']);

export const ManageResponseSchema = z.object({
  status: z.enum(['success', 'error', 'warning']),
  code: z.string().regex(/^([0-9]{6}|[a-z]{3}[0-9]{3})$/),
  message: z.string()
});

export const ManagePageResponseSchema = z.object({
  items: z.array(z.unknown()),
  totalCount: z.number().int()
});

// Failure variant: data is an empty object.
export const ErrorResponseSchema = ManageResponseSchema.extend({
  data: z.object({}).passthrough().nullable().optional()
});

export const BuffQuantitySchema = z.object({
  buffItem: BuffItemSchema,
  quantity: z.number().int().min(0)
});

export const BestScoreSchema = z.object({
  boardVersion: z.string(),
  tier: TierSchema,
  isVictory: z.boolean(),
  gameSeconds: z.number().int()
});
