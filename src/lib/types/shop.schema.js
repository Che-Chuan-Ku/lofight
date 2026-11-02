// Shop tag (specs/api.yml).
import { z } from 'zod';
import { BuffItemSchema, DateTimeSchema } from './common.schema.js';

export const ShopItemListResponseSchema = z.object({
  shopItemId: z.string(),
  buffItem: BuffItemSchema,
  price: z.number().int(),
  isPurchasable: z.boolean(),
  silverUnlockBossSeq: z.number().int().nullable().optional(),
  bronzeBestBossesKilled: z.number().int().nullable().optional()
});

// Rewarded-ad card state attached to listShopItems (AMB-A6=a). remainingCount / nextClaimableAt are null when closed.
export const AdRewardStatusResponseSchema = z.object({
  isEnabled: z.boolean(),
  remainingCount: z.number().int().min(0).nullable(),
  nextClaimableAt: DateTimeSchema.nullable()
});

// listShopItems data = { items: ShopItemListResponse[], adReward: AdRewardStatusResponse }.
// adReward is required (api.yml): front end and API ship in one image (FE-28), so no transitional .optional() (FE-33).
export const ShopItemsDataSchema = z.object({
  items: z.array(ShopItemListResponseSchema),
  adReward: AdRewardStatusResponseSchema
});

export const BuffPurchaseDetailResponseSchema = z.object({
  buffItem: BuffItemSchema,
  pricePaid: z.number().int(),
  walletCoins: z.number().int(),
  quantity: z.number().int()
});
