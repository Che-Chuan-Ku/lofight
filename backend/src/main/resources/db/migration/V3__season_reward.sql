-- CHANGE 2026-11-03 (D38): season reward paid at backend startup for finished seasons.
-- ledger_source / event_kind SEASON_REWARD are application-level smallint codes (4 / 3): no DDL needed.

ALTER TABLE coin_ledger_entries ADD COLUMN season_board_version varchar(16) NULL;
ALTER TABLE coin_ledger_entries ADD COLUMN season_tier smallint NULL;
ALTER TABLE coin_ledger_entries ADD COLUMN season_rank integer NULL;

-- Second guard against double payment (INF-S16); NULLs of other sources never collide.
CREATE UNIQUE INDEX uk_coin_ledger_entries_player_id_season_board_version_season_tier
    ON coin_ledger_entries (player_id, season_board_version, season_tier);

-- One row per (season, tier), written in the same transaction as its credits (INF-S4, INF-S14).
CREATE TABLE season_settlements (
    id              bigserial PRIMARY KEY,
    board_version   varchar(16)  NOT NULL,
    tier            smallint     NOT NULL,
    recipient_count integer      NOT NULL DEFAULT 0,
    total_coins     integer      NOT NULL DEFAULT 0,
    settled_at      timestamp(3) NOT NULL,
    created_at      timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
CREATE UNIQUE INDEX uk_season_settlements_board_version_tier ON season_settlements (board_version, tier);
