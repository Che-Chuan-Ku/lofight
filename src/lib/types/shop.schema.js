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

// D44: one top-up package; coins come from the server (the front end never multiplies).
export const TopupPackageSchema = z.object({
  packageId: z.string(),
  amountTwd: z.number().int(),
  coins: z.number().int()
});

// Sponsor card state attached to listShopItems. packages is [] when isEnabled is false.
export const TopupStatusResponseSchema = z.object({
  isEnabled: z.boolean(),
  requiresLogin: z.boolean(),
  minAmountTwd: z.number().int(),
  coinsPerTwd: z.number().int(),
  packages: z.array(TopupPackageSchema)
});

// listShopItems data = { items, adReward, topup }.
// adReward and topup are required (api.yml): front end and API ship in one image (FE-28), so no transitional .optional() (FE-33).
export const ShopItemsDataSchema = z.object({
  items: z.array(ShopItemListResponseSchema),
  adReward: AdRewardStatusResponseSchema,
  topup: TopupStatusResponseSchema
});

export const TopupOrderStatusSchema = z.enum(['CREATED', 'AWAITING_PAYMENT', 'PAID', 'FAILED', 'EXPIRED']);

export const TopupOrderCreateRequestSchema = z.object({
  packageId: z.string().min(1)
});

// checkout.fields are all strings and are posted to the ECPay page untouched.
export const TopupCheckoutResponseSchema = z.object({
  actionUrl: z.string(),
  method: z.string(),
  fields: z.record(z.string(), z.string())
});

export const TopupOrderDetailResponseSchema = z.object({
  topupOrderId: z.string(),
  packageId: z.string(),
  amountTwd: z.number().int(),
  coins: z.number().int(),
  status: TopupOrderStatusSchema,
  checkout: TopupCheckoutResponseSchema
});

export const TopupOrderStatusResponseSchema = z.object({
  topupOrderId: z.string(),
  packageId: z.string(),
  amountTwd: z.number().int(),
  coins: z.number().int(),
  status: TopupOrderStatusSchema,
  paymentExpireAt: DateTimeSchema.nullable().optional(),
  paidAt: DateTimeSchema.nullable().optional(),
  walletCoins: z.number().int()
});

export const BuffPurchaseDetailResponseSchema = z.object({
  buffItem: BuffItemSchema,
  pricePaid: z.number().int(),
  walletCoins: z.number().int(),
  quantity: z.number().int()
});
