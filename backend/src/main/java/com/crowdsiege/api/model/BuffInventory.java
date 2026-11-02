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
@Table(name = "buff_inventories")
@SQLRestriction("is_deleted = false")
public class BuffInventory extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "buff_item", nullable = false)
    private BuffItem buffItem;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected BuffInventory() {
    }

    public BuffInventory(Long playerId, BuffItem buffItem, int quantity, Instant now) {
        this.playerId = playerId;
        this.buffItem = buffItem;
        this.quantity = quantity;
        markCreated(now, playerId);
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public BuffItem getBuffItem() {
        return buffItem;
    }

    public int getQuantity() {
        return quantity;
    }
}
