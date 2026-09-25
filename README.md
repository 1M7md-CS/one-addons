# OneAddons

Client-side Fabric mod for Minecraft 26.1.2 with automation utilities for Hypixel SkyBlock.

Powered by **Odin API** — all configuration is done through Odin's GUI

## Modules

| Module | Description |
|--------|-------------|
| **Auto Experiment** | Automatically solves Chronomatron and Ultrasequencer experiments |
| **Mushroom** | Auto-breaks red and brown mushrooms |
| **NoBlind** | Removes blindness effects while enabled |
| **Chest Assist** | Auto-interacts with chests with configurable delay |
| **Auto Slot Swap** | Switches from a trigger slot to a target slot and optionally uses items |
| **Waypoint Actions** | Runs optional item-use and return-slot actions near saved waypoints |
| **Waypoint** | Saves current position to JSON on key press |
| **Key Maker** | Auto-crafts Tungsten or Umber keys in the Forge |
| **Toggle Key** | Toggle any keybind on/off with a single press |

## Requirements

- Minecraft 26.1.2
- Fabric Loader >= 0.19.3
- Fabric API >= 0.152.1+26.1.2
- Fabric Language Kotlin >= 1.13.12+kotlin.2.4.0
- Odin >= 0.3.4
- Java >= 25

## Building

```bash
./gradlew build
```

Output in `build/libs/`.

## Config

- Waypoints: `config/oneaddons/positions.json`
- Place sequences: `config/oneaddons/placeonposition.json`
- Module config: managed through Odin GUI

## License

MIT
