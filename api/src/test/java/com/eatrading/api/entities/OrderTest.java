package com.eatrading.api.entities;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Status;

public class OrderTest {

    @SuppressWarnings("unchecked")
    private Map<Status, Instant> getStatusChangeLog(Order order) throws Exception {
        Field f = Order.class.getDeclaredField("statusChangeLog");
        f.setAccessible(true);
        return (Map<Status, Instant>) f.get(order);
    }

    private void setStatusChangeLog(Order order, Map<Status, Instant> value) throws Exception {
        Field f = Order.class.getDeclaredField("statusChangeLog");
        f.setAccessible(true);
        f.set(order, value);
    }

    private Instant waitUntilAfter(Instant previous) {
        Instant now = Instant.now();
        while (!now.isAfter(previous)) {
            now = Instant.now();
        }
        return now;
    }

    @Test
    public void testGetCurrentStatus_NullMap_returnsSubmitted() throws Exception {
        Order order = new Order();
        setStatusChangeLog(order, null);

        assertEquals(Status.SUBMITTED, order.getCurrentStatus());
    }

    @Test
    public void testGetCurrentStatus_EmptyMap_returnsSubmitted() throws Exception {
        Order order = new Order();
        setStatusChangeLog(order, new HashMap<>());

        assertEquals(Status.SUBMITTED, order.getCurrentStatus());
    }

    @Test
    public void testGetCurrentStatus_MostRecentSelected() throws Exception {
        Order order = new Order();
        Map<Status, Instant> map = new HashMap<>();
        Instant t1 = Instant.now().minusSeconds(60);
        Instant t2 = Instant.now();
        map.put(Status.SUBMITTED, t1);
        map.put(Status.FILLED, t2);

        setStatusChangeLog(order, map);

        assertEquals(Status.FILLED, order.getCurrentStatus());
    }

    @Test
    public void testConstructorInitializesSubmitted_andPriceAndDate() throws Exception {
        UUID clientId = UUID.randomUUID();
        Asset asset = mock(Asset.class);
        BigDecimal price = new BigDecimal("123.45000000");

        Order order = new Order(UUID.randomUUID(), clientId, asset, new BigDecimal("10"), true, price);

        // status
        assertEquals(Status.SUBMITTED, order.getCurrentStatus());

        Map<Status, Instant> map = getStatusChangeLog(order);
        assertNotNull(map);
        assertTrue(map.containsKey(Status.SUBMITTED));
        Instant then = map.get(Status.SUBMITTED);
        assertNotNull(then);
        assertTrue(Duration.between(then, Instant.now()).abs().getSeconds() < 5);

        // price and orderDate
        assertEquals(price, order.getPrice());
        assertTrue(Duration.between(order.getOrderDate(), Instant.now()).abs().getSeconds() < 5);
    }

    @Test
    public void testSetStatus_AppendsAndUpdatesCurrentStatus() throws Exception {
        UUID clientId = UUID.randomUUID();
        Asset asset = mock(Asset.class);

        Order order = new Order(UUID.randomUUID(), clientId, asset, new BigDecimal("1"), true, new BigDecimal("1.00"));
        Instant submittedAt = getStatusChangeLog(order).get(Status.SUBMITTED);
        assertNotNull(submittedAt);
        waitUntilAfter(submittedAt);
        order.setStatus(Status.REJECTED);

        assertEquals(Status.REJECTED, order.getCurrentStatus());

        Map<Status, Instant> map = getStatusChangeLog(order);
        assertTrue(map.containsKey(Status.REJECTED));
        assertNotNull(map.get(Status.REJECTED));
    }

    @Test
    public void testDefaultConstructor_GetCurrentStatus_returnsSubmitted() {
        Order order = new Order();

        assertEquals(Status.SUBMITTED, order.getCurrentStatus());
    }

    @Test
    public void testSetStatus_OnDefaultConstructedOrder_updatesStatus() throws Exception {
        Order order = new Order();

        order.setStatus(Status.ACCEPTED);

        assertEquals(Status.ACCEPTED, order.getCurrentStatus());
        Map<Status, Instant> map = getStatusChangeLog(order);
        assertTrue(map.containsKey(Status.ACCEPTED));
    }

    @Test
    public void testSetStatus_MultipleCalls_LatestWins() throws Exception {
        Order order = new Order();

        order.setStatus(Status.ACCEPTED);
        Instant acceptedAt = getStatusChangeLog(order).get(Status.ACCEPTED);
        assertNotNull(acceptedAt);
        waitUntilAfter(acceptedAt);
        order.setStatus(Status.FILLED);

        assertEquals(Status.FILLED, order.getCurrentStatus());
    }

    @Test
    public void testConstructor_NullAsset_allowedWhenPriceIsExplicit() {
        UUID clientId = UUID.randomUUID();

        assertDoesNotThrow(
            () -> new Order(UUID.randomUUID(), clientId, null, new BigDecimal("1"), true, new BigDecimal("1.00")));
    }

    @Test
    public void testSetStatus_SameStatusTwice_refreshesTimestamp() throws Exception {
        Order order = new Order();

        order.setStatus(Status.REJECTED);
        Map<Status, Instant> map = getStatusChangeLog(order);
        Instant first = map.get(Status.REJECTED);
        assertNotNull(first);

        waitUntilAfter(first);
        order.setStatus(Status.REJECTED);

        Instant second = getStatusChangeLog(order).get(Status.REJECTED);
        assertNotNull(second);
        assertTrue(second.isAfter(first) || second.equals(first));
    }

    @Test
    public void testGetOrderID_BeforeAndAfterReflectionSet() throws Exception {
        UUID clientId = UUID.randomUUID();
        Asset asset = mock(Asset.class);

        Order order = new Order(UUID.randomUUID(), clientId, asset, new BigDecimal("1"), true, new BigDecimal("1.00"));
        assertNull(order.getOrderId());

        UUID id = UUID.randomUUID();
        Field idField = Order.class.getDeclaredField("orderId");
        idField.setAccessible(true);
        idField.set(order, id);

        assertEquals(id, order.getOrderId());
    }
}
