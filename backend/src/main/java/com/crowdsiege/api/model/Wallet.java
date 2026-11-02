package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "wallets")
@SQLRestriction("is_deleted = false")
public class Wallet extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "coins", nullable = false)
    private int coins;

    protected Wallet() {
    }

    public Wallet(Long playerId, int coins, Instant now) {
        this.playerId = playerId;
        this.coins = coins;
        markCreated(now, playerId);
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public int getCoins() {
        return coins;
    }
}
