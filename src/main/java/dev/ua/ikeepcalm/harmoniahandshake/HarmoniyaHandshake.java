package dev.ua.ikeepcalm.harmoniahandshake;

import com.google.inject.Inject;
import com.nickuc.login.api.nLoginAPI;
import com.nickuc.login.api.types.AccountData;
import com.nickuc.login.api.types.Identity;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import dev.ua.ikeepcalm.harmoniahandshake.commands.HarmoniyaCommand;
import dev.ua.ikeepcalm.harmoniahandshake.config.ConfigManager;
import dev.ua.ikeepcalm.harmoniahandshake.service.HttpService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.bossbar.BossBar;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Plugin(id = "harmoniyahandshake", name = "HarmoniyaHandshake", version = "1.0.0")
public class HarmoniyaHandshake {

    private final Logger logger;
    private final ProxyServer server;
    private final ConfigManager configManager;
    private final HttpService httpService;
    private final MiniMessage miniMessage;
    private final ConsoleCommandSource console;
    private final SecureRandom random;
    
    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("harmoniyabridge:handshake");

    @Inject
    public HarmoniyaHandshake(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.server = server;
        this.console = server.getConsoleCommandSource();
        this.configManager = new ConfigManager(logger, dataDirectory);
        this.httpService = new HttpService(logger, configManager, this);
        this.miniMessage = MiniMessage.miniMessage();
        this.random = new SecureRandom();
        
        try {
            configManager.loadConfig();
            logger.info("HarmoniyaHandshake has been initialized!");
            logger.info("Starting listening for Harmoniya Handshakes...");
        } catch (Exception e) {
            logger.error("Failed to initialize HarmoniyaHandshake", e);
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(IDENTIFIER);
        server.getCommandManager().register("harmoniya", new HarmoniyaCommand(this, server));
        logger.info("Harmoniya commands registered successfully");
    }

    @Subscribe
    public void onPluginMessageFromPlayer(PluginMessageEvent event) {
        if (!(event.getSource() instanceof Player player)) {
            return;
        }
        if (!event.getIdentifier().equals(IDENTIFIER)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                debugLog("Processing plugin message from player {} with data length: {}", 
                    player.getUsername(), event.getData().length);
                
                String accessToken = extractAccessToken(event.getData());
                if (accessToken == null) {
                    debugLog("Failed to extract access token from plugin message for player {}", player.getUsername());
                    sendLauncherReminder(player);
                    return;
                }
                
                debugLog("Successfully extracted access token for player {} (length: {})", 
                    player.getUsername(), accessToken.length());
                
                httpService.validateTokenAsync(accessToken)
                    .thenAccept(responseCode -> {
                        if (responseCode == 200) {
                            debugLog("Token validation successful for player {}, proceeding with auto-login", player.getUsername());
                            forceLoginUser(player);
                        } else {
                            logger.info("Token validation failed for player {} with response code: {}", player.getUsername(), responseCode);
                            if (responseCode == 401) {
                                debugLog("401 Unauthorized - Token may be invalid, expired, or server endpoint may have authentication issues");
                            } else if (responseCode == 403) {
                                debugLog("403 Forbidden - Token is valid but lacks required permissions");
                            } else if (responseCode == 404) {
                                debugLog("404 Not Found - API endpoint may not exist or be incorrectly configured");
                            }
                            sendLauncherReminder(player);
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.error("Error during token validation for player {}", player.getUsername(), throwable);
                        sendLauncherReminder(player);
                        return null;
                    });
                    
            } catch (Exception e) {
                logger.error("Error processing plugin message from player {}", player.getUsername(), e);
                sendLauncherReminder(player);
            }
        });
    }

    private void forceLoginUser(Player player) {
        try {
            Identity identity = Identity.ofKnownName(player.getUsername());
            AccountData account = nLoginAPI.getApi().getAccount(identity).orElse(null);
            
            if (account == null) {
                debugLog("No nLogin account found for player {}, creating new account", player.getUsername());
                createAccountForPlayer(player, identity);
                return;
            }

            String lastIP = account.getLastIP();
            String currentIP = player.getRemoteAddress().getAddress().getHostAddress();
            
            if (httpService.checkIPMatch(currentIP, lastIP)) {
                nLoginAPI.getApi().forceLogin(identity);
                
                var messages = configManager.getConfig().messages;
                player.sendMessage(miniMessage.deserialize(messages.successLogin));
                
                Title title = Title.title(
                    miniMessage.deserialize(messages.successTitle),
                    miniMessage.deserialize(messages.successSubtitle),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                );
                player.showTitle(title);
                
                logger.info("Automatic login successful for player {} {} from IP {}", 
                    player.getUsername(), player.getUniqueId(), currentIP);
                
                debugLog("Player IP: {}, Last IP: {}", currentIP, lastIP);
                
            } else {
                var messages = configManager.getConfig().messages;
                player.sendMessage(miniMessage.deserialize(messages.ipMismatch));
                player.sendMessage(miniMessage.deserialize(messages.manualLoginRequired));
                
                logger.info("IP mismatch for player {} {}: current={}, last={}", 
                    player.getUsername(), player.getUniqueId(), currentIP, lastIP);
            }
            
        } catch (Exception e) {
            logger.error("Error during force login for player {}", player.getUsername(), e);
        }
    }

