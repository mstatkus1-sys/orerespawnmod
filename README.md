# OreRespawn

A Minecraft Forge mod for **1.19.4** that makes ores respawn after being mined — just like resource nodes in Rust. When a player breaks an ore, a timer starts and the ore block comes back automatically at the same location. Server owners can place custom respawn nodes and configure how long each ore takes to come back.

---

## Features

- All vanilla ores respawn automatically after being mined
- Respawn timers are configurable per ore type in-game
- Player-placed ore blocks are **never** treated as respawn nodes — only natural or op-placed ores respawn
- Server operators can manually place respawn nodes anywhere using a command
- All settings and registered positions persist across server restarts
- Nearby players receive a chat notification when an ore respawns close to them

---

## Installation

1. Make sure you have **Forge 1.19.4** installed on your server
2. Download the latest `OreRespawn-1.19.4.jar` from the [Actions tab](../../actions) (click the latest successful build, then download the artifact)
3. Drop the `.jar` into your server's `mods/` folder
4. Restart the server

> If you are using Aternos: go to **Files → mods/** and upload the `.jar` there, then restart.

---

## Default Respawn Times

| Ore | Respawn Time |
|-----|-------------|
| Coal ore / Deepslate coal ore | 2 minutes |
| Copper ore / Deepslate copper ore | 4 minutes |
| Iron ore / Deepslate iron ore | 4 minutes |
| Redstone ore / Deepslate redstone ore | 5 minutes |
| Gold ore / Deepslate gold ore | 6 minutes |
| Lapis ore / Deepslate lapis ore | 6 minutes |
| Diamond ore / Deepslate diamond ore | 10 minutes |
| Emerald ore / Deepslate emerald ore | 10 minutes |

All timers can be changed in-game using `/orerespawn settime` — see commands below.

---

## Commands

All commands require **operator level 2** or higher.

---

### `/orehelp`
Shows all available commands and a list of ore names directly in chat.

```
/orehelp
```

---

### `/orerespawn place <ore>`

Places a respawning ore node at the block you are looking at. The block you are looking at will be replaced with the ore you specify. When a player mines it, it will respawn after the configured delay.

**Usage:** Look directly at any block (stone, dirt, air, anything), then run the command.

```
/orerespawn place minecraft:diamond_ore
/orerespawn place minecraft:deepslate_diamond_ore
/orerespawn place minecraft:coal_ore
/orerespawn place minecraft:ancient_debris
```

**Full list of valid ore names:**

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

Changes how long a specific ore takes to respawn. The value is in **seconds**. The change saves immediately and persists across restarts.

```
/orerespawn settime diamond_ore 300
/orerespawn settime coal_ore 60
/orerespawn settime ancient_debris 1800
/orerespawn settime deepslate_iron_ore 120
```

> Note: use the ore name **without** `minecraft:` for this command.

---

## How Player Protection Works

If a player picks up an ore block and places it somewhere (for example in their base), that block is flagged as player-placed. If they mine it, it will **not** respawn — it behaves like a normal block.

Only the following ore sources will respawn:
- Naturally generated ores in the world
- Blocks placed by an operator using `/orerespawn place`

This prevents players from farming infinite ores by placing and re-mining them.

---

## Config Files

The mod automatically creates two files in your server's `config/` folder on first use:

| File | Contents |
|------|----------|
| `config/orerespawn_registered.txt` | Coordinates of all op-placed respawn nodes |
| `config/orerespawn_times.txt` | Custom respawn times set via `/orerespawn settime` |

You can edit these files manually if needed. Each line in `orerespawn_registered.txt` is `x,y,z` and each line in `orerespawn_times.txt` is `ore_name=seconds`.

---

## Compatibility

| | |
|---|---|
| Minecraft version | 1.19.4 |
| Mod loader | Forge 45.2.0+ |
| Server side only | Yes — clients do not need the mod installed |
| Singleplayer | Works, but commands require cheats enabled |

---

## Building from Source

This repo uses GitHub Actions to build automatically. Every push to `main` compiles the mod and uploads the `.jar` as a build artifact.

To download the latest build:
1. Go to the **Actions** tab in this repo
2. Click the most recent successful workflow run (green checkmark)
3. Scroll down to **Artifacts** and download `OreRespawn-1.19.4`

To build locally you will need Java 17 and run:

```bash
./gradlew build
```

The output `.jar` will be at `build/libs/`.

---

## License

MIT — do whatever you want with it.
