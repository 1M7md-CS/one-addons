# OneAddons (26.1.2)

Client-side Fabric mod for Minecraft 26.1.2 (Mojang mappings) with automation utilities for Hypixel SkyBlock.

> **Branch:** `port/mc-26.1.2` — Ported from the original 1.21.11 Yarn version on `main`.

## Features

| Module | Description |
|--------|-------------|
| **Flower** | Auto-right-clicks tall flowers you're looking at |
| **Mushroom** | Auto-attacks red mushrooms; tracks brown mushrooms |
| **Enchanting** | Auto-solves Chronomatron & Ultrasequencer in the Experimental Table |
| **ChestAssist** | Auto-right-clicks chests with configurable random delay |
| **SwapAssist** | Auto-swaps to a target hotbar slot when you select a trigger slot |
| **PlaceOnPosition** | Configurable sequence of slot-swaps & interactions when near a saved waypoint |
| **Waypoint** | Saves current position to JSON on key press |
| **CooldownFix** | Removes the 5-tick mining cooldown (toggleable) |
| **KeyMaker** | Auto-crafts Tungsten or Umber keys in the Forge (scan, collect, craft, repeat) |

## Requirements

- Minecraft 26.1.2
- Fabric Loader >= 0.19.3
- Fabric API >= 0.152.1+26.1.2
- Java >= 25

## Installation

1. Install Fabric Loader for Minecraft 26.1.2
2. Place `OneAddons.jar` in your `mods/` folder
3. Place Fabric API in `mods/` too
4. Launch the game

## Usage

Open the UI with `/oneaddons` in-game (3 tabs: Utility, Enchant, Plants). Toggle features on/off and configure keybinds.

- Waypoints save to `config/oneaddons/positions.json`
- Placement sequences save to `config/oneaddons/placeonposition.json`
- KeyMaker uses the Rift Forge with configurable mode (Tungsten/Umber) and click delay

## Building

```bash
./gradlew build
```

Output is in `build/libs/`.

## License

MIT
