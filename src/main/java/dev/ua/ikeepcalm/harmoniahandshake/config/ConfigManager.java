package dev.ua.ikeepcalm.harmoniahandshake.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ConfigManager {
    
    private final Logger logger;
    private final Path dataDirectory;
    private final Path configFile;
    private final Gson gson;
    private PluginConfig config;
    
    public ConfigManager(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.configFile = dataDirectory.resolve("config.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    public void loadConfig() throws IOException {
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
        }
        
        if (!Files.exists(configFile)) {
            saveDefaultConfig();
        }
        
        try (Reader reader = Files.newBufferedReader(configFile)) {
            config = gson.fromJson(reader, PluginConfig.class);
            if (config == null) {
                config = new PluginConfig();
                saveConfig();
            }
        }
        
        logger.info("Configuration loaded successfully");
    }
    
    public void saveConfig() throws IOException {
        if (config == null) {
            config = new PluginConfig();
        }
        
        String json = gson.toJson(config);
        Files.write(configFile, json.getBytes());
        logger.info("Configuration saved successfully");
    }
    
    private void saveDefaultConfig() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.json")) {
            if (inputStream != null) {
                Files.copy(inputStream, configFile, StandardCopyOption.REPLACE_EXISTING);
            } else {
                config = new PluginConfig();
                saveConfig();
            }
        }
    }
    
    public void reloadConfig() throws IOException {
        loadConfig();
        logger.info("Configuration reloaded successfully");
    }
    
    public PluginConfig getConfig() {
        return config;
    }
}