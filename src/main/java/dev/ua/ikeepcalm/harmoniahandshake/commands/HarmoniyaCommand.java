package dev.ua.ikeepcalm.harmoniahandshake.commands;

import com.nickuc.login.api.nLoginAPI;
import com.nickuc.login.api.types.AccountData;
import com.nickuc.login.api.types.Identity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.ua.ikeepcalm.harmoniahandshake.HarmoniyaHandshake;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HarmoniyaCommand implements SimpleCommand {

    private final HarmoniyaHandshake plugin;
    private final ProxyServer server;
    private final MiniMessage miniMessage;

    public HarmoniyaCommand(HarmoniyaHandshake plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            showHelp(source);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!source.hasPermission("harmoniya.admin.reload")) {
                    source.sendMessage(miniMessage.deserialize(
                            plugin.getConfigManager().getConfig().messages.noPermission
                    ));
                    return;
                }
                reloadConfig(source);
            }
            case "status" -> {
                if (!source.hasPermission("harmoniya.admin.status")) {
                    source.sendMessage(miniMessage.deserialize(
                            plugin.getConfigManager().getConfig().messages.noPermission
                    ));
                    return;
                }
                showStatus(source);
            }
            case "debug" -> {
                if (!source.hasPermission("harmoniya.admin.debug")) {
                    source.sendMessage(miniMessage.deserialize(
                            plugin.getConfigManager().getConfig().messages.noPermission
                    ));
                    return;
                }
                if (args.length < 2) {
                    source.sendMessage(miniMessage.deserialize(
                            plugin.getConfigManager().getConfig().messages.commandDebugUsage
                    ));
                    return;
                }
                debugPlayer(source, args[1]);
            }
            default -> showHelp(source);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            return List.of("reload", "status", "debug");
        }
        if (args[0].equalsIgnoreCase("debug") && args.length == 2) {
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("harmoniya.admin");
    }

    private void showHelp(CommandSource source) {
        var messages = plugin.getConfigManager().getConfig().messages;
        source.sendMessage(miniMessage.deserialize(messages.commandHelpHeader));
        source.sendMessage(miniMessage.deserialize(messages.commandHelpReload));
        source.sendMessage(miniMessage.deserialize(messages.commandHelpStatus));
        source.sendMessage(miniMessage.deserialize(messages.commandHelpDebug));
    }

    private void reloadConfig(CommandSource source) {
        CompletableFuture.runAsync(() -> {
            try {
                plugin.getConfigManager().reloadConfig();
                source.sendMessage(miniMessage.deserialize(
                        plugin.getConfigManager().getConfig().messages.configReloaded
                ));
            } catch (IOException e) {
                source.sendMessage(miniMessage.deserialize(
                        String.format(plugin.getConfigManager().getConfig().messages.commandReloadError, e.getMessage())
                ));
                plugin.getLogger().error("Failed to reload configuration", e);
            }
        });
    }

    private void showStatus(CommandSource source) {
        var config = plugin.getConfigManager().getConfig();
        var messages = config.messages;

        source.sendMessage(miniMessage.deserialize(messages.statusOnline));
        source.sendMessage(miniMessage.deserialize(String.format(messages.statusApiEndpoint, config.apiEndpoint)));
        source.sendMessage(miniMessage.deserialize(String.format(messages.statusTimeout, config.httpTimeout)));
        source.sendMessage(miniMessage.deserialize(String.format(messages.statusDebugMode, config.debugMode ? "Увімкнено" : "Вимкнено")));

        long playersOnline = server.getAllPlayers().size();
        source.sendMessage(miniMessage.deserialize(String.format(messages.statusPlayersOnline, playersOnline)));
    }

    private void debugPlayer(CommandSource source, String playerName) {
        server.getPlayer(playerName).ifPresentOrElse(player -> {
            CompletableFuture.runAsync(() -> {
                var messages = plugin.getConfigManager().getConfig().messages;

                source.sendMessage(miniMessage.deserialize(String.format(messages.debugPlayerInfo, player.getUsername())));

                Identity identity = Identity.ofKnownName(player.getUsername());
                AccountData account = nLoginAPI.getApi().getAccount(identity).orElse(null);

                String currentIP = player.getRemoteAddress().getAddress().getHostAddress();
                source.sendMessage(miniMessage.deserialize(String.format(messages.debugCurrentIp, currentIP)));

                if (account != null) {
                    String lastIP = account.getLastIP();
                    source.sendMessage(miniMessage.deserialize(String.format(messages.debugLastIp, lastIP)));

                    boolean ipMatches = plugin.getHttpService().checkIPMatch(currentIP, lastIP);
                    source.sendMessage(miniMessage.deserialize(String.format(messages.debugIpMatch, ipMatches ? "Так" : "Ні")));
                } else {
                    source.sendMessage(miniMessage.deserialize(messages.debugAccountNotFound));
                }
            });
        }, () -> {
            source.sendMessage(miniMessage.deserialize(
                    plugin.getConfigManager().getConfig().messages.debugPlayerNotFound
            ));
        });
    }
}