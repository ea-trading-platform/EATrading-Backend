package com.eatrading.api.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.eatrading.api.dto.TradingActivityResponse;
import com.eatrading.api.entities.Client;
import com.eatrading.api.exception.GlobalExceptionHandler;
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.objects.Role;
import com.eatrading.api.objects.TradeInterval;
import com.eatrading.api.services.AuthService;
import com.eatrading.api.services.ClientService;
import com.eatrading.api.services.OrderService;

@ExtendWith(MockitoExtension.class)
class OrdersTradingActivityTest {
    @Mock
    private OrderService orderService;
    @Mock
    private Producers producers;
    @Mock
    private AuthService authService;
    @Mock
    private ClientService clientService;
    @Mock
    private Client client;

    private MockMvc mvc;
    private final UUID clientId = UUID.randomUUID();
    private final String clientEmail = "jane@example.com";
    private final Instant from = Instant.parse("2026-10-16T00:00:00Z");
    private final Instant to = Instant.parse("2026-10-19T00:00:00Z");

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new OrdersController(orderService, producers, authService, clientService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void adminRequestReturnsExactChartShapeAndNormalizesInterval() throws Exception {
        when(clientService.getClientByEmail(clientEmail)).thenReturn(Optional.of(client));
        when(client.getId()).thenReturn(clientId);
        when(orderService.getTradingActivity(clientId, from, to, TradeInterval.DAY))
                .thenReturn(new TradingActivityResponse(clientId, from, to, TradeInterval.DAY, List.of(
                        new TradingActivityResponse.Point(from, 12),
                        new TradingActivityResponse.Point(from.plusSeconds(86400), 0),
                        new TradingActivityResponse.Point(from.plusSeconds(172800), 7))));

        mvc.perform(get("/api/orders/trading-activity")
                .param("clientEmail", "  " + clientEmail + "  ")
                .param("from", from.toString()).param("to", to.toString())
                .param("interval", "DAY").requestAttr("role", Role.ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.clientId").value(clientId.toString()))
                .andExpect(jsonPath("$.from").value(from.toString()))
                .andExpect(jsonPath("$.to").value(to.toString()))
                .andExpect(jsonPath("$.interval").value("day"))
                .andExpect(jsonPath("$.points.length()").value(3))
                .andExpect(jsonPath("$.points[0].length()").value(2))
                .andExpect(jsonPath("$.points[0].timestamp").value(from.toString()))
                .andExpect(jsonPath("$.points[0].tradeCount").value(12))
                .andExpect(jsonPath("$.points[1].tradeCount").value(0))
                .andExpect(jsonPath("$.points[2].tradeCount").value(7));
    }

    @Test
    void unknownClientEmailReturnsNotFoundWithoutQueryingActivity() throws Exception {
        when(clientService.getClientByEmail(clientEmail)).thenReturn(Optional.empty());

        mvc.perform(request("day").requestAttr("role", Role.ADMIN)).andExpect(status().isNotFound());

        verifyNoInteractions(orderService);
    }

    @Test
    void clientsAndRequestsWithoutAnAdminRoleCannotQueryActivity() throws Exception {
        mvc.perform(request("day").requestAttr("role", Role.CLIENT)).andExpect(status().isForbidden());
        mvc.perform(request("day")).andExpect(status().isForbidden());
        verifyNoInteractions(orderService, clientService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"month", "", "1D"})
    void rejectsUnsupportedIntervals(String interval) throws Exception {
        mvc.perform(request(interval).requestAttr("role", Role.ADMIN)).andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    @Test
    void rejectsEqualAndReversedDates() throws Exception {
        for (String end : List.of(from.toString(), from.minusSeconds(1).toString())) {
            var request = get("/api/orders/trading-activity")
                    .param("clientEmail", clientEmail).param("from", from.toString())
                    .param("to", end).param("interval", "day").requestAttr("role", Role.ADMIN);
            mvc.perform(request).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(orderService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"clientEmail", "from", "to", "interval"})
    void requiresEveryQueryParameter(String omitted) throws Exception {
        var request = get("/api/orders/trading-activity").requestAttr("role", Role.ADMIN);
        if (!omitted.equals("clientEmail")) {
            request.param("clientEmail", clientEmail);
        }
        if (!omitted.equals("from")) {
            request.param("from", from.toString());
        }
        if (!omitted.equals("to")) {
            request.param("to", to.toString());
        }
        if (!omitted.equals("interval")) {
            request.param("interval", "day");
        }
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    @Test
    void rejectsMalformedDate() throws Exception {
        mvc.perform(get("/api/orders/trading-activity").requestAttr("role", Role.ADMIN)
                .param("clientEmail", clientEmail).param("from", "invalid")
                .param("to", to.toString()).param("interval", "day"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    private MockHttpServletRequestBuilder request(String interval) {
        return get("/api/orders/trading-activity")
                .param("clientEmail", clientEmail).param("from", from.toString())
                .param("to", to.toString()).param("interval", interval);
    }
}
