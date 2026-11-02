package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Append-only backend event (shop view, buff purchase); created_at is the occurred time.
 */
@Entity
@Table(name = "player_events")
public class PlayerEvent extends AppendOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_kind", nullable = false)
    private EventKind eventKind;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "is_silver_unlocked", nullable = false)
    private boolean silverUnlocked;

    @Column(name = "buff_item")
    private BuffItem buffItem;

    @Column(name = "paid_coins")
    private Integer paidCoins;

    protected PlayerEvent() {
    }

    public PlayerEvent(EventKind eventKind, Long playerId, boolean silverUnlocked, BuffItem buffItem,
                       Integer paidCoins, Instant now) {
        this.eventKind = eventKind;
        this.playerId = playerId;
        this.silverUnlocked = silverUnlocked;
        this.buffItem = buffItem;
        this.paidCoins = paidCoins;
        markCreated(now, playerId);
    }

    public Long getId() {
        return id;
    }

    public EventKind getEventKind() {
        return eventKind;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public boolean isSilverUnlocked() {
        return silverUnlocked;
    }

    public BuffItem getBuffItem() {
        return buffItem;
    }

    public Integer getPaidCoins() {
        return paidCoins;
    }
}
