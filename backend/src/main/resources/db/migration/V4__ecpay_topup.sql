-- CHANGE 2026-11-17 (D43-D52): ECPay AIO coin top-up and two-tier sponsor frame.
-- ledger_source TOPUP = 5, event_kind TOPUP = 4 are application-level smallint codes: no DDL needed for them.
-- payment_status codes: 0 CREATED, 1 AWAITING_PAYMENT, 2 PAID, 3 FAILED, 4 EXPIRED (PaymentStatus enum).

CREATE TABLE payment_orders (
    id                    bigserial PRIMARY KEY,
    player_id             bigint       NOT NULL REFERENCES players (id),
    merchant_trade_no     varchar(20)  NOT NULL,
    package_id            varchar(16)  NOT NULL,
    amount_twd            integer      NOT NULL CHECK (amount_twd >= 10),
    coins                 integer      NOT NULL CHECK (coins > 0),
    payment_status        smallint     NOT NULL DEFAULT 0,
    ecpay_env             varchar(8)   NOT NULL,
    merchant_id           varchar(10)  NOT NULL,
    trade_no              varchar(20)  NULL,
    payment_type          varchar(50)  NULL,
    rtn_code              integer      NULL,
    rtn_msg               varchar(200) NULL,
    trade_amt             integer      NULL,
    charge_fee            numeric(10, 2) NULL,
    is_simulated          boolean      NOT NULL DEFAULT false,
    failure_reason        varchar(32)  NULL,
    atm_bank_code         varchar(3)   NULL,
    atm_v_account         varchar(16)  NULL,
    payment_expire_at     timestamp(3) NULL,
    paid_at               timestamp(3) NULL,
    last_queried_at       timestamp(3) NULL,
    last_notify_payload   text         NULL,
    notify_count          integer      NOT NULL DEFAULT 0,
    created_at            timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at            timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by            bigint       NOT NULL,
    updated_by            bigint       NOT NULL,
    is_deleted            boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_payment_orders_merchant_trade_no ON payment_orders (merchant_trade_no);
CREATE INDEX idx_payment_orders_player_id_created_at ON payment_orders (player_id, created_at);
CREATE INDEX idx_payment_orders_payment_status_created_at ON payment_orders (payment_status, created_at);

-- Ledger link: one TOPUP credit per order at most (second guard after the order row lock, same idea as V3).
ALTER TABLE coin_ledger_entries ADD COLUMN payment_order_id bigint NULL REFERENCES payment_orders (id);
CREATE UNIQUE INDEX uk_coin_ledger_entries_payment_order_id ON coin_ledger_entries (payment_order_id);

-- Sponsor tiers (D48, D51): cumulative paid TWD, incremented in the crediting transaction only, never decreased by code.
-- Tier is derived in the application: 0 = total 0, 1 = total > 0, 2 = total >= topup.sponsor-tier2-min-twd (200).
ALTER TABLE players ADD COLUMN sponsored_total_twd integer NOT NULL DEFAULT 0 CHECK (sponsored_total_twd >= 0);
ALTER TABLE players ADD COLUMN first_sponsored_at timestamp(3) NULL;
