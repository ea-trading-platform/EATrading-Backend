package com.eatrading.api.services;

import java.time.Duration;

import com.eatrading.api.dto.CandlesResponse;

final class CachedCandles {
    final CandlesResponse response;
    private final long timestamp;

    CachedCandles(CandlesResponse response, long timestamp) {
        this.response = response;
        this.timestamp = timestamp;
    }

    boolean isExpired(long ttlSeconds) {
        return Duration.ofMillis(System.currentTimeMillis() - timestamp)
                .compareTo(Duration.ofSeconds(ttlSeconds)) > 0;
    }
}
