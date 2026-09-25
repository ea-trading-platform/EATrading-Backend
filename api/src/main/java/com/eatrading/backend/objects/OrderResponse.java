package com.eatrading.api.Objects;

public class OrderResponse {
    Status statusCode;
    String rejectionReason;
    // Trade trade;

    public OrderResponse() {
        this.statusCode = Status.SUBMITTED;
        this.rejectionReason = "";
    }

    public Status getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Status statusCode) {
        this.statusCode = statusCode;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
