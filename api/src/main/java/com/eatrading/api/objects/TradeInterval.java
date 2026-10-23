package com.eatrading.api.objects;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TradeInterval {
    HOUR, DAY, WEEK;

    @JsonValue
    public String getValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Instant bucketStart(Instant timestamp) {
        return switch (this) {
            case HOUR -> timestamp.truncatedTo(ChronoUnit.HOURS);
            case DAY -> timestamp.truncatedTo(ChronoUnit.DAYS);
            case WEEK -> timestamp.atZone(ZoneOffset.UTC)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .truncatedTo(ChronoUnit.DAYS).toInstant();
        };
    }

    public Instant nextBucket(Instant timestamp) {
        return switch (this) {
            case HOUR -> timestamp.plus(1, ChronoUnit.HOURS);
            case DAY -> timestamp.plus(1, ChronoUnit.DAYS);
            case WEEK -> timestamp.plus(7, ChronoUnit.DAYS);
        };
    }
}
