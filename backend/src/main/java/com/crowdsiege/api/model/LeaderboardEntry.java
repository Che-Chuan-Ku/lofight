package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLRestriction;

/**
 * Best score per (board version, tier, player).
 */
@Entity
@Table(name = "leaderboard_entries")
@SQLRestriction("is_deleted = false")
public class LeaderboardEntry extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "board_version", nullable = false, length = 16)
    private String boardVersion;

    @Column(name = "tier", nullable = false)
    private Tier tier;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "is_victory", nullable = false)
    private boolean victory;

    @Column(name = "game_seconds", nullable = false)
    private int gameSeconds;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    protected LeaderboardEntry() {
    }

    public LeaderboardEntry(Long playerId, String boardVersion, Tier tier, Long runId, boolean victory,
                            int gameSeconds, Instant settledAt, Instant now) {
        this.playerId = playerId;
        this.boardVersion = boardVersion;
        this.tier = tier;
        this.runId = runId;
        this.victory = victory;
        this.gameSeconds = gameSeconds;
        this.settledAt = settledAt;
        markCreated(now, playerId);
    }

    public void replaceScore(LeaderboardEntry better, Instant now, Long actorId) {
        this.runId = better.runId;
        this.victory = better.victory;
        this.gameSeconds = better.gameSeconds;
        this.settledAt = better.settledAt;
        markUpdated(now, actorId);
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getBoardVersion() {
        return boardVersion;
    }

    public Tier getTier() {
        return tier;
    }

    public Long getRunId() {
        return runId;
    }

    public boolean isVictory() {
        return victory;
    }

    public int getGameSeconds() {
        return gameSeconds;
    }

    public Instant getSettledAt() {
        return settledAt;
    }
}
