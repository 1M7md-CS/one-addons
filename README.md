# OneAddons (26.1.2)

Client-side Fabric mod for Minecraft 26.1.2 (Mojang mappings) with automation utilities for Hypixel SkyBlock.

> **Branch:** `port/mc-26.1.2` — Ported from the original 1.21.11 Yarn version on `main`.

## Features

- **Flower** — Auto-right-clicks tall flowers you're looking at
- **Mushroom** — Auto-attacks red mushrooms you're looking at; tracks brown mushrooms as you look at them
- **Enchanting** — Auto-solves Chronomatron and Ultrasequencer in the Experimental Table
- **ChestAssist** — Auto-right-clicks chests you look at with a random delay
- **SwapAssist** — Auto-swaps to a target hotbar slot when you select a trigger slot
- **PlaceOnPosition** — Runs a configurable sequence of slot-swaps and interactions when you stand near a saved waypoint
- **Waypoint** — Saves your current position to a JSON file on key press
- **CooldownFix** — Removes the 5-tick mining cooldown (toggleable)

## Requirements

- Minecraft 26.1.2
- Fabric Loader >= 0.19.3
- Fabric API >= 0.152.1+26.1.2
- Java >= 25

## Installation

1. Install Fabric Loader for Minecraft 26.1.2
2. Put OneAddons.jar in your `mods/` folder
3. Put Fabric API in `mods/` too
4. Launch the game

## Usage

Open the UI with `/oneaddons` in-game. Toggle features on/off and configure keybinds.

Off-days are saved to `config/oneaddons/positions.json` and placement sequences to `config/oneaddons/placeonposition.json`.

## Building

```bash
./gradlew build
```

Output is in `build/libs/`.

## License

MIT