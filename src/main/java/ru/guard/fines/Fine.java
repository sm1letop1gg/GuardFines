package ru.guard.fines;

import java.util.UUID;

public final class Fine {
    private final long id;
    private final UUID issuerId;
    private final String issuerName;
    private final UUID targetId;
    private final String targetName;
    private final double amount;
    private final String reason;
    private final long createdAt;
    private final long deadline;
    private FineStatus status;
    private long closedAt;

    public Fine(long id, UUID issuerId, String issuerName, UUID targetId, String targetName,
                double amount, String reason, long createdAt, long deadline, FineStatus status, long closedAt) {
        this.id = id;
        this.issuerId = issuerId;
        this.issuerName = issuerName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.amount = amount;
        this.reason = reason;
        this.createdAt = createdAt;
        this.deadline = deadline;
        this.status = status;
        this.closedAt = closedAt;
    }

    public void updateOverdue(long now) {
        if (status == FineStatus.UNPAID && now > deadline) status = FineStatus.OVERDUE;
    }

    public void close(FineStatus newStatus, long now) {
        status = newStatus;
        closedAt = now;
    }

    public long id() { return id; }
    public UUID issuerId() { return issuerId; }
    public String issuerName() { return issuerName; }
    public UUID targetId() { return targetId; }
    public String targetName() { return targetName; }
    public double amount() { return amount; }
    public String reason() { return reason; }
    public long createdAt() { return createdAt; }
    public long deadline() { return deadline; }
    public FineStatus status() { return status; }
    public long closedAt() { return closedAt; }
}
