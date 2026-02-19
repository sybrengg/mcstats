# MC Stats Mod

A server-side Fabric mod for Minecraft 1.21.11 that tracks comprehensive player statistics and pushes them to an external database.

## ⚙️ Configuration

On first run, a config file is created at `config/mcstats.json`. Edit it and restart the server.

### Database Types

Change `"database_type"` to one of:

| Type | Description |
|------|-------------|
| `logging` | Logs stats to server console (default, for testing) |
| `mysql` | Connects to a MySQL/MariaDB server |
| `sqlite` | Stores in a local `mcstats.db` file (zero setup) |
| `webhook` | POSTs JSON to a URL every flush cycle |

### MySQL Setup
1. Create a database: `CREATE DATABASE mcstats;`
2. Set `database_type` to `"mysql"`
3. Fill in `host`, `port`, `database`, `username`, `password`
4. The mod auto-creates the `stat_events` table on startup

### SQLite Setup
1. Set `database_type` to `"sqlite"`
2. Optionally change the `file` name (default: `mcstats.db` in server root)
3. That's it — no server or credentials needed

### Webhook Setup
The webhook is a **one-way push** — the mod sends stat data TO your endpoint via HTTP POST. It does **not** query or return any data.

1. Set `database_type` to `"webhook"`
2. Set `url` to your endpoint (e.g. `http://your-server.com/api/stats`)
3. Optionally set `api_key` for Bearer token authentication

**Every flush cycle (default: 30 seconds), the mod POSTs:**
```json
{
  "player_uuid": "444be9c1-2ac6-4c4c-82b2-77859e333c45",
  "stats": {
    "minecraft.mined:minecraft.stone": 42,
    "mcstats.natural_mined:minecraft.stone": 38,
    "minecraft.custom:minecraft.jump": 15,
    "mcstats.custom:times_slept": 1
  },
  "timestamp": 1708204800000
}
```

Your endpoint can then store this in any database, forward to Discord, feed a dashboard, etc.

---

## 🎯 Tracked Stats

### Vanilla (via StatHandler Mixin)
- **Blocks**: broken, placed (by type)
- **Items**: picked up, crafted, used (by type)
- **Combat**: kills (by entity), deaths, damage dealt/taken
- **Movement**: walk, sprint, crouch, swim, fly, elytra, boat, horse, etc.
- **Actions**: jumps, animals bred, fish caught, villager trades
- **Time**: play time, world time, time since death/rest

### Natural Mining Tracker
Tracks blocks mined that were **not placed by a player** — perfect for quests like "mine 50 diamond ore" without letting players exploit by placing and re-mining.

**How it works:**
1. When a player places a block, the position is recorded
2. When a block is broken, the mod checks if that position was player-placed
3. Only non-placed blocks emit a `mcstats.natural_mined:` stat

**Example webhook data:**
```json
{
  "minecraft.mined:minecraft.diamond_ore": 10,
  "mcstats.natural_mined:minecraft.diamond_ore": 7
}
```
↑ Player broke 10 diamond ore total, but only 7 were from the world (3 were re-placed).

> **Note:** The placed-block tracker resets on server restart. This is fine for natural resources since they don't get replanted by players.

### Custom Trackers
| Stat | Key | Type |
|------|-----|------|
| Natural blocks mined | `mcstats.natural_mined:minecraft.diamond_ore` | sum |
| Death with cause | `mcstats.death:<type>:<killer>` | sum |
| Enchantments done | `mcstats.custom:enchanted` | sum |
| Unique biomes | `mcstats.custom:unique_biomes_visited` | count |
| Totem uses | `mcstats.custom:totems_used` | sum |
| Times slept | `mcstats.custom:times_slept` | sum |
| Highest single hit | `mcstats.custom:highest_single_hit` | max (×10) |

## 🛠️ Building

### Requirements
- Java 21 JDK

### Build
```bash
./gradlew build
```

### Install
Copy `build/libs/mcstats-1.0.0.jar` to your server's `mods/` folder alongside Fabric API.
