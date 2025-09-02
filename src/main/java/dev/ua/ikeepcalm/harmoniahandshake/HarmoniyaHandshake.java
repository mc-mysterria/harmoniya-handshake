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
import dev.ua.ikeepcalm.harmoniahandshake.commands.HarmoniyaCommand;
import dev.ua.ikeepcalm.harmoniahandshake.config.ConfigManager;
import dev.ua.ikeepcalm.harmoniahandshake.service.HttpService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Plugin(id = "harmoniyahandshake", name = "HarmoniyaHandshake", version = "1.0.0")
public class HarmoniyaHandshake {

    private final Logger logger;
    private final ProxyServer server;
    private final ConfigManager configManager;
    private final HttpService httpService;
    private final MiniMessage miniMessage;
    
    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("harmoniyabridge:handshake");

    @Inject
    public HarmoniyaHandshake(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.server = server;
        this.configManager = new ConfigManager(logger, dataDirectory);
        this.httpService = new HttpService(logger, configManager);
        this.miniMessage = MiniMessage.miniMessage();
        
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
                String accessToken = extractAccessToken(event.getData());
                if (accessToken == null) {
                    if (configManager.getConfig().debugMode) {
                        logger.warn("Failed to extract access token from plugin message for player {}", player.getUsername());
                    }
                    sendLauncherReminder(player);
                    return;
                }
                
                httpService.validateTokenAsync(accessToken)
                    .thenAccept(responseCode -> {
                        if (responseCode == 200) {
                            forceLoginUser(player);
                        } else {
                            if (configManager.getConfig().debugMode) {
                                logger.info("Token validation failed for player {} with response code: {}", player.getUsername(), responseCode);
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
                if (configManager.getConfig().debugMode) {
                    logger.warn("No nLogin account found for player {}", player.getUsername());
                }
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
                
                if (configManager.getConfig().debugMode) {
                    logger.debug("Player IP: {}, Last IP: {}", currentIP, lastIP);
                }
                
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
        try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(data));
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream))) {
            return bufferedReader.readLine();
        } catch (IOException e) {
            logger.error("Failed to extract access token from plugin message data", e);
            return null;
        }
    }
    
    private void sendLauncherReminder(Player player) {
        var message = configManager.getConfig().messages.launcherReminder;
        player.sendMessage(miniMessage.deserialize(message));
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
