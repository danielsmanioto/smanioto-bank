package com.smanioto.bank.accounts.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movements")
public class Movement {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID accountId;

    @Column(nullable = false, updatable = false)
    private UUID transferId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private MovementType type;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 255, updatable = false)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Movement() {
    }

    public Movement(UUID id, UUID accountId, UUID transferId, MovementType type, BigDecimal amount, String description, Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.transferId = transferId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public MovementType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
