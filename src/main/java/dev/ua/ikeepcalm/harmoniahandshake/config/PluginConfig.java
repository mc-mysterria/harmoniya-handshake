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
        
        public String launcherReminder = "<green>Нагадуємо, що ви можете грати через лаунчер наших партнерів з модами:</green> <click:open_url:'https://harmoniya.net/'><underlined><aqua>Harmoniya (тицни на мене)!</aqua></underlined></click>";
        
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
        public String debugAccountNotFound = "<red>Акаунт не знайдено в nLogin</red>";

        public String accountCreated = "<gradient:green:lime>🎉 Вітаємо на сервері! Для вас було створено новий акаунт!</gradient>";
        public String accountCreatedTitle = "<gradient:gold:green>🎉 Акаунт створено!</gradient>";
        public String accountCreatedSubtitle = "<gray>Збережіть ваш пароль в безпечному місці</gray>";
        public String accountPassword = "<gradient:yellow:orange>📋 Ваш згенерований пароль: </gradient><click:copy_to_clipboard:'%1$s'><white><bold><underlined>%1$s</underlined></bold></white></click> <gray>(тицни щоб скопіювати)</gray>";
        public String accountPasswordSave = "<gradient:red:orange>⚠️ ВАЖЛИВО: Збережіть цей пароль! Він не буде показаний знову!</gradient>";
        public String accountBossbarMessage = "<gradient:gold:yellow>🔐 ЗБЕРЕЖІТЬ ПАРОЛЬ: %s</gradient>";
        public String accountPasswordBookTitle = "Пароль від акаунту";
        public String accountPasswordBookAuthor = "Harmoniya";
        public String accountPasswordBookPage = "<gold><bold>Вітаємо на Harmoniya!</bold></gold>\n\n<gray>Для вас було автоматично створено акаунт.</gray>\n\n<yellow>Ваш пароль:</yellow>\n<click:copy_to_clipboard:'%1$s'><white><bold>%1$s</bold></white></click>\n\n<red>Збережіть цей пароль! Він НЕ буде показаний знову.</red>";

        public String commandHelpHeader = "<gradient:gold:yellow>Harmoniya Handshake Commands:</gradient>";
        public String commandHelpReload = "<gray>• </gray><white>/harmoniya reload</white> <gray>- Перезавантажити конфігурацію</gray>";
        public String commandHelpStatus = "<gray>• </gray><white>/harmoniya status</white> <gray>- Показати статус плагіна</gray>";
        public String commandHelpDebug = "<gray>• </gray><white>/harmoniya debug <гравець></white> <gray>- Відлагодження для гравця</gray>";
        public String commandDebugUsage = "<red>Використання: /harmoniya debug <гравець></red>";
        public String commandReloadError = "<red>Помилка при перезавантаженні конфігурації: %s</red>";
        public String statusPlayersOnline = "<gray>Гравців онлайн: </gray><white>%d</white>";
    }
}