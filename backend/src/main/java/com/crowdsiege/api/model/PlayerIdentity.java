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
@Table(name = "player_identities")
@SQLRestriction("is_deleted = false")
public class PlayerIdentity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "login_provider", nullable = false)
    private LoginProvider loginProvider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    protected PlayerIdentity() {
    }

    public PlayerIdentity(Long playerId, LoginProvider loginProvider, String providerSubject, Instant now) {
        this.playerId = playerId;
        this.loginProvider = loginProvider;
        this.providerSubject = providerSubject;
        markCreated(now, playerId);
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public LoginProvider getLoginProvider() {
        return loginProvider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }
}
