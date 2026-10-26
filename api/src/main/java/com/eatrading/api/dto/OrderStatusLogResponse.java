package com.eatrading.api.dto;

import java.time.Instant;

public class OrderStatusLogResponse {

    private final Instant statusChangeTime;
    private final String rejectionReason;

    public OrderStatusLogResponse(Instant statusChangeTime, String rejectionReason) {
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