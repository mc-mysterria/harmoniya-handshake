package dev.ua.ikeepcalm.harmoniahandshake.service;

import dev.ua.ikeepcalm.harmoniahandshake.HarmoniyaHandshake;
import dev.ua.ikeepcalm.harmoniahandshake.config.ConfigManager;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class HttpService {

    private final Logger logger;
    private final ConfigManager configManager;
    private final HarmoniyaHandshake plugin;
    private final ExecutorService executor;
    private final HttpClient httpClient;

    private static final Pattern CLEAN_TOKEN_PATTERN = Pattern.compile("[^\\p{Print}]|\\s");
    private static final Pattern IP_PATTERN = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");

    public HttpService(Logger logger, ConfigManager configManager, HarmoniyaHandshake plugin) {
        this.logger = logger;
        this.configManager = configManager;
        this.plugin = plugin;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.httpClient = HttpClient.newBuilder()
                .executor(executor)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public CompletableFuture<Integer> validateTokenAsync(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            logger.warn("Received empty or null access token");
            return CompletableFuture.completedFuture(400);
        }

        String cleanedToken = sanitizeToken(accessToken);
        if (cleanedToken.isEmpty()) {
            logger.warn("Access token became empty after sanitization");
            return CompletableFuture.completedFuture(400);
        }

        plugin.debugLog("Original token length: {}, cleaned token length: {}", accessToken.length(), cleanedToken.length());
        plugin.debugLog("Token starts with: {}", preview(cleanedToken));

        String endpoint = configManager.getConfig().apiEndpoint;
        int timeout = configManager.getConfig().httpTimeout;

        if (!endpoint.startsWith("https://")) {
            logger.warn("API endpoint {} does not use HTTPS - access tokens would be sent in plaintext", endpoint);
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(endpoint))
                    .GET()
                    .header("Authorization", "Bearer " + cleanedToken)
                    .header("User-Agent", "HarmoniyaHandshake/1.0")
                    .timeout(Duration.ofMillis(timeout))
                    .build();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid API endpoint configured: {}", endpoint, e);
            return CompletableFuture.completedFuture(-1);
        }

        plugin.debugLog("Sending request to endpoint: {}", endpoint);
        plugin.debugLog("Request headers: Authorization=Bearer {}, User-Agent=HarmoniyaHandshake/1.0", preview(cleanedToken));
        plugin.debugLog("Timeout: {}", timeout);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int responseCode = response.statusCode();
                    logger.info("Token validation response: {} for endpoint: {}", responseCode, endpoint);
                    plugin.debugLog("Response headers: {}", response.headers().map());

                    if (responseCode >= 400 && !response.body().isEmpty()) {
                        plugin.debugLog("Error response body: {}", response.body());
                    }

                    return responseCode;
                })
                .exceptionally(throwable -> {
                    logger.error("Failed to validate token at endpoint: {}", endpoint, throwable);
                    return -1;
                });
    }

    public boolean checkIPMatch(String currentIP, String lastIP) {
        if (currentIP == null || lastIP == null) {
            logger.warn("IP comparison failed: currentIP={}, lastIP={}", currentIP, lastIP);
            return false;
        }

        if (!isValidIP(currentIP) || !isValidIP(lastIP)) {
            logger.warn("Invalid IP format: currentIP={}, lastIP={}", currentIP, lastIP);
            return false;
        }

        if (configManager.getConfig().strictIpValidation) {
            return currentIP.equals(lastIP);
        } else {
            return extractIPPrefix(currentIP).equals(extractIPPrefix(lastIP));
        }
    }

    public void shutdown() {
        executor.shutdown();
    }

    private String preview(String token) {
        return token.length() > 10 ? token.substring(0, 10) + "..." : token;
    }

    private String sanitizeToken(String token) {
        if (token == null) {
            return "";
        }
        return CLEAN_TOKEN_PATTERN.matcher(token.trim()).replaceAll("");
    }

    private boolean isValidIP(String ip) {
        return ip != null && IP_PATTERN.matcher(ip).matches();
    }

    private String extractIPPrefix(String ip) {
        int lastDotIndex = ip.lastIndexOf('.');
        if (lastDotIndex == -1) {
            logger.warn("IP does not contain dots: {}", ip);
            return ip;
        }
        return ip.substring(0, lastDotIndex);
    }
}
