// Shop tag (specs/api.yml).
import { z } from 'zod';
import { BuffItemSchema } from './common.schema.js';

export const ShopItemListResponseSchema = z.object({
  shopItemId: z.string(),
  buffItem: BuffItemSchema,
  price: z.number().int(),
  isPurchasable: z.boolean(),
  silverUnlockBossSeq: z.number().int().nullable().optional(),
  bronzeBestBossesKilled: z.number().int().nullable().optional()
});

// listShopItems data = { items: ShopItemListResponse[] }
export const ShopItemsDataSchema = z.object({
  items: z.array(ShopItemListResponseSchema)
});

export const BuffPurchaseDetailResponseSchema = z.object({
  buffItem: BuffItemSchema,
  pricePaid: z.number().int(),
  walletCoins: z.number().int(),
  quantity: z.number().int()
});
