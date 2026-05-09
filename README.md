# Allegro MCP Server

A Model Context Protocol (MCP) server for integrating with your personal [Allegro](https://allegro.pl) account. Access your orders, purchases, watched items, and more through AI assistants like Claude.

## Features

- **OAuth Device Flow Authentication** - Secure authentication with your Allegro account
- **Token Persistence & Auto-Refresh** - Keep sessions across restarts and refresh tokens automatically
- **View Orders** - See your purchase history and order details
- **Watched Offers** - Check items you're watching
- **Product Search** - Search Allegro offers by phrase
- **User Info** - Get your account information

## Available MCP Tools

| Tool | Description |
|------|-------------|
| `allegro_authenticate` | Start OAuth device authorization flow |
| `allegro_complete_authentication` | Complete authentication with device code |
| `allegro_check_auth_status` | Check if authenticated |
| `allegro_get_my_info` | Get your account information |
| `allegro_get_my_orders` | List your orders/purchases |
| `allegro_get_order_details` | Get details of a specific order |
| `allegro_get_watched_offers` | List offers you're watching |
| `allegro_get_bought_items` | List items you've bought |
| `allegro_search_products` | Search products by phrase |

## Prerequisites

- Java 21+
- Allegro Developer Account with registered application

## Setup

### 1. Register an Allegro Application

1. Go to [Allegro Developer Portal](https://developer.allegro.pl/)
2. Create a new application
3. Note your `Client ID` and `Client Secret`
4. Set redirect URI to `https://allegro.pl` (for device flow)

### 2. Configure Environment Variables

```bash
export ALLEGRO_CLIENT_ID=your_client_id
export ALLEGRO_CLIENT_SECRET=your_client_secret
# or use file-based secrets:
# export ALLEGRO_CLIENT_ID_FILE=/run/secrets/allegro_client_id
# export ALLEGRO_CLIENT_SECRET_FILE=/run/secrets/allegro_client_secret
```

### 3. Build and Run

```bash
./gradlew bootRun
```

The MCP server will be available at `http://localhost:8080/mcp`

## Usage with Claude Code

Add to your Claude Code MCP configuration:

```json
{
  "mcpServers": {
    "allegro": {
      "command": "curl",
      "args": ["-N", "http://localhost:8080/mcp/sse"]
    }
  }
}
```

Or run in stdio mode (coming soon).

## Authentication Flow

1. Call `allegro_authenticate` tool
2. Visit the provided URL in your browser
3. Authorize the application
4. Call `allegro_complete_authentication` with the device code
5. You're now authenticated and can use other tools

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `allegro.client-id` | - | Your Allegro Client ID |
| `allegro.client-secret` | - | Your Allegro Client Secret |
| `allegro.client-id-file` | - | Path to file containing Client ID |
| `allegro.client-secret-file` | - | Path to file containing Client Secret |
| `allegro.auth.token-file` | `~/.allegro-mcp/token.json` | Persisted OAuth token location |
| `allegro.auth.refresh-before-expiry-seconds` | `60` | Refresh token this many seconds before expiry |
| `allegro.sandbox` | `false` | Use Allegro sandbox environment |
| `server.port` | `8080` | Server port |

## Development

### Build

```bash
./gradlew build
```

### Test

```bash
./gradlew test
```

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Disclaimer

This is an unofficial project and is not affiliated with Allegro. Use at your own risk.
