# OneAddons

Client-side Fabric mod for Minecraft 26.1.2 with automation utilities for Hypixel SkyBlock.

Powered by **Odin API** — all configuration is done through Odin's GUI (no custom UI).

## Modules

| Module | Description |
|--------|-------------|
| **Auto Experiment** | Automatically solves Chronomatron and Ultrasequencer experiments |
| **Flower** | Auto-breaks tall flowers with configurable CPS |
| **Mushroom** | Auto-breaks red and brown mushrooms |
| **Chest Assist** | Auto-interacts with chests with configurable delay |
| **Swap Assist** | Auto-swaps to a target hotbar slot when a trigger slot is selected |
| **Place On Position** | Executes slot-swap & interaction sequences when near saved waypoints |
| **Waypoint** | Saves current position to JSON on key press |
| **Key Maker** | Auto-crafts Tungsten or Umber keys in the Forge |

## Requirements

- Minecraft 26.1.2
- Fabric Loader >= 0.19.3
- Fabric API >= 0.152.1+26.1.2
- Fabric Language Kotlin >= 1.13.12+kotlin.2.4.0
- Odin >= 0.2.2
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
