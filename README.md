# HorseCombatRevampedAgain

A Minecraft plugin that enhances horse combat gameplay with customizable horse spawning, lance mechanics, and Towny integration.

## Features

- **Custom Horse Spawning**: Define regions where horses spawn with specific colors and styles
- **Lance Combat**: Special lance weapons for mounted combat
- **Towny Integration**: Respect town boundaries for horse spawning
- **Admin Commands**: Manage horse spawning and plugin configuration

## Installation

1. Download the plugin JAR file
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Edit the configuration file to customize settings

## Configuration

The plugin will generate a default configuration file on first startup. You can modify this file to customize various aspects of the plugin.

### Basic Configuration

```yaml
# Enable or disable debug logging
debug: false

# Horse spawning settings
horses:
  # Chance of a horse spawning (0.0 to 1.0)
  spawnChance: 0.05
  # Minimum and maximum health values for spawned horses
  minHealth: 15.0
  maxHealth: 30.0
  # Horse movement speed multiplier
  speedMultiplier: 1.0

# Lance configuration
lances:
  # Base damage for lance attacks
  baseDamage: 8.0
  # Damage multiplier based on speed
  speedMultiplier: 1.5
  # Cooldown between lance attacks (in seconds)
  cooldown: 2.0

# Towny integration
towny:
  # Enable or disable Towny integration
  enabled: true
  # Allow horses to spawn in towns
  allowTownHorseSpawns: false
  # Allow combat in towns
  allowTownCombat: false

# Custom horse spawn regions
regions:
  - name: "Castle"
    world: "world"
    x1: -100
    z1: -100
    x2: 100
    z2: 100
    color: "BLACK"
    style: "NONE"
    spawnChance: 0.1
  - name: "Plains"
    world: "world"
    x1: 200
    z1: 200
    x2: 400
    z2: 400
    color: "WHITE"
    style: "WHITE"
    spawnChance: 0.05
```

### Horse Colors and Styles

Valid horse colors:
- BLACK
- BROWN
- CHESTNUT
- CREAMY
- DARK_BROWN
- GRAY
- WHITE

Valid horse styles:
- NONE
- BLACK_DOTS
- WHITE
- WHITE_DOTS
- WHITEFIELD

## Commands

### Player Commands

- `/horsecombat checkregion` - Check if you're in a horse spawn region
- `/horsecombat listregs` - List all configured horse spawn regions

### Admin Commands

- `/horsecombat reload` - Reload the plugin configuration
- `/horsecombat debug` - Toggle debug mode
- `/horsecombat spawnhorse` - Force spawn a horse at your location
- `/givelance [player] [type]` - Give a lance to a player

## Permissions

- `horsecombat.reload` - Access to reload the plugin
- `horsecombat.debug` - Access to debug mode
- `horsecombat.admin` - Access to admin commands
- `horsecombat.use` - Ability to use lances and horse combat features
- `horsecombat.givelance` - Access to give lances to players

## Towny Integration

When Towny is installed and enabled in the config, the plugin will respect town boundaries:

- Horses won't spawn in town areas unless `allowTownHorseSpawns` is set to true
- Combat mechanics can be disabled in towns with the `allowTownCombat` setting

## Troubleshooting

### Common Issues

1. **Horses not spawning in defined regions**
   - Check if the regions are properly configured in the config.yml
   - Ensure the world names match exactly
   - Verify the coordinates are correct
   - Check if the spawn chance is set appropriately

2. **Towny integration not working**
   - Make sure Towny is installed and enabled
   - Verify the `towny.enabled` setting is set to true
   - Check server logs for any integration errors

3. **Lance commands not working**
   - Ensure the player has the correct permissions
   - Verify the lance types are correctly defined in the config

### Console Errors

If you see `Unresolved reference` errors in the console related to Towny, this is normal when Towny is not installed. The plugin handles this gracefully and will run without Towny integration.

## License

This plugin is released under the GNU Affero General Public License v3.0 (AGPL-3.0). This license ensures that:
- Users can use, modify, and distribute the plugin
- Any modified versions must also be released under the AGPL-3.0
- The source code must remain available to all users
- The plugin cannot be incorporated into proprietary software

## Credits

Developed by sakyQr

Discord is SakyQ
