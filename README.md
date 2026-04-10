# OreRespawn

A Minecraft Forge mod for **1.19.4** that makes ores respawn automatically after being mined. When an ore is broken, a configurable timer starts and the block restores itself at the exact same position. Server operators have full control over respawn times and can manually register custom respawn nodes anywhere in the world.

---

## Requirements

- Minecraft 1.19.4
- Forge 45.2.0 or higher
- Java 17
- Server-side only — players do not need the mod installed

---

## Installation

1. Download the latest `OreRespawn-1.19.4.jar` from the [Releases](../../releases) page
2. Place the `.jar` file into your server's `mods/` folder
3. Restart the server

The mod will generate its config files automatically on first run.

---

## Default Respawn Times

| Ore | Default Time |
|-----|-------------|
| Coal ore / Deepslate coal ore | 2 minutes |
| Copper ore / Deepslate copper ore | 4 minutes |
| Iron ore / Deepslate iron ore | 4 minutes |
| Redstone ore / Deepslate redstone ore | 5 minutes |
| Gold ore / Deepslate gold ore | 6 minutes |
| Lapis ore / Deepslate lapis ore | 6 minutes |
| Diamond ore / Deepslate diamond ore | 10 minutes |
| Emerald ore / Deepslate emerald ore | 10 minutes |

All timers are adjustable per ore type using `/orerespawn settime`.

---

## Commands

All commands require operator level 2 or higher.

---

### `/orehelp`
Displays all available commands and valid ore names in chat.

---

### `/orerespawn place <ore>`
Replaces the block you are looking at with the specified ore and registers it as a permanent respawn node. When a player mines it, it will respawn after the configured delay. The position is saved and survives server restarts.

Look directly at a block, then run the command:

```
/orerespawn place minecraft:diamond_ore
/orerespawn place minecraft:deepslate_iron_ore
/orerespawn place minecraft:ancient_debris
```

**Valid ore names:**

```
minecraft:coal_ore                  minecraft:deepslate_coal_ore
minecraft:iron_ore                  minecraft:deepslate_iron_ore
minecraft:copper_ore                minecraft:deepslate_copper_ore
minecraft:gold_ore                  minecraft:deepslate_gold_ore
minecraft:lapis_ore                 minecraft:deepslate_lapis_ore
minecraft:redstone_ore              minecraft:deepslate_redstone_ore
minecraft:diamond_ore               minecraft:deepslate_diamond_ore
minecraft:emerald_ore               minecraft:deepslate_emerald_ore
minecraft:ancient_debris
minecraft:nether_gold_ore
minecraft:nether_quartz_ore
```

---

### `/orerespawn settime <ore> <seconds>`
Sets the respawn delay for a specific ore type in seconds. The change applies immediately and persists across restarts.

```
/orerespawn settime diamond_ore 300
/orerespawn settime coal_ore 60
/orerespawn settime ancient_debris 1800
```

> Use the ore name without the `minecraft:` prefix for this command.

---

## Player Protection

Ore blocks placed by players are tracked and excluded from the respawn system. If a player places an ore block and later mines it, it will behave like any normal block and will not respawn. Only the following ore sources are eligible to respawn:

- Naturally generated ores in the world
- Blocks manually registered by an operator via `/orerespawn place`

This prevents players from exploiting the system by placing and re-mining ore blocks.

---

## Configuration Files

The mod stores its data in two plain text files inside the server's `config/` directory, created automatically on first use.

| File | Description |
|------|-------------|
| `config/orerespawn_registered.txt` | Coordinates of all operator-registered respawn nodes, one entry per line in `x,y,z` format |
| `config/orerespawn_times.txt` | Custom respawn delays set via the command, one entry per line in `ore_name=seconds` format |

Both files are human-readable and can be edited manually while the server is offline.

---

## Building from Source

Requires Java 17 and Gradle 7.6.

```bash
git clone https://github.com/your-username/orerespawnmod.git
cd orerespawnmod
./gradlew build
```

The compiled `.jar` will be output to `build/libs/`.

---

## License

MIT
