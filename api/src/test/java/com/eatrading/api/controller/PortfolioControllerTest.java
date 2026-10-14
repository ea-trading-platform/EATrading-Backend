package com.eatrading.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import com.eatrading.api.dto.PortfolioHistoryResponse;
import com.eatrading.api.services.AuthService;
import com.eatrading.api.services.PortfolioHistoryService;
import com.eatrading.api.services.PortfolioService;

@ExtendWith(MockitoExtension.class)
class PortfolioControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private PortfolioHistoryService portfolioHistoryService;

    private PortfolioController controller;

    @BeforeEach
    void setUp() {
        controller = new PortfolioController(authService, portfolioService, portfolioHistoryService);
    }

    @Test
    void getPortfolioHistoryRejectsDifferentClient() {
        UUID requestedClientId = UUID.randomUUID();
        UUID authenticatedClientId = UUID.randomUUID();
        MockHttpServletRequest request = requestWithClient(authenticatedClientId);
        when(authService.canAccessResource(authenticatedClientId, requestedClientId)).thenReturn(false);

        var response = controller.getPortfolioHistory(
                requestedClientId.toString(),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                request);

        assertEquals(403, response.getStatusCode().value());
        verify(portfolioHistoryService, never()).getHistory(
                requestedClientId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2));
    }

    @Test
    void getPortfolioHistoryReturnsAuthorizedHistory() {
        UUID clientId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 2);
        MockHttpServletRequest request = requestWithClient(clientId);
        PortfolioHistoryResponse history = new PortfolioHistoryResponse(
                clientId, from, to, "1d", List.of());
        when(authService.canAccessResource(clientId, clientId)).thenReturn(true);
        when(portfolioHistoryService.getHistory(clientId, from, to)).thenReturn(history);

        var response = controller.getPortfolioHistory(clientId.toString(), from, to, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(history, response.getBody());
        verify(portfolioHistoryService).getHistory(clientId, from, to);
    }

    private MockHttpServletRequest requestWithClient(UUID clientId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("clientId", clientId);
        return request;
    }
}
