package dev.ua.ikeepcalm.harmoniahandshake.service;

import dev.ua.ikeepcalm.harmoniahandshake.config.ConfigManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class HttpService {
    
    private final Logger logger;
    private final ConfigManager configManager;
    
    private static final Pattern CLEAN_TOKEN_PATTERN = Pattern.compile("[^\\p{Print}]|\\s");
    private static final Pattern IP_PATTERN = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    
    public HttpService(Logger logger, ConfigManager configManager) {
        this.logger = logger;
        this.configManager = configManager;
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
            
            int responseCode = connection.getResponseCode();
            
            if (configManager.getConfig().debugMode) {
                logger.info("Token validation response: {} for endpoint: {}", responseCode, endpoint);
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