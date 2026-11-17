-- CHANGE 2026-11-17: ECPay streamer-donation (live-stream payment link) notifications.
-- One row per verified, paid ECPay notification (or per verified-but-unusable one, see match_status). Idempotency anchor:
-- the unique TradeNo, so a resend or a concurrent duplicate can never be recorded (or credited) twice. MerchantTradeNo
-- is NOT unique: a payment link may reuse it for different payments.
-- match_status: MATCHED (credited through payment_orders / TopupCreditService), UNMATCHED, AMBIGUOUS, GUEST_ONLY,
-- DEFAULT_NAME (patron name is a system default nickname), SIMULATED (back-office simulated payment), INVALID (verified
-- but unusable: bad JSON, wrong inner merchant, missing fields, bad amount), ERROR (credit failed, see error_class).
-- Only MATCHED rows have player_id / payment_order_id. trade_no may be NULL only for INVALID rows.

CREATE TABLE donations (
    id                bigserial PRIMARY KEY,
    merchant_trade_no varchar(20)    NULL,
    trade_no          varchar(20)    NULL,
    amount_twd        integer        NULL,
    patron_name       varchar(100)   NULL,
    patron_note       varchar(200)   NULL,
    payment_type      varchar(50)    NULL,
    payment_date      varchar(32)    NULL,
    charge_fee        numeric(10, 2) NULL,
    simulate_paid     boolean        NOT NULL DEFAULT false,
    match_status      varchar(16)    NOT NULL,
    player_id         bigint         NULL REFERENCES players (id),
    payment_order_id  bigint         NULL REFERENCES payment_orders (id),
    error_class       varchar(64)    NULL,
    payload           text           NULL,
    created_at        timestamp(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        timestamp(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
CREATE UNIQUE INDEX uk_donations_trade_no ON donations (trade_no);
CREATE INDEX idx_donations_merchant_trade_no ON donations (merchant_trade_no);
CREATE INDEX idx_donations_match_status_created_at ON donations (match_status, created_at);
