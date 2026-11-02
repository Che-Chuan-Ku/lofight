package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Audit columns of append-only tables (exempt from updated_by / is_deleted per erm.dbml).
 */
@MappedSuperclass
public abstract class AppendOnlyEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    protected void markCreated(Instant now, Long actorId) {
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
