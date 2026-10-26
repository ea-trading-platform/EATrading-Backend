package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.objects.Status;
import com.eatrading.api.objects.TradeInterval;
import com.eatrading.api.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTradingActivityTest {
    @Mock
    private OrderRepository repository;

    private final UUID clientId = UUID.randomUUID();

    @Test
    void countsEveryCompletionOnceAndZeroFillsDaysInChronologicalOrder() {
        Instant from = Instant.parse("2026-10-16T00:00:00Z");
        Instant to = Instant.parse("2026-10-19T00:00:00Z");
        when(repository.findCompletionTimes(clientId, Status.FILLED, Status.FILLED, from, to)).thenReturn(List.of(
                Instant.parse("2026-10-18T20:00:00Z"), from, from));

        var response = new OrderService(repository).getTradingActivity(clientId, from, to, TradeInterval.DAY);

        assertEquals(clientId, response.getClientId());
        assertEquals(from, response.getFrom());
        assertEquals(to, response.getTo());
        assertEquals(TradeInterval.DAY, response.getInterval());
        assertEquals(List.of(2L, 0L, 1L),
                response.getPoints().stream().map(point -> point.getTradeCount()).toList());
        assertEquals(List.of(from, from.plusSeconds(86400), from.plusSeconds(172800)),
                response.getPoints().stream().map(point -> point.getTimestamp()).toList());
    }

    @Test
    void hourlyBucketsIncludePartialEdgesAndExcludeBucketStartingAtTo() {
        Instant from = Instant.parse("2026-10-16T23:30:00Z");
        Instant to = Instant.parse("2026-10-17T02:00:00Z");
        when(repository.findCompletionTimes(clientId, Status.FILLED, Status.FILLED, from, to)).thenReturn(List.of(
                from, Instant.parse("2026-10-17T00:00:00Z")));

        var points = new OrderService(repository)
                .getTradingActivity(clientId, from, to, TradeInterval.HOUR).getPoints();

        assertEquals(List.of(
                Instant.parse("2026-10-16T23:00:00Z"),
                Instant.parse("2026-10-17T00:00:00Z"),
                Instant.parse("2026-10-17T01:00:00Z")),
                points.stream().map(point -> point.getTimestamp()).toList());
        assertEquals(List.of(1L, 1L, 0L), points.stream().map(point -> point.getTradeCount()).toList());
    }

    @Test
    void weeklyBucketsStartOnMondayAndCrossTheYearBoundary() {
        Instant from = Instant.parse("2026-12-31T12:00:00Z");
        Instant to = Instant.parse("2027-01-11T06:00:00Z");
        when(repository.findCompletionTimes(clientId, Status.FILLED, Status.FILLED, from, to)).thenReturn(List.of(
                Instant.parse("2027-01-03T23:59:59Z"),
                Instant.parse("2027-01-04T00:00:00Z")));

        var points = new OrderService(repository)
                .getTradingActivity(clientId, from, to, TradeInterval.WEEK).getPoints();

        assertEquals(List.of(
                Instant.parse("2026-12-28T00:00:00Z"),
                Instant.parse("2027-01-04T00:00:00Z"),
                Instant.parse("2027-01-11T00:00:00Z")),
                points.stream().map(point -> point.getTimestamp()).toList());
        assertEquals(List.of(1L, 1L, 0L), points.stream().map(point -> point.getTradeCount()).toList());
    }

    @Test
    void emptyRangeOfTradesStillProducesEveryDailyBucketIncludingLeapDay() {
        Instant from = Instant.parse("2028-02-28T00:00:00Z");
        Instant to = Instant.parse("2028-03-02T00:00:00Z");
        when(repository.findCompletionTimes(clientId, Status.FILLED, Status.FILLED, from, to)).thenReturn(List.of());

        var points = new OrderService(repository)
                .getTradingActivity(clientId, from, to, TradeInterval.DAY).getPoints();

        assertEquals(3, points.size());
        assertEquals(Instant.parse("2028-02-29T00:00:00Z"), points.get(1).getTimestamp());
        assertEquals(List.of(0L, 0L, 0L), points.stream().map(point -> point.getTradeCount()).toList());
    }

    @Test
    void rejectsEqualAndReversedRangesBeforeQuerying() {
        Instant from = Instant.parse("2026-10-16T00:00:00Z");
        OrderService service = new OrderService(repository);

        assertThrows(IllegalArgumentException.class,
                () -> service.getTradingActivity(clientId, from, from, TradeInterval.DAY));
        assertThrows(IllegalArgumentException.class,
                () -> service.getTradingActivity(clientId, from, from.minusSeconds(1), TradeInterval.DAY));
        verifyNoInteractions(repository);
    }
}
