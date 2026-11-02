-- CHANGE 2026-11-02 (D33-D35): rewarded ad +200 coins.
-- ledger_source / event_kind AD_REWARD are application-level smallint codes (3 / 2): no DDL needed.
-- Daily count and cooldown are derived from AD_REWARD ledger rows (AMB-A2=b): no claim table.

ALTER TABLE coin_ledger_entries ADD COLUMN device_id varchar(64) NULL;
ALTER TABLE player_events ADD COLUMN device_id varchar(64) NULL;

CREATE INDEX idx_coin_ledger_entries_player_id_ledger_source_created_at
    ON coin_ledger_entries (player_id, ledger_source, created_at);
