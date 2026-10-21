package com.eatrading.api.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.Quote;
import com.eatrading.api.dto.WatchlistItemResponse;
import com.eatrading.api.entities.Client;
import com.eatrading.api.exception.ConflictException;
import com.eatrading.api.exception.ResourceNotFoundException;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.repository.ClientRepository;

/**
 * Service for managing client watchlist operations
 * 
 * Provides business logic for:
 * - Retrieving watchlist items with current stock prices
 * - Adding stocks to watchlist
 * - Removing stocks from watchlist
 * - Checking if stock is in watchlist
 * 
 * Uses Client entity's @ElementCollection watchlist (client_watchlist table)
 */
@Service
public class WatchlistService {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistService.class);
    public static final String CLIENT_NOT_FOUND_MESSAGE = "Client not found: ";

    private final ClientRepository clientRepository;
    private final QuoteService quoteService;

    public WatchlistService(ClientRepository clientRepository, QuoteService quoteService) {
        this.clientRepository = clientRepository;
        this.quoteService = quoteService;
    }

    /**
     * Get client's watchlist with current market data for each stock
     * 
     * @param clientId UUID of the client
     * @return List of WatchlistItemResponse with current prices
     * @throws ResourceNotFoundException if client doesn't exist
     */
    @Transactional(readOnly = true)
    public List<WatchlistItemResponse> getWatchlist(UUID clientId) {
        logger.info("Retrieving watchlist for client: {}", clientId);

        Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new ResourceNotFoundException(CLIENT_NOT_FOUND_MESSAGE + clientId));

        return client.getWatchlist().stream()
                .map(asset -> {
                    try {
                        // Get current quote for the symbol
                        Quote quote = quoteService.getQuote(asset.getSymbol());
                        if (quote != null) {
                            return new WatchlistItemResponse(
                                    quote.getSymbol(),
                                    asset.getName() != null ? asset.getName() : asset.getSymbol(),
                                    quote.getPrice(),
                                    quote.getChange(),
                                    quote.getChangePercent(),
                                    Instant.parse(quote.getAsOf()));
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to get quote for symbol {} in watchlist: {}",
                                asset.getSymbol(), e.getMessage());
                    }
                    return null;
                })
                .filter(item -> item != null)
                .toList();
    }

    /**
     * Add a stock to client's watchlist
     * 
     * @param clientId UUID of the client
     * @param symbol   Stock symbol to add
     * @return WatchlistItemResponse with current price data
     * @throws ResourceNotFoundException if client doesn't exist or stock symbol not
     *                                   found
     * @throws ConflictException         if stock already in watchlist
     */
    @Transactional
    public WatchlistItemResponse addToWatchlist(UUID clientId, String symbol, Instrument instrument) {
        logger.info("Adding symbol {} to watchlist for client: {}", symbol, clientId);

        String upperSymbol = symbol.toUpperCase();

        // Get client
        Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new ResourceNotFoundException(CLIENT_NOT_FOUND_MESSAGE + clientId));

        // Check if already exists in watchlist
        boolean alreadyExists = client.getWatchlist().stream()
                .anyMatch(asset -> asset.getSymbol().equalsIgnoreCase(upperSymbol));

        if (alreadyExists) {
            logger.warn("Stock {} already in watchlist for client {}", upperSymbol, clientId);
            throw new ConflictException("Stock " + upperSymbol + " already in watchlist");
        }

        // Get current quote to verify stock exists
        Quote quote;
        try {
            quote = quoteService.getQuote(upperSymbol);
            if (quote == null) {
                logger.warn("Stock quote not found for symbol: {}", upperSymbol);
                throw new ResourceNotFoundException("Stock symbol not found: " + upperSymbol);
            }
        } catch (Exception e) {
            logger.error("Error fetching quote for symbol {}: {}", upperSymbol, e.getMessage());
            throw new ResourceNotFoundException("Stock symbol not found or API error: " + upperSymbol);
        }

        // Create new Asset and add to watchlist
        Asset asset = new Asset(upperSymbol, quote.getSymbol(), instrument);
        client.addToWatchlist(asset);
        clientRepository.save(client);

        logger.info("Successfully added {} to watchlist for client {}", upperSymbol, clientId);

        // Return with current market data
        return new WatchlistItemResponse(
                quote.getSymbol(),
                quote.getSymbol(),
                quote.getPrice(),
                quote.getChange(),
                quote.getChangePercent(),
                Instant.parse(quote.getAsOf()));
    }

    /**
     * Remove a stock from client's watchlist
     * 
     * @param clientId UUID of the client
     * @param symbol   Stock symbol to remove
     * @throws ResourceNotFoundException if client doesn't exist or stock not in
     *                                   watchlist
     */
    @Transactional
    public void removeFromWatchlist(UUID clientId, String symbol) {
        logger.info("Removing symbol {} from watchlist for client: {}", symbol, clientId);

        String upperSymbol = symbol.toUpperCase();

        // Get client
        Client client = clientRepository.findById(clientId)
            .orElseThrow(() -> new ResourceNotFoundException(CLIENT_NOT_FOUND_MESSAGE + clientId));

        // Check if exists in watchlist
        boolean found = client.getWatchlist().stream()
                .anyMatch(asset -> asset.getSymbol().equalsIgnoreCase(upperSymbol));

        if (!found) {
            logger.warn("Stock {} not found in watchlist for client {}", upperSymbol, clientId);
            throw new ResourceNotFoundException("Stock " + upperSymbol + " not in watchlist");
        }

        // Remove from watchlist
        client.getWatchlist().removeIf(asset -> asset.getSymbol().equalsIgnoreCase(upperSymbol));
        clientRepository.save(client);

        logger.info("Successfully removed {} from watchlist for client {}", upperSymbol, clientId);
    }

    /**
     * Check if a stock is in client's watchlist
     * 
     * @param clientId UUID of the client
     * @param symbol   Stock symbol to check
     * @return true if stock is in watchlist, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isInWatchlist(UUID clientId, String symbol) {
        return clientRepository.findById(clientId)
                .map(client -> client.getWatchlist().stream()
                        .anyMatch(asset -> asset.getSymbol().equalsIgnoreCase(symbol.toUpperCase())))
                .orElse(false);
    }

    /**
     * Clear entire watchlist for a client
     * 
     * @param clientId UUID of the client
     */
    @Transactional
    public void clearWatchlist(UUID clientId) {
        logger.info("Clearing watchlist for client: {}", clientId);
        clientRepository.findById(clientId).ifPresent(client -> {
            client.getWatchlist().clear();
            clientRepository.save(client);
        });
    }

    /**
     * Get watchlist size for a client
     * 
     * @param clientId UUID of the client
     * @return Number of items in watchlist
     */
    @Transactional(readOnly = true)
    public int getWatchlistSize(UUID clientId) {
        return clientRepository.findById(clientId)
                .map(client -> client.getWatchlist().size())
                .orElse(0);
    }
}
