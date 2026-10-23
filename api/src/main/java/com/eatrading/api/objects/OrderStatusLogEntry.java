package com.eatrading.api.objects;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderStatusLogEntry {
    @Column(name = "status_change_time")
    private Instant statusChangeTime;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    protected OrderStatusLogEntry() {}

    public OrderStatusLogEntry(Instant statusChangeTime, String rejectionReason) {
        this.statusChangeTime = statusChangeTime;
        this.rejectionReason = rejectionReason;
    }

    public Instant getStatusChangeTime() {
        return statusChangeTime;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
