// Ops tag: internal statistics (no UI, GAP-14=A); kept so the API layer covers every active operation.
import { z } from 'zod';
import { EventKindSchema, BuffItemSchema, DateTimeSchema } from './common.schema.js';

export const EventTotalSchema = z.object({
  eventKind: EventKindSchema,
  count: z.number().int(),
  uniquePlayers: z.number().int()
});

export const PurchaseByItemSchema = z.object({
  buffItem: BuffItemSchema,
  count: z.number().int(),
  totalCoins: z.number().int()
});

export const EventStatisticsDetailResponseSchema = z.object({
  from: DateTimeSchema,
  to: DateTimeSchema,
  eventTotals: z.array(EventTotalSchema),
  purchasesByItem: z.array(PurchaseByItemSchema)
});
