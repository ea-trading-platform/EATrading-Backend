package com.eatrading.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import com.eatrading.api.dto.OrderTransactionResponse;
import com.eatrading.api.dto.OrderAnalyticsResponse;
import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.Order;
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.Role;
import com.eatrading.api.objects.Status;
import com.eatrading.api.services.AuthService;
import com.eatrading.api.services.ClientService;
import com.eatrading.api.services.OrderService;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OrdersControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private Producers producers;

    @Mock
    private AuthService authService;

    @Mock
    private ClientService clientService;

    @Mock
    private Order order;

    @Mock
    private Asset asset;

    @Mock
    private Client client;

    private OrdersController controller;
    private UUID orderId;
    private UUID trackingId;
    private UUID clientId;
    private Instant orderDate;

    @BeforeEach
    void setUp() {
        controller = new OrdersController(orderService, producers, authService, clientService);

        orderId = UUID.randomUUID();
        trackingId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        orderDate = Instant.parse("2026-01-02T03:04:05Z");
    }

    @Test
    void getAllOrdersAsAdminReturnsOrderTransactionResponses() {
        stubOrderResponse();
        when(orderService.getAllOrders()).thenReturn(List.of(order));

        var response = controller.getAllOrders(requestWithRole(Role.ADMIN));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertOrderResponse(response.getBody().get(0), "BUY");
        assertInstanceOf(OrderTransactionResponse.class, response.getBody().get(0));
        verify(orderService).getAllOrders();
    }

    @Test
    void orderListJsonMatchesOrderTransactionResponseContract() {
        stubOrderResponse();
        when(orderService.getAllOrders()).thenReturn(List.of(order));

        var response = controller.getAllOrders(requestWithRole(Role.ADMIN));
        var mapper = JsonMapper.builder().build();

        var expected = mapper.readTree("""
                [{
                    "orderId": "%s",
                    "trackingId": "%s",
                    "clientId": "%s",
                    "symbol": "AAPL",
                    "instrument": "EQUITY",
                    "transactionType": "BUY",
                    "quantity": "2.50000000",
                    "price": "238.45000000",
                    "status": "FILLED",
                    "orderDate": "2026-01-02T03:04:05Z"
                }]
                """.formatted(orderId, trackingId, clientId));
        assertEquals(expected, mapper.readTree(mapper.writeValueAsString(response.getBody())));
    }

    @Test
    void getOrdersByClientReturnsAuthorizedClientOrders() {
        stubOrderResponse();
        when(authService.canAccessResource(clientId, clientId)).thenReturn(true);
        when(orderService.getOrdersByClient(clientId)).thenReturn(List.of(order));

        var response = controller.getOrdersByClient(
                clientId.toString(), requestWithClientAndRole(clientId, Role.CLIENT));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertOrderResponse(response.getBody().get(0), "BUY");
        verify(orderService).getOrdersByClient(clientId);
    }

    @Test
    void getOrdersByClientRejectsAnotherClient() {
        UUID authenticatedClientId = UUID.randomUUID();
        when(authService.canAccessResource(authenticatedClientId, clientId)).thenReturn(false);

        var response = controller.getOrdersByClient(
                clientId.toString(), requestWithClientAndRole(authenticatedClientId, Role.CLIENT));

        assertEquals(403, response.getStatusCode().value());
        verify(orderService, never()).getOrdersByClient(clientId);
    }

    @Test
    void getOrdersByClientEmailTrimsEmailAndReturnsOrders() {
        stubOrderResponse();
        when(client.getId()).thenReturn(clientId);
        when(clientService.getClientByEmail("jane@example.com")).thenReturn(Optional.of(client));
        when(orderService.getOrdersByClient(clientId)).thenReturn(List.of(order));

        var response = controller.getOrdersByClientEmail(
                "  jane@example.com  ", requestWithRole(Role.ADMIN));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertOrderResponse(response.getBody().get(0), "BUY");
        verify(clientService).getClientByEmail("jane@example.com");
        verify(orderService).getOrdersByClient(clientId);
    }

    @Test
    void getOrdersByClientEmailReturnsNotFoundForUnknownClient() {
        when(clientService.getClientByEmail("missing@example.com")).thenReturn(Optional.empty());

        var response = controller.getOrdersByClientEmail(
                "missing@example.com", requestWithRole(Role.ADMIN));

        assertEquals(404, response.getStatusCode().value());
        verify(orderService, never()).getOrdersByClient(clientId);
    }

    @Test
    void getOrdersByInstrumentReturnsInstrumentOrders() {
        stubOrderResponse();
        when(orderService.getOrdersByInstrument(Instrument.EQUITY)).thenReturn(List.of(order));

        var response = controller.getOrdersByInstrument(
                Instrument.EQUITY, requestWithRole(Role.ADMIN));

        assertEquals(200, response.getStatusCode().value());
        assertOrderResponse(response.getBody().get(0), "BUY");
        verify(orderService).getOrdersByInstrument(Instrument.EQUITY);
    }

    @Test
    void getOrdersBySideSupportsSell() {
        stubOrderResponse();
        when(order.isBuy()).thenReturn(false);
        when(orderService.getOrdersBySide(false)).thenReturn(List.of(order));

        var response = controller.getOrdersBySide("sell", requestWithRole(Role.ADMIN));

        assertEquals(200, response.getStatusCode().value());
        assertOrderResponse(response.getBody().get(0), "SELL");
        verify(orderService).getOrdersBySide(false);
    }

    @Test
    void getOrdersBySideRejectsInvalidSide() {
        var response = controller.getOrdersBySide("hold", requestWithRole(Role.ADMIN));

        assertEquals(400, response.getStatusCode().value());
        verify(orderService, never()).getOrdersBySide(true);
        verify(orderService, never()).getOrdersBySide(false);
    }

    @Test
    void getOrdersByStatusNormalizesStatus() {
        stubOrderResponse();
        when(orderService.getOrdersByStatus(Status.ACCEPTED)).thenReturn(List.of(order));

        var response = controller.getOrdersByStatus(
                " accepted ", requestWithRole(Role.ADMIN));

        assertEquals(200, response.getStatusCode().value());
        assertOrderResponse(response.getBody().get(0), "BUY");
        verify(orderService).getOrdersByStatus(Status.ACCEPTED);
    }

    @Test
    void getOrdersByStatusRejectsInvalidStatus() {
        var response = controller.getOrdersByStatus(
                "not-a-status", requestWithRole(Role.ADMIN));

        assertEquals(400, response.getStatusCode().value());
        verify(orderService, never()).getOrdersByStatus(Status.ACCEPTED);
    }

    @Test
    void getOrdersByDateRangeReturnsOrdersForHalfOpenRange() {
        stubOrderResponse();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");
        when(orderService.getOrdersByDateRange(from, to)).thenReturn(List.of(order));

        var response = controller.getOrdersByDateRange(
                from, to, requestWithRole(Role.ADMIN));

        assertEquals(200, response.getStatusCode().value());
        assertOrderResponse(response.getBody().get(0), "BUY");
        verify(orderService).getOrdersByDateRange(from, to);
    }

    @Test
    void getOrdersByDateRangeRejectsReversedRange() {
        Instant from = Instant.parse("2026-01-02T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z");

        var response = controller.getOrdersByDateRange(
                from, to, requestWithRole(Role.ADMIN));

        assertEquals(400, response.getStatusCode().value());
        verify(orderService, never()).getOrdersByDateRange(from, to);
    }

    @Test
    void getOrderAnalyticsReturnsComposableFilteredAnalyticsForAdmin() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");
        OrderAnalyticsResponse analytics = new OrderAnalyticsResponse();
        analytics.setTotalOrders(4);
        when(orderService.getOrderAnalytics(
                from, to, Instrument.EQUITY, true, Status.FILLED)).thenReturn(analytics);

        var response = controller.getOrderAnalytics(
                from, to, Instrument.EQUITY, "buy", "filled", requestWithRole(Role.ADMIN));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(analytics, response.getBody());
        verify(orderService).getOrderAnalytics(
                from, to, Instrument.EQUITY, true, Status.FILLED);
    }

    @Test
    void getOrderAnalyticsRejectsInvalidSide() {
        var response = controller.getOrderAnalytics(
                null, null, null, "hold", null, requestWithRole(Role.ADMIN));

        assertEquals(400, response.getStatusCode().value());
        verify(orderService, never()).getOrderAnalytics(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void adminOnlyQueriesRejectNonAdminUsers() {
        MockHttpServletRequest request = requestWithRole(Role.CLIENT);

        assertEquals(403, controller.getAllOrders(request).getStatusCode().value());
        assertEquals(403, controller.getOrdersByClientEmail("jane@example.com", request)
                .getStatusCode().value());
        assertEquals(403, controller.getOrdersByInstrument(Instrument.EQUITY, request)
                .getStatusCode().value());
        assertEquals(403, controller.getOrdersBySide("BUY", request).getStatusCode().value());
        assertEquals(403, controller.getOrdersByStatus("FILLED", request).getStatusCode().value());
        assertEquals(403, controller.getOrdersByDateRange(
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                request).getStatusCode().value());
        assertEquals(403, controller.getOrderAnalytics(
                null, null, null, null, null, request).getStatusCode().value());
    }

    private void assertOrderResponse(OrderTransactionResponse response, String transactionType) {
        assertEquals(orderId.toString(), response.getOrderId());
        assertEquals(trackingId.toString(), response.getTrackingId());
        assertEquals(clientId.toString(), response.getClientId());
        assertEquals("AAPL", response.getSymbol());
        assertEquals("EQUITY", response.getInstrument());
        assertEquals(transactionType, response.getTransactionType());
        assertEquals("2.50000000", response.getQuantity());
        assertEquals("238.45000000", response.getPrice());
        assertEquals("FILLED", response.getStatus());
        assertEquals(orderDate.toString(), response.getOrderDate());
    }

    private void stubOrderResponse() {
        when(order.getOrderId()).thenReturn(orderId);
        when(order.getTrackingId()).thenReturn(trackingId);
        when(order.getClientId()).thenReturn(clientId);
        when(order.getAsset()).thenReturn(asset);
        when(asset.getSymbol()).thenReturn("AAPL");
        when(asset.getInstrument()).thenReturn(Instrument.EQUITY);
        when(order.isBuy()).thenReturn(true);
        when(order.getQuantity()).thenReturn(new BigDecimal("2.50000000"));
        when(order.getPrice()).thenReturn(new BigDecimal("238.45000000"));
        when(order.getCurrentStatus()).thenReturn(Status.FILLED);
        when(order.getOrderDate()).thenReturn(orderDate);
    }

    private MockHttpServletRequest requestWithRole(Role role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("role", role);
        return request;
    }

    private MockHttpServletRequest requestWithClientAndRole(UUID authenticatedClientId, Role role) {
        MockHttpServletRequest request = requestWithRole(role);
        request.setAttribute("clientId", authenticatedClientId);
        return request;
    }
}
