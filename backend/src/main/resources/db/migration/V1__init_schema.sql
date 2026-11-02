-- Crowd Siege v19 initial schema, derived from specs/erm.dbml.
-- Type mapping (erm.dbml Project note): tinyint -> smallint, tinyint(1) flags -> boolean,
-- nvarchar(N) -> varchar(N), datetime(3) -> timestamp(3) (stored as UTC).

CREATE TABLE players (
    id                    bigserial PRIMARY KEY,
    player_kind           smallint     NOT NULL,
    nickname              varchar(12)  NOT NULL,
    device_id             varchar(64)  NULL,
    is_silver_unlocked    boolean      NOT NULL DEFAULT false,
    silver_unlocked_at    timestamp(3) NULL,
    is_voided             boolean      NOT NULL DEFAULT false,
    voided_at             timestamp(3) NULL,
    merged_into_player_id bigint       NULL REFERENCES players (id),
    created_at            timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at            timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by            bigint       NOT NULL,
    updated_by            bigint       NOT NULL,
    is_deleted            boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_players_device_id ON players (device_id);
CREATE INDEX idx_players_merged_into_player_id ON players (merged_into_player_id);

CREATE TABLE player_identities (
    id               bigserial PRIMARY KEY,
    player_id        bigint       NOT NULL REFERENCES players (id),
    login_provider   smallint     NOT NULL,
    provider_subject varchar(255) NOT NULL,
    created_at       timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by       bigint       NOT NULL,
    updated_by       bigint       NOT NULL,
    is_deleted       boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_player_identities_login_provider_provider_subject
    ON player_identities (login_provider, provider_subject);
CREATE UNIQUE INDEX uk_player_identities_player_id_login_provider
    ON player_identities (player_id, login_provider);

CREATE TABLE refresh_tokens (
    id                   bigserial PRIMARY KEY,
    player_id            bigint       NOT NULL REFERENCES players (id),
    token_hash           varchar(64)  NOT NULL,
    expires_at           timestamp(3) NOT NULL,
    revoked_at           timestamp(3) NULL,
    replaced_by_token_id bigint       NULL REFERENCES refresh_tokens (id),
    created_at           timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by           bigint       NOT NULL,
    updated_by           bigint       NOT NULL,
    is_deleted           boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_player_id ON refresh_tokens (player_id);

CREATE TABLE wallets (
    id         bigserial PRIMARY KEY,
    player_id  bigint       NOT NULL REFERENCES players (id),
    coins      integer      NOT NULL DEFAULT 0 CHECK (coins >= 0),
    created_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by bigint       NOT NULL,
    updated_by bigint       NOT NULL,
    is_deleted boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_wallets_player_id ON wallets (player_id);

CREATE TABLE runs (
    id                       bigserial PRIMARY KEY,
    player_id                bigint       NOT NULL REFERENCES players (id),
    tier                     smallint     NOT NULL,
    equipped_buff            smallint     NULL,
    board_version            varchar(16)  NOT NULL,
    started_at               timestamp(3) NOT NULL,
    run_state                smallint     NOT NULL DEFAULT 0,
    settled_at               timestamp(3) NULL,
    game_seconds             integer      NULL,
    is_victory               boolean      NULL,
    bosses_killed            integer      NULL,
    death_cause              smallint     NULL,
    death_boss_seq           integer      NULL,
    coins_awarded            integer      NULL,
    is_silver_newly_unlocked boolean      NULL,
    created_at               timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at               timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by               bigint       NOT NULL,
    updated_by               bigint       NOT NULL,
    is_deleted               boolean      NOT NULL DEFAULT false
);
CREATE INDEX idx_runs_player_id ON runs (player_id);
CREATE INDEX idx_runs_run_state_started_at ON runs (run_state, started_at);

CREATE TABLE coin_ledger_entries (
    id                    bigserial PRIMARY KEY,
    player_id             bigint       NOT NULL REFERENCES players (id),
    ledger_source         smallint     NOT NULL,
    coin_delta            integer      NOT NULL,
    coins_after           integer      NOT NULL,
    run_id                bigint       NULL REFERENCES runs (id),
    boss_seq              integer      NULL,
    buff_item             smallint     NULL,
    merged_from_player_id bigint       NULL REFERENCES players (id),
    created_at            timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at            timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by            bigint       NOT NULL
);
CREATE INDEX idx_coin_ledger_entries_player_id_created_at_id ON coin_ledger_entries (player_id, created_at, id);
CREATE INDEX idx_coin_ledger_entries_run_id ON coin_ledger_entries (run_id);

CREATE TABLE buff_inventories (
    id         bigserial PRIMARY KEY,
    player_id  bigint       NOT NULL REFERENCES players (id),
    buff_item  smallint     NOT NULL,
    quantity   integer      NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    created_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by bigint       NOT NULL,
    updated_by bigint       NOT NULL,
    is_deleted boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_buff_inventories_player_id_buff_item ON buff_inventories (player_id, buff_item);

CREATE TABLE run_boss_kills (
    id               bigserial PRIMARY KEY,
    run_id           bigint       NOT NULL REFERENCES runs (id),
    boss_seq         integer      NOT NULL,
    killed_at_second integer      NOT NULL,
    coins_awarded    integer      NOT NULL,
    created_at       timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by       bigint       NOT NULL
);
CREATE UNIQUE INDEX uk_run_boss_kills_run_id_boss_seq ON run_boss_kills (run_id, boss_seq);

CREATE TABLE leaderboard_entries (
    id            bigserial PRIMARY KEY,
    player_id     bigint       NOT NULL REFERENCES players (id),
    board_version varchar(16)  NOT NULL,
    tier          smallint     NOT NULL,
    run_id        bigint       NOT NULL REFERENCES runs (id),
    is_victory    boolean      NOT NULL,
    game_seconds  integer      NOT NULL,
    settled_at    timestamp(3) NOT NULL,
    created_at    timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by    bigint       NOT NULL,
    updated_by    bigint       NOT NULL,
    is_deleted    boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_leaderboard_entries_board_version_tier_player_id
    ON leaderboard_entries (board_version, tier, player_id);
CREATE INDEX idx_leaderboard_entries_board_version_tier_is_victory_game_seconds_settled_at
    ON leaderboard_entries (board_version, tier, is_victory, game_seconds, settled_at);

CREATE TABLE player_events (
    id                 bigserial PRIMARY KEY,
    event_kind         smallint     NOT NULL,
    player_id          bigint       NOT NULL REFERENCES players (id),
    is_silver_unlocked boolean      NOT NULL,
    buff_item          smallint     NULL,
    paid_coins         integer      NULL,
    created_at         timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at         timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by         bigint       NOT NULL
);
CREATE INDEX idx_player_events_event_kind_created_at ON player_events (event_kind, created_at);
