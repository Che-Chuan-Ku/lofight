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
 * One run per game. State flow: OPEN -> SETTLED | CHECK_FAILED | ABANDONED (all terminal).
 */
@Entity
@Table(name = "runs")
@SQLRestriction("is_deleted = false")
public class Run extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "tier", nullable = false)
    private Tier tier;

    @Column(name = "equipped_buff")
    private BuffItem equippedBuff;

    @Column(name = "board_version", nullable = false, length = 16)
    private String boardVersion;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "run_state", nullable = false)
    private RunState runState;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "game_seconds")
    private Integer gameSeconds;

    @Column(name = "is_victory")
    private Boolean victory;

    @Column(name = "bosses_killed")
    private Integer bossesKilled;

    @Column(name = "death_cause")
    private DeathCause deathCause;

    @Column(name = "death_boss_seq")
    private Integer deathBossSeq;

    @Column(name = "coins_awarded")
    private Integer coinsAwarded;

    @Column(name = "is_silver_newly_unlocked")
    private Boolean silverNewlyUnlocked;

    protected Run() {
    }

    public Run(Long playerId, Tier tier, BuffItem equippedBuff, String boardVersion, Instant startedAt) {
        this.playerId = playerId;
        this.tier = tier;
        this.equippedBuff = equippedBuff;
        this.boardVersion = boardVersion;
        this.startedAt = startedAt;
        this.runState = RunState.OPEN;
        markCreated(startedAt, playerId);
    }

    public void markAbandoned(Instant now) {
        this.runState = RunState.ABANDONED;
        this.settledAt = now;
        markUpdated(now, playerId);
    }

    public void markCheckFailed(Instant now) {
        this.runState = RunState.CHECK_FAILED;
        this.settledAt = now;
        markUpdated(now, playerId);
    }

    public void markSettled(RunResult result, int coinsAwarded, boolean silverNewlyUnlocked, Instant now) {
        this.runState = RunState.SETTLED;
        this.settledAt = now;
        this.gameSeconds = result.gameSeconds();
        this.victory = result.victory();
        this.bossesKilled = result.bossesKilled();
        this.deathCause = result.deathCause();
        this.deathBossSeq = result.deathBossSeq();
        this.coinsAwarded = coinsAwarded;
        this.silverNewlyUnlocked = silverNewlyUnlocked;
        markUpdated(now, playerId);
    }

    /**
     * Reported outcome of a run, stored on settlement.
     */
    public record RunResult(int gameSeconds, boolean victory, int bossesKilled, DeathCause deathCause,
                            Integer deathBossSeq) {
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Tier getTier() {
        return tier;
    }

    public BuffItem getEquippedBuff() {
        return equippedBuff;
    }

    public String getBoardVersion() {
        return boardVersion;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public RunState getRunState() {
        return runState;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public Integer getGameSeconds() {
        return gameSeconds;
    }

    public Boolean getVictory() {
        return victory;
    }

    public Integer getBossesKilled() {
        return bossesKilled;
    }

    public DeathCause getDeathCause() {
        return deathCause;
    }

    public Integer getDeathBossSeq() {
        return deathBossSeq;
    }

    public Integer getCoinsAwarded() {
        return coinsAwarded;
    }

    public Boolean getSilverNewlyUnlocked() {
        return silverNewlyUnlocked;
    }
}
