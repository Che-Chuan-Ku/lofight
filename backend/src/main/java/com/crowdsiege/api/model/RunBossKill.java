package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "run_boss_kills")
public class RunBossKill extends AppendOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "boss_seq", nullable = false)
    private int bossSeq;

    @Column(name = "killed_at_second", nullable = false)
    private int killedAtSecond;

    @Column(name = "coins_awarded", nullable = false)
    private int coinsAwarded;

    protected RunBossKill() {
    }

    public RunBossKill(Long runId, int bossSeq, int killedAtSecond, int coinsAwarded, Long actorId, Instant now) {
        this.runId = runId;
        this.bossSeq = bossSeq;
        this.killedAtSecond = killedAtSecond;
        this.coinsAwarded = coinsAwarded;
        markCreated(now, actorId);
    }

    public Long getId() {
        return id;
    }

    public Long getRunId() {
        return runId;
    }

    public int getBossSeq() {
        return bossSeq;
    }

    public int getKilledAtSecond() {
        return killedAtSecond;
    }

    public int getCoinsAwarded() {
        return coinsAwarded;
    }
}
