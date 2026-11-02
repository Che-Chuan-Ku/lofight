package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLRestriction;

/**
 * Player (guest or member). The id is drawn from players_id_seq before insert so created_by can reference itself.
 */
@Entity
@Table(name = "players")
@SQLRestriction("is_deleted = false")
public class Player extends AuditedEntity {

    @Id
    private Long id;

    @Column(name = "player_kind", nullable = false)
    private PlayerKind playerKind;

    @Column(name = "nickname", nullable = false, length = 12)
    private String nickname;

    @Column(name = "device_id", length = 64)
    private String deviceId;

    @Column(name = "is_silver_unlocked", nullable = false)
    private boolean silverUnlocked;

    @Column(name = "silver_unlocked_at")
    private Instant silverUnlockedAt;

    @Column(name = "is_voided", nullable = false)
    private boolean voided;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "merged_into_player_id")
    private Long mergedIntoPlayerId;

    protected Player() {
    }

    public Player(Long id, PlayerKind playerKind, String nickname, String deviceId, Instant now) {
        this.id = id;
        this.playerKind = playerKind;
        this.nickname = nickname;
        this.deviceId = deviceId;
        markCreated(now, id);
    }

    public boolean isGuest() {
        return playerKind == PlayerKind.GUEST;
    }

    public void unlockSilver(Instant now) {
        this.silverUnlocked = true;
        this.silverUnlockedAt = now;
        markUpdated(now, id);
    }

    public void voidInto(Long memberId, Instant now) {
        this.voided = true;
        this.voidedAt = now;
        this.mergedIntoPlayerId = memberId;
        markUpdated(now, memberId == null ? id : memberId);
    }

    public void rename(String nickname, Instant now) {
        this.nickname = nickname;
        markUpdated(now, id);
    }

    public Long getId() {
        return id;
    }

    public PlayerKind getPlayerKind() {
        return playerKind;
    }

    public String getNickname() {
        return nickname;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean isSilverUnlocked() {
        return silverUnlocked;
    }

    public Instant getSilverUnlockedAt() {
        return silverUnlockedAt;
    }

    public boolean isVoided() {
        return voided;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public Long getMergedIntoPlayerId() {
        return mergedIntoPlayerId;
    }
}
