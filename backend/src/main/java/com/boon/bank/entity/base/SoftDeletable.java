package com.boon.bank.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
@SQLRestriction("deleted_at is null")
public abstract class SoftDeletable extends BaseEntity {

    @Column(name = "deleted_at")
    protected Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markDeleted() {
        this.deletedAt = Instant.now();
    }
}
