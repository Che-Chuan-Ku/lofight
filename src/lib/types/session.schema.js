// Sessions tag: guest / Google / refresh / logout / merge preview (specs/api.yml).
import { z } from 'zod';
import { PlayerKindSchema, MergeTargetKindSchema, BuffQuantitySchema, BestScoreSchema, DateTimeSchema } from './common.schema.js';

export const GuestSessionCreateRequestSchema = z.object({
  deviceId: z.string().min(1).max(64)
});

export const GoogleLoginRequestSchema = z.object({
  idToken: z.string().min(1)
});

// loginApple is deferred (x-deferred, BDY-01=B): the request schema is kept for contract parity, no client/handler.
export const AppleLoginRequestSchema = z.object({
  identityToken: z.string().min(1)
});

export const SessionRefreshRequestSchema = z.object({
  refreshToken: z.string().min(1)
});

export const SessionLogoutRequestSchema = z.object({
  refreshToken: z.string().min(1)
});

export const SessionDetailResponseSchema = z.object({
  playerId: z.string(),
  playerKind: PlayerKindSchema,
  accessToken: z.string(),
  accessTokenExpiresIn: z.number().int(),
  hasRefreshToken: z.boolean(),
  refreshToken: z.string().nullable().optional(),
  refreshTokenExpiresIn: z.number().int().nullable().optional(),
  refreshTokenExpiresAt: DateTimeSchema.nullable().optional()
});

// logoutSession 200: data is an (empty) object.
export const LogoutDataSchema = z.object({}).passthrough();

export const MergePreviewDetailResponseSchema = z.object({
  targetKind: MergeTargetKindSchema,
  guestCoins: z.number().int(),
  guestIsSilverUnlocked: z.boolean(),
  guestBuffInventories: z.array(BuffQuantitySchema),
  guestBestScores: z.array(BestScoreSchema),
  mergedCoins: z.number().int(),
  mergedIsSilverUnlocked: z.boolean(),
  mergedBuffInventories: z.array(BuffQuantitySchema),
  mergedBestScores: z.array(BestScoreSchema)
});
