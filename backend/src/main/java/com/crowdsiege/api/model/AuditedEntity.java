package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Audit and soft-delete columns shared by mutable tables (erm.dbml soft-delete convention).
 */
@MappedSuperclass
public abstract class AuditedEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    protected void markCreated(Instant now, Long actorId) {
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public void markUpdated(Instant now, Long actorId) {
        this.updatedAt = now;
        this.updatedBy = actorId;
    }

    public void markDeleted(Instant now, Long actorId) {
        this.deleted = true;
        markUpdated(now, actorId);
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

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
