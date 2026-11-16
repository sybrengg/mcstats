# MC Stats Mod

A server-side [Fabric](https://fabricmc.net/) mod for Minecraft that tracks comprehensive player statistics and pushes them to an external database (MySQL, SQLite, or any HTTP webhook).

Built for server operators who want dashboards, quest systems, leaderboards, or analytics — without writing a custom mod for it.

---

## Downloads

| Mod version | Minecraft | Fabric Loader | Fabric API     | Download |
|-------------|-----------|---------------|----------------|----------|
| 2.0.0       | 26.3      | ≥ 0.20.1      | 0.166.2+26.3   | [Release](../../releases/tag/v2.0.0) |
| 1.0.0       | 26.1.2    | ≥ 0.19.2      | 0.148.0+26.1.2 | [Releases](../../releases) |

All releases: [GitHub Releases](../../releases). Each release has the built `.jar` attached and includes a changelog.

> The mod is **server-side only**. Clients do not need to install it.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/server/) for your server.
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) into `mods/`.
3. Drop `mcstats-<version>.jar` (from the release page) into `mods/`.
4. Start the server once — it will generate `config/mcstats.json`.
5. Edit the config (see below), then restart.

---

## Configuration

First start creates `config/mcstats.json` with sane defaults. Restart after editing.

### Backends

| `database_type` | Description |
|-----------------|-------------|
| `logging`       | Prints stats to the server console. Default — useful for testing without setup. |
| `sqlite`        | Local file database. Zero setup. Good for single-server use. |
| `mysql`         | MySQL or MariaDB. Good when multiple services need to read the data. |
| `webhook`       | POSTs JSON to a URL on every flush. Good for external dashboards, Discord bots, custom backends. |

### Flush interval

Stats are buffered in memory and flushed in batches:

```json
"flush_interval_seconds": 30
```

Lower = more real-time, higher = less DB load. 30s is a reasonable default for most servers.

### Baseline import

If you already have a populated `world/stats/` directory, set `import_baseline: true` to retroactively push existing vanilla stats on next startup. The mod marks itself done so it won't run twice.

You can also trigger this manually:

```
/mcstats import           # run once if not yet imported
/mcstats import force     # re-run regardless
/mcstats status           # show tracker state + active backend
```

Both commands require OP level 4 by default.

> ⚠️ Only import against a clean/empty database, or you will end up with duplicate records.

### MySQL

```json
"mysql": {
  "host": "localhost",
  "port": 3306,
  "database": "mcstats",
  "username": "mcstats_writer",
  "password": "..."
}
```

The mod auto-creates the `stat_events` table on first connect. **Use a dedicated DB user with `INSERT` (and `SELECT` if you want the import to verify state) privileges only** — the mod never needs `DROP` or `ALTER`.

### SQLite

```json
"sqlite": {
  "file": "mcstats.db"
}
```

File is created in the server root. Back it up with the rest of your world data.

### Webhook

```json
"webhook": {
  "url": "https://your-server.example/api/stats",
  "api_key": "..."
}
```

The webhook is a **one-way push** — the mod POSTs to your endpoint, it never reads. If `api_key` is set, it's sent as `Authorization: Bearer <key>`.

Payload (one POST per player per flush cycle):

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

### Security notes

- The config file stores **credentials in plaintext**. Lock down `config/mcstats.json` (`chmod 600` on Linux) and never commit it.
- For webhooks, use `https://` for anything that crosses a network. Plain `http://` is fine for `localhost` only.
- The MySQL connector is bundled directly into the jar — no separate driver install required.

---

## Tracked stats

### Vanilla (via `StatHandler` mixin)

- **Blocks**: broken / placed, by type
- **Items**: picked up / crafted / used, by type
- **Combat**: kills (by entity type), deaths, damage dealt / taken
- **Movement**: walk, sprint, crouch, swim, fly, elytra, boat, horse, …
- **Actions**: jumps, animals bred, fish caught, villager trades
- **Time**: play time, world time, time since death / rest

These are pushed using the vanilla stat key format, e.g. `minecraft.mined:minecraft.stone`.

### Natural Mining Tracker

Tracks blocks mined that were **not placed by a player** — so quests like "mine 50 diamond ore" can't be cheesed by placing and re-mining cobblestone.

**How it works:**
1. When a player places a block, the position is recorded.
2. When a block is broken, the mod checks if that position was player-placed.
3. Only non-placed breaks emit a `mcstats.natural_mined:` stat.

**Example:**

```json
{
  "minecraft.mined:minecraft.diamond_ore": 10,
  "mcstats.natural_mined:minecraft.diamond_ore": 7
}
```

↑ The player broke 10 diamond ore total; 7 were natural, 3 were re-placed.

> The placed-block tracker resets on server restart. Fine for naturally-generated resources (they don't get replanted), but means cobblestone placed before a restart and broken after will count as "natural" once.

### Custom trackers

| Stat                 | Key                                                | Aggregation |
|----------------------|----------------------------------------------------|-------------|
| Natural blocks mined | `mcstats.natural_mined:minecraft.<block>`          | sum |
| Death with cause     | `mcstats.death:<damage_type>:<killer_entity>`      | sum |
| Enchantments done    | `mcstats.custom:enchanted`                         | sum |
| Unique biomes        | `mcstats.custom:unique_biomes_visited`             | count |
| Totem uses           | `mcstats.custom:totems_used`                       | sum |
| Times slept          | `mcstats.custom:times_slept`                       | sum |
| Highest single hit   | `mcstats.custom:highest_single_hit`                | max (×10 — stored as deci-damage) |

---

## Building from source

### Requirements

- JDK 25 (Temurin recommended)
- The repo's bundled Gradle wrapper (Gradle 9.8.0)

### Build

```bash
./gradlew build
```

The shadowed jar lands in `build/libs/mcstats-<version>.jar`. SQLite and MySQL drivers are bundled in.

### Run a dev server

```bash
./gradlew runServer
```

This spins up a Fabric dev environment with the mod loaded.

---

## Releases & versioning

Releases are built by GitHub Actions on every `v*` tag push. To cut a release:

1. Bump `mod_version` in `gradle.properties`.
2. Commit and tag: `git tag v1.2.3 && git push origin v1.2.3`.
3. The `release` workflow builds the jar and attaches it to a GitHub Release automatically.

Version scheme: `MAJOR.MINOR.PATCH` against a given Minecraft version. Minecraft version bumps usually mean a `MAJOR` bump because mappings change.

---

## Troubleshooting

| Symptom | Likely cause |
|---------|--------------|
| Mod loads but no stats appear | `database_type` is still `logging` — check console for "[MC Stats] event:" lines. |
| `Failed to connect to MySQL` | Bad credentials, firewall, or DB user missing `INSERT` on the database. |
| Baseline import is skipped | Already ran once; use `/mcstats import force` to re-run. |
| `natural_mined` counts cobblestone after restart | Expected — placed-block tracker is in-memory and resets on restart. |

---

## License

[CC0 1.0 Universal](LICENSE) — public domain, no attribution required (but appreciated).
