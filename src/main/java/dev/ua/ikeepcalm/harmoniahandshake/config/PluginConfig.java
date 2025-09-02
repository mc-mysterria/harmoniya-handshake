package dev.ua.ikeepcalm.harmoniahandshake.config;

public class PluginConfig {
    
    public String apiEndpoint = "https://yggdrasil.harmoniya.net/privileges";
    public int httpTimeout = 5000;
    public boolean debugMode = false;
    public boolean strictIpValidation = true;
    
    public Messages messages = new Messages();
    
    public static class Messages {
        public String successLogin = "<gradient:green:aqua>Ви автоматично увійшли в гру за допомогою лаунчеру!</gradient>";
        public String successTitle = "<gradient:gold:yellow>Успішно увійшли!</gradient>";
        public String successSubtitle = "<gray>Через Harmoniya Launcher</gray>";
        
        public String ipMismatch = "<gradient:red:dark_red>Ваша IP-адреса не співпадає з IP-адресою, з якої ви востаннє увійшли в гру!</gradient>";
        public String manualLoginRequired = "<gradient:orange:red>Щоб продовжити, ви маєте увійти власноруч, для підтвердження особистості гравця!</gradient>";
        
        public String launcherReminder = "<green>Нагадуємо, що у нашого серверу є власний зручний лаунчер</green> <click:open_url:'https://harmoniya.net/'><underlined><aqua>Harmoniya (тицни на мене)!</aqua></underlined></click>";
        
        public String configReloaded = "<gradient:green:lime>Конфігурацію перезавантажено!</gradient>";
        public String noPermission = "<red>У вас немає дозволу на використання цієї команди!</red>";
        
        public String statusOnline = "<gradient:green:lime>✓ Harmoniya Handshake працює нормально</gradient>";
        public String statusApiEndpoint = "<gray>API Endpoint: </gray><white>%s</white>";
        public String statusTimeout = "<gray>HTTP Timeout: </gray><white>%d мс</white>";
        public String statusDebugMode = "<gray>Debug Mode: </gray><white>%s</white>";
        
        public String debugPlayerNotFound = "<red>Гравця не знайдено!</red>";
        public String debugPlayerInfo = "<gradient:blue:cyan>Інформація про гравця %s:</gradient>";
        public String debugCurrentIp = "<gray>Поточна IP: </gray><white>%s</white>";
        public String debugLastIp = "<gray>Остання IP: </gray><white>%s</white>";
        public String debugIpMatch = "<gray>IP співпадає: </gray><white>%s</white>";
        
        public String accountCreated = "<gradient:green:lime>🎉 Вітаємо на сервері! Для вас було створено новий акаунт!</gradient>";
        public String accountCreatedTitle = "<gradient:gold:green>🎉 Акаунт створено!</gradient>";
        public String accountCreatedSubtitle = "<gray>Збережіть ваш пароль в безпечному місці</gray>";
        public String accountPassword = "<gradient:yellow:orange>📋 Ваш згенерований пароль: </gradient><white><bold>%s</bold></white>";
        public String accountPasswordSave = "<gradient:red:orange>⚠️ ВАЖЛИВО: Збережіть цей пароль! Він не буде показаний знову!</gradient>";
        public String accountBossbarMessage = "🔐 ЗБЕРЕЖІТЬ ПАРОЛЬ: %s";
    }
}