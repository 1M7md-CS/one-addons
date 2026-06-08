# OneAddons

Client-side Fabric mod for Minecraft 1.21.11 with automation utilities for Hypixel SkyBlock.

## Features

- **Flower** — Auto-right-clicks tall flowers you're looking at with configurable CPS
- **Mushroom** — Auto-attacks red mushrooms you're looking at; tracks brown mushrooms as you look at them
- **Enchanting** — Auto-solves Chronomatron and Ultrasequencer in the Experimental Table
- **ChestAssist** — Auto-right-clicks chests you look at with a random delay
- **SwapAssist** — Auto-swaps to a target hotbar slot when you select a trigger slot
- **Waypoint** — Saves your current position to a JSON file on key press
- **CooldownFix** — Removes the 5-tick mining cooldown (toggleable)

## Requirements

- Minecraft 1.21.11
- Fabric Loader >= 0.19.2
- Fabric API >= 0.141.3
- Java >= 21

## Installation

1. Install Fabric Loader for Minecraft 1.21.11
2. Put OneAddons.jar in your `mods/` folder
3. Put Fabric API in `mods/` too
4. Launch the game

## Usage

Open the UI with `/oneaddons` in-game. Toggle features on/off and configure keybinds.

## Building

```bash
./gradlew build
```

Output is in `build/libs/`.

## License

MIT
