package com.eatrading.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.eatrading.api.objects.TradeInterval;

public class TradingActivityResponse {
    private final UUID clientId;
    private final Instant from;
    private final Instant to;
    private final TradeInterval interval;
    private final List<Point> points;

    public TradingActivityResponse(UUID clientId, Instant from, Instant to,
            TradeInterval interval, List<Point> points) {
        this.clientId = clientId;
        this.from = from;
        this.to = to;
        this.interval = interval;
        this.points = List.copyOf(points);
    }

    public UUID getClientId() {
        return clientId;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

    public TradeInterval getInterval() {
        return interval;
    }

    public List<Point> getPoints() {
        return points;
    }

    public static class Point {
        private final Instant timestamp;
        private final long tradeCount;

        public Point(Instant timestamp, long tradeCount) {
            this.timestamp = timestamp;
            this.tradeCount = tradeCount;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public long getTradeCount() {
            return tradeCount;
        }
    }
}
