-- CHANGE 2026-11-20: sponsorship no longer pays coins (ECPay review; topup.coins-per-twd = 0). New orders carry coins = 0;
-- earlier orders keep their coins. The wallet and the TOPUP ledger are untouched for a 0-coin order.
ALTER TABLE payment_orders DROP CONSTRAINT payment_orders_coins_check;
ALTER TABLE payment_orders ADD CONSTRAINT payment_orders_coins_check CHECK (coins >= 0);