    private String extractAccessToken(byte[] data) {
        debugLog("Raw data length: {} bytes", data.length);
        
        try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(data))) {
            String handshakeToken = dataInputStream.readUTF();
            debugLog("Successfully extracted handshake token from structured payload (length: {})", handshakeToken.length());
            return handshakeToken;
            
        } catch (Exception structuredException) {
            debugLog("Failed to read as structured payload: {}", structuredException.getMessage());
            
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)))) {
                String token = bufferedReader.readLine();
                if (token != null) {
                    debugLog("Successfully extracted handshake token from simple string format (length: {})", token.length());
                    return token;
                } else {
                    debugLog("Read null token from simple string format");
                    return null;
                }
            } catch (Exception stringException) {
                debugLog("Failed to read as simple string: {}", stringException.getMessage());
                logger.error("Failed to extract access token from plugin message data using both methods");
                return null;
            }
        }
    }
    
    private void sendLauncherReminder(Player player) {
        var message = configManager.getConfig().messages.launcherReminder;
        player.sendMessage(miniMessage.deserialize(message));
    }
    
    public void debugLog(String message, Object... args) {
        if (configManager.getConfig().debugMode) {
            String formattedMessage = String.format(message.replace("{}", "%s"), args);
            console.sendMessage(Component.text("[HarmoniyaHandshake DEBUG] " + formattedMessage));
        }
    }
    
    private void createAccountForPlayer(Player player, Identity identity) {
        try {
            String generatedPassword = generateRandomPassword();
            String currentIP = player.getRemoteAddress().getAddress().getHostAddress();
            
            boolean accountCreated = nLoginAPI.getApi().performRegister(identity, generatedPassword, currentIP);
            
            if (accountCreated) {
                debugLog("Successfully created account for player {}", player.getUsername());
                nLoginAPI.getApi().forceLogin(identity);
                notifyAccountCreation(player, generatedPassword);
                
                logger.info("New account created and auto-logged for player {} {} from IP {}", 
                    player.getUsername(), player.getUniqueId(), currentIP);
                    
            } else {
                debugLog("Failed to create account for player {}", player.getUsername());
                logger.error("Failed to create nLogin account for player {}", player.getUsername());
                sendLauncherReminder(player);
            }
            
        } catch (Exception e) {
            logger.error("Error creating account for player {}", player.getUsername(), e);
            sendLauncherReminder(player);
        }
    }
    
    private void notifyAccountCreation(Player player, String password) {
        var messages = configManager.getConfig().messages;
        
        player.sendMessage(miniMessage.deserialize(messages.accountCreated));
        player.sendMessage(miniMessage.deserialize(String.format(messages.accountPassword, password)));
        player.sendMessage(miniMessage.deserialize(messages.accountPasswordSave));
        
        Title title = Title.title(
            miniMessage.deserialize(messages.accountCreatedTitle),
            miniMessage.deserialize(messages.accountCreatedSubtitle),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
        );
        player.showTitle(title);
        
        BossBar bossBar = BossBar.bossBar(
            Component.text(String.format(messages.accountBossbarMessage, password)),
            1.0f,
            BossBar.Color.YELLOW,
            BossBar.Overlay.PROGRESS
        );
        
        player.showBossBar(bossBar);
        
        server.getScheduler().buildTask(this, () -> {
            player.hideBossBar(bossBar);
        }).delay(30, TimeUnit.SECONDS).schedule();
    }
    
    private String generateRandomPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < 12; i++) {
            int index = random.nextInt(characters.length());
            password.append(characters.charAt(index));
        }
        
        return password.toString();
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public HttpService getHttpService() {
        return httpService;
    }
    
    public Logger getLogger() {
        return logger;
    }

}
