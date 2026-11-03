package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Season reward payout record, one per (season, tier) (INF-S4); system batch record without audit actor columns.
 */
@Entity
@Table(name = "season_settlements")
public class SeasonSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_version", nullable = false, length = 16)
    private String boardVersion;

    @Column(name = "tier", nullable = false)
    private Tier tier;

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;

    @Column(name = "total_coins", nullable = false)
    private int totalCoins;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SeasonSettlement() {
    }

    public SeasonSettlement(String boardVersion, Tier tier, int recipientCount, int totalCoins, Instant settledAt) {
        this.boardVersion = boardVersion;
        this.tier = tier;
        this.recipientCount = recipientCount;
        this.totalCoins = totalCoins;
        this.settledAt = settledAt;
        this.createdAt = settledAt;
        this.updatedAt = settledAt;
    }

    public Long getId() {
        return id;
    }

    public String getBoardVersion() {
        return boardVersion;
    }

    public Tier getTier() {
        return tier;
    }

    public int getRecipientCount() {
        return recipientCount;
    }

    public int getTotalCoins() {
        return totalCoins;
    }

    public Instant getSettledAt() {
        return settledAt;
    }
}
