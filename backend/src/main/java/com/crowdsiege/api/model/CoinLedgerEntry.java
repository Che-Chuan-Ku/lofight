package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Append-only coin ledger; created_at is the occurred time.
 */
@Entity
@Table(name = "coin_ledger_entries")
public class CoinLedgerEntry extends AppendOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "ledger_source", nullable = false)
    private LedgerSource ledgerSource;

    @Column(name = "coin_delta", nullable = false)
    private int coinDelta;

    @Column(name = "coins_after", nullable = false)
    private int coinsAfter;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "boss_seq")
    private Integer bossSeq;

    @Column(name = "buff_item")
    private BuffItem buffItem;

    @Column(name = "merged_from_player_id")
    private Long mergedFromPlayerId;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    protected CoinLedgerEntry() {
    }

    private CoinLedgerEntry(Long playerId, LedgerSource ledgerSource, int coinDelta, int coinsAfter, Instant now) {
        this.playerId = playerId;
        this.ledgerSource = ledgerSource;
        this.coinDelta = coinDelta;
        this.coinsAfter = coinsAfter;
        markCreated(now, playerId);
    }

    public static CoinLedgerEntry bossReward(Long playerId, Long runId, int bossSeq, int coins, int coinsAfter,
                                             Instant now) {
        CoinLedgerEntry entry = new CoinLedgerEntry(playerId, LedgerSource.BOSS_REWARD, coins, coinsAfter, now);
        entry.runId = runId;
        entry.bossSeq = bossSeq;
        return entry;
    }

    public static CoinLedgerEntry mergeIn(Long playerId, Long guestId, int coins, int coinsAfter, Instant now) {
        CoinLedgerEntry entry = new CoinLedgerEntry(playerId, LedgerSource.MERGE_IN, coins, coinsAfter, now);
        entry.mergedFromPlayerId = guestId;
        return entry;
    }

    public static CoinLedgerEntry buffPurchase(Long playerId, BuffItem buffItem, int price, int coinsAfter,
                                               Instant now) {
        CoinLedgerEntry entry = new CoinLedgerEntry(playerId, LedgerSource.BUFF_PURCHASE, -price, coinsAfter, now);
        entry.buffItem = buffItem;
        return entry;
    }

    /**
     * AD_REWARD credit; deviceId is the client device id kept for audit only (AMB-A5=a, INF-A14).
     */
    public static CoinLedgerEntry adReward(Long playerId, String deviceId, int coins, int coinsAfter, Instant now) {
        CoinLedgerEntry entry = new CoinLedgerEntry(playerId, LedgerSource.AD_REWARD, coins, coinsAfter, now);
        entry.deviceId = deviceId;
        return entry;
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public LedgerSource getLedgerSource() {
        return ledgerSource;
    }

    public int getCoinDelta() {
        return coinDelta;
    }

    public int getCoinsAfter() {
        return coinsAfter;
    }

    public Long getRunId() {
        return runId;
    }

    public Integer getBossSeq() {
        return bossSeq;
    }

    public BuffItem getBuffItem() {
        return buffItem;
    }

    public Long getMergedFromPlayerId() {
        return mergedFromPlayerId;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
