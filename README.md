# HarmoniyaHandshake

A Velocity plugin that provides seamless automatic login for players using the Harmoniya Launcher through secure token validation.

## Features

- 🔐 **Secure Token Validation** - Validates launcher tokens against Harmoniya API
- 🌈 **Beautiful Messages** - Gradient colored messages using MiniMessage
- ⚙️ **Configurable** - JSON-based configuration with hot reload
- 🛡️ **IP Validation** - Smart IP matching with configurable strictness
- 📋 **Admin Commands** - Comprehensive management and debugging tools
- ⚡ **Async Processing** - Non-blocking operations for better performance
- 🔍 **Debug Mode** - Detailed logging for troubleshooting

## Installation

1. Download the latest release
2. Place `HarmoniyaHandshake-1.0.0.jar` in your Velocity `plugins/` folder
3. Restart your Velocity proxy
4. Configure the plugin in `plugins/HarmoniyaHandshake/config.json`

## Configuration

The plugin creates a `config.json` file on first run:

```json
{
  "apiEndpoint": "https://yggdrasil.harmoniya.net/privileges",
  "httpTimeout": 5000,
  "debugMode": false,
  "strictIpValidation": true,
  "messages": {
    "successLogin": "<gradient:green:aqua>Ви автоматично увійшли в гру за допомогою лаунчеру!</gradient>",
    "successTitle": "<gradient:gold:yellow>Успішно увійшли!</gradient>",
    "successSubtitle": "<gray>Через Harmoniya Launcher</gray>",
    "ipMismatch": "<gradient:red:dark_red>Ваша IP-адреса не співпадає з IP-адресою, з якої ви востаннє увійшли в гру!</gradient>",
    "manualLoginRequired": "<gradient:orange:red>Щоб продовжити, ви маєте увійти власноруч, для підтвердження особистості гравця!</gradient>",
    "launcherReminder": "<green>Нагадуємо, що у нашого серверу є власний зручний лаунчер</green> <click:open_url:'https://harmoniya.net/'><underlined><aqua>Harmoniya (тицни на мене)!</aqua></underlined></click>"
  }
}
```

### Configuration Options

- `apiEndpoint` - Harmoniya API endpoint for token validation
- `httpTimeout` - HTTP request timeout in milliseconds
- `debugMode` - Enable detailed debug logging
- `strictIpValidation` - When `false`, matches IP subnets instead of exact IPs

## Commands

All commands require the `harmoniya.admin` permission.

### `/harmoniya reload`
- **Permission**: `harmoniya.admin.reload`
- **Description**: Reloads the plugin configuration
- **Usage**: `/harmoniya reload`

### `/harmoniya status`
- **Permission**: `harmoniya.admin.status`
- **Description**: Shows plugin status and configuration
- **Usage**: `/harmoniya status`

### `/harmoniya debug <player>`
- **Permission**: `harmoniya.admin.debug`
- **Description**: Debug authentication information for a specific player
- **Usage**: `/harmoniya debug PlayerName`

## Permissions

- `harmoniya.admin` - Base admin permission (required for all commands)
- `harmoniya.admin.reload` - Permission to reload configuration
- `harmoniya.admin.status` - Permission to view status
- `harmoniya.admin.debug` - Permission to debug players

## How It Works

1. **Plugin Message Reception**: Receives authentication data from Harmoniya Launcher via plugin channels
2. **Token Validation**: Validates the received access token against Harmoniya API
3. **IP Verification**: Compares current IP with last known IP from nLogin
4. **Automatic Login**: If validation passes, automatically logs in the player using nLogin API
5. **Feedback**: Provides beautiful gradient messages and titles to inform the player

## Dependencies

- **Velocity API** 3.4.0+
- **nLogin API** 10.4+
- **Adventure MiniMessage** 4.17.0+

## Development

### Building

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

## Troubleshooting

### Enable Debug Mode
Set `debugMode: true` in `config.json` and reload the plugin for detailed logging.

### Common Issues

1. **Token Validation Fails**
   - Check API endpoint configuration
   - Verify network connectivity
   - Check server logs for HTTP errors

2. **IP Mismatch Errors**
   - Set `strictIpValidation: false` for subnet matching
   - Check nLogin database for correct IP storage

3. **Plugin Messages Not Received**
   - Ensure Harmoniya Launcher is up to date
   - Verify plugin channel registration
   - Check for plugin conflicts

## License

This project is licensed under the MIT License.

## Support

For support and bug reports, please contact the server administration or check the plugin logs with debug mode enabled.