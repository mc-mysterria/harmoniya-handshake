package dev.ua.ikeepcalm.harmoniahandshake.service;

import dev.ua.ikeepcalm.harmoniahandshake.config.ConfigManager;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class HttpService {

    private final Logger logger;
    private final ConfigManager configManager;
    private final dev.ua.ikeepcalm.harmoniahandshake.HarmoniyaHandshake plugin;

    private static final Pattern CLEAN_TOKEN_PATTERN = Pattern.compile("[^\\p{Print}]|\\s");
    private static final Pattern IP_PATTERN = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");

    public HttpService(Logger logger, ConfigManager configManager, dev.ua.ikeepcalm.harmoniahandshake.HarmoniyaHandshake plugin) {
        this.logger = logger;
        this.configManager = configManager;
        this.plugin = plugin;
    }

    public CompletableFuture<Integer> validateTokenAsync(String accessToken) {
        return CompletableFuture.supplyAsync(() -> validateToken(accessToken));
    }

    public int validateToken(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            logger.warn("Received empty or null access token");
            return 400;
        }

        String cleanedToken = sanitizeToken(accessToken);
        if (cleanedToken.isEmpty()) {
            logger.warn("Access token became empty after sanitization");
            return 400;
        }

        plugin.debugLog("Original token length: {}, cleaned token length: {}", accessToken.length(), cleanedToken.length());
        plugin.debugLog("Token starts with: {}", cleanedToken.length() > 10 ? cleanedToken.substring(0, 10) + "..." : cleanedToken);

        String endpoint = configManager.getConfig().apiEndpoint;
        int timeout = configManager.getConfig().httpTimeout;

        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + cleanedToken);
            connection.setRequestProperty("User-Agent", "HarmoniyaHandshake/1.0");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            plugin.debugLog("Sending request to endpoint: {}", endpoint);
            plugin.debugLog("Request headers: Authorization=Bearer {}, User-Agent=HarmoniyaHandshake/1.0",
                    cleanedToken.length() > 10 ? cleanedToken.substring(0, 10) + "..." : cleanedToken);
            plugin.debugLog("Timeouts: connect={}, read={}", timeout, timeout);

            int responseCode = connection.getResponseCode();

            logger.info("Token validation response: {} for endpoint: {}", responseCode, endpoint);
            plugin.debugLog("Response message: {}", connection.getResponseMessage());
            plugin.debugLog("Response headers: {}", connection.getHeaderFields());

            if (responseCode >= 400) {
                try {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(
                            responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line).append("\n");
                    }
                    errorReader.close();

                    if (errorResponse.length() > 0) {
                        plugin.debugLog("Error response body: {}", errorResponse.toString().trim());
                    }
                } catch (Exception e) {
                    plugin.debugLog("Failed to read error response body: {}", e.getMessage());
                }
            }

            return responseCode;

        } catch (IOException e) {
            logger.error("Failed to validate token at endpoint: {}", endpoint, e);
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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