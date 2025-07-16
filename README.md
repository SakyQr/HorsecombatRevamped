# HorseCombat Revamped Again

**⚠️ BETA VERSION - Use at your own risk! ⚠️**

A comprehensive Minecraft plugin that enhances horse combat mechanics with momentum-based damage, particle effects, and integration with popular protection plugins.

> **Note**: This plugin is currently in beta testing. Features may change, and bugs are expected. Please report any issues you encounter.

## Features

### Core Combat System
- **Momentum-Based Combat**: Damage calculations based on horse movement speed and momentum
- **Enhanced Horse Combat**: Improved mechanics for mounted combat scenarios
- **Particle Effects**: Visual feedback for combat actions and momentum changes
- **Custom Recipes**: Specialized items and equipment for horse combat

### Plugin Integrations
- **Towny Support**: Respects town boundaries and PvP settings
- **WorldGuard Integration**: Compatible with WorldGuard protection regions
- **Location-Based Combat Control**: Configurable combat restrictions by area

### Management Features
- **Debug Mode**: Toggle detailed logging for troubleshooting
- **Hot Reload**: Reload configuration without server restart
- **Player Cleanup**: Automatic cleanup when players disconnect
- **Command System**: Comprehensive command interface

## Installation

1. Download the latest **BETA** release from the releases page
2. Place the `.jar` file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin using the generated `config.yml`

**⚠️ Beta Warning**: 
- Always backup your server before installing beta software
- Test thoroughly in a development environment first
- Report bugs and issues on the project repository

## Dependencies

### Required
- **Spigot/Paper**: 1.16+ (recommended: latest Paper build)
- **Java**: 8 or higher

### Optional (for enhanced features)
- **Towny**: For town-based combat restrictions
- **WorldGuard**: For region-based protection integration

## Configuration

The plugin generates a `config.yml` file with the following key options:

```yaml
# Enable debug logging
debug: false

# Momentum system settings
momentum:
  enabled: true
  decay_rate: 0.1
  max_momentum: 100.0

# Integration settings
integrations:
  towny:
    enabled: true
    respect_pvp_settings: true
  worldguard:
    enabled: true
    respect_regions: true

# Particle effects
particles:
  enabled: true
  combat_effects: true
  momentum_trails: true
```

## Commands

### Main Command
```
/horsecombat [subcommand]
```

### Subcommands
- `/horsecombat reload` - Reload the plugin configuration
- `/horsecombat debug` - Toggle debug mode
- `/horsecombat status` - View plugin status and loaded integrations
- `/horsecombat help` - Display command help

### Permissions
- `horsecombat.admin` - Access to admin commands
- `horsecombat.use` - Basic plugin usage
- `horsecombat.debug` - Toggle debug mode
- `horsecombat.reload` - Reload configuration

## How It Works

### Momentum System
The plugin tracks horse movement and builds momentum that affects combat damage:
- **Speed Building**: Momentum increases with sustained movement
- **Damage Scaling**: Higher momentum = increased damage output
- **Decay**: Momentum naturally decreases when stationary
- **Visual Feedback**: Particle effects indicate current momentum level

### Combat Mechanics
- Damage calculations factor in horse speed and momentum
- Special effects trigger based on momentum thresholds
- Combat restrictions respect protection plugin settings
- Automatic cleanup prevents memory leaks

### Integration Features
- **Towny**: Automatically respects town PvP settings and boundaries
- **WorldGuard**: Honors region protection flags and combat restrictions
- **Fallback**: Graceful degradation when integration plugins are unavailable
