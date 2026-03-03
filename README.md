# 🌾 AgriDeco

> A Folia-compatible Paper plugin that adds **custom furniture** and **multi-stage crops** to your Minecraft server via
> virtual armor-stand entities — no resource-pack entity replacement required.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21.x-blue?logo=minecraft)](https://papermc.io/)
[![Folia](https://img.shields.io/badge/Folia-supported-green)](https://papermc.io/software/folia)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](LICENSE)

---

## ✨ Features

- **Custom Furniture** — place, interact with, sit on, store items in, or toggle decorative objects defined entirely
  in `config.yml`.
- **Multi-Stage Crops** — plant custom crops with configurable growth timers, growth chance, and stage visuals sourced
  from MMOItems.
- **Player Removal** — sneak + left-click any furniture to remove it, fully configurable (owner-only, sneak gate, item
  drop).
- **Ownership System** — restrict crop harvesting and furniture removal to the original placer, with admin bypass.
- **Placement Cooldown** — optional per-player cooldown shared between furniture and crop placements.
- **Configurable Sounds** — four sound events (place furniture, remove furniture, plant crop, harvest crop), each with
  independent enable/disable, Bukkit Sound name, volume, and pitch.
- **Per-Item Permissions** — optional `place_permission` and `remove_permission` nodes on individual furniture/crop
  definitions.
- **Packet-Based Rendering** — furniture and crops are rendered as virtual armor stands via PacketEvents; no real
  entities are spawned permanently.
- **Folia Support** — all scheduling uses Folia's region scheduler, making the plugin safe for multi-threaded server
  environments.
- **Soft Integrations** — optional support for AuraSkills (XP on harvest), JobsReborn (money + XP on harvest), and
  GriefPrevention (claim protection on placement).
- **Dual Storage** — SQLite (zero-config, default) or MySQL (for networks/BungeeCord).
- **Admin GUI** — browse and distribute all furniture and crop items in-game via `/agrideco gui`.
- **Per-Chunk Limits** — configurable caps on placed furniture and crops per chunk to prevent lag.

---

## 📦 Requirements

| Dependency                                                                                | Type     | Version |
|-------------------------------------------------------------------------------------------|----------|---------|
| [Paper](https://papermc.io/downloads/paper) / [Folia](https://papermc.io/downloads/folia) | Server   | 1.21.x  |
| [PacketEvents](https://github.com/retrooper/packetevents)                                 | Required | 2.11.2+ |
| [MMOItems](https://www.spigotmc.org/resources/mmoitems-premium.39267/)                    | Required | 6.9.5+  |
| [AuraSkills](https://aurelium.dev/auraskills)                                             | Optional | 2.3.9+  |
| [JobsReborn](https://www.spigotmc.org/resources/jobs-reborn.4216/)                        | Optional | 5.2.x+  |
| [GriefPrevention](https://github.com/TechFortress/GriefPrevention)                        | Optional | 16.18+  |

**Java 21** is required.

---

## 🚀 Installation

1. Download the latest `AgriDeco.jar` from [Releases](../../releases).
2. Drop it into your server's `plugins/` folder alongside **PacketEvents** and **MMOItems**.
3. Start the server once to generate `plugins/AgriDeco/config.yml` and `messages.yml`.
4. Define your furniture and crops in `config.yml` (see [Configuration](#configuration)).
5. Run `/agrideco reload` or restart the server to apply changes.

> ⚠️ **Note:** Structural changes to definitions (new furniture/crop entries, growth timings) require a **full restart**
> to apply to already-placed objects.

---

## ⚙️ Configuration

All configuration lives in `plugins/AgriDeco/config.yml`. Below is a full reference of every available option.

### General Settings

```yaml
storage-type: sqlite   # sqlite | mysql

settings:
  armorstands_max_distance: 32    # render distance for virtual entities (blocks)
  reload_permission: "agrideco.admin"

  chunk_limit:
    furnitures: 32
    crops: 64

  # Player-initiated furniture removal (sneak + left-click the armor stand)
  removal:
    enabled: true           # allow players to remove furniture at all
    sneak_required: true    # must be sneaking (prevents accidental removal)
    owner_only: true        # only the placer (or agrideco.admin) can remove
    drop_item: true         # drop the furniture item back on removal

  # Crop harvesting
  crops:
    owner_only_harvest: false  # restrict harvest to the planting player globally
    # can also be overridden per-crop

  # Placement cooldown (shared between furniture and crops)
  placement:
    cooldown_seconds: 0     # 0 = disabled

  # Sound effects — Bukkit Sound enum names:
  # https://jd.papermc.io/paper/1.21.11/org/bukkit/Sound.html
  sounds:
    place_furniture:
      enabled: true
      sound: "minecraft:block.wood.place"
      volume: 1.0
      pitch: 1.0

    remove_furniture:
      enabled: true
      sound: "minecraft:entity.item.break"
      volume: 1.0
      pitch: 1.2

    plant_crop:
      enabled: true
      sound: "minecraft:block.crop.break"
      volume: 0.8
      pitch: 1.5

    harvest_crop:
      enabled: true
      sound: "minecraft:entity.experience_orb.pickup"
      volume: 1.0
      pitch: 1.0
```

### Defining Furniture

```yaml
furniture:
  my_chair:
    id: "mmoitems:FURNITURE:CHAIR"         # MMOItem players hold to place this
    placement_type: "floor"                # floor | wall | ceiling
    furniture_type: "seat"                 # decorative | interactable | container | seat | light
    interaction_mmoitem_id: ""             # only for furniture_type: interactable (toggle-on visual)

    container:
      title: "<dark_gray>Storage"          # MiniMessage format
      size: 27                             # multiple of 9

    armorstand:
      offset: "0, 0, 0"                    # x, y, z decimal offset from placement location
      is_baby: false

    seat_offset: "0, 0.3, 0"              # only for furniture_type: seat

    barrier_offsets:
      - "0, 0, 0"                          # one BARRIER block per entry

    # Optional per-item permission nodes (leave "" for none)
    place_permission: ""                   # e.g. "agrideco.furniture.chair.place"
    remove_permission: ""                  # e.g. "agrideco.furniture.chair.remove"

    # Override global settings.removal.drop_item for this item
    remove_drop_item: true
```

**Furniture Types**

| Type           | Behaviour                                                  |
|----------------|------------------------------------------------------------|
| `decorative`   | No interaction.                                            |
| `interactable` | Toggles between two MMOItem visuals on right-click.        |
| `container`    | Opens a chest-like inventory on right-click.               |
| `seat`         | Player sits on right-click; standing up ejects the player. |
| `light`        | Reserved for future light-toggle support.                  |

### Defining Crops

```yaml
crops:
  my_carrot:
    crop_stage_ids: # ordered MMOItem IDs per growth stage
      - "mmoitems:INGREDIENTS:CARROT_STAGE_I"           # stage 0 = just planted
      - "mmoitems:INGREDIENTS:CARROT_STAGE_II"          # last stage = fully grown; item is dropped on harvest
    seed_id: "mmoitems:INGREDIENTS:CARROT_SEED"
    placement_block_type: "farmland"                     # Bukkit Material (case-insensitive)
    growth_time: 120                                     # ticks between growth checks (20 = 1 s)
    growth_chance: 40                                    # % chance to advance one stage per check

    aura_skills:
      experience: 1.4

    jobs:
      job_id: "farming"
      experience: 5.4
      money: 100.4

    armorstand:
      offset: "0, 0.5, 0"
      is_baby: true

    # Per-crop overrides
    place_permission: ""          # e.g. "agrideco.crop.carrot.plant"
    owner_only_harvest: false     # overrides global crops.owner_only_harvest
```

### MySQL (optional)

```yaml
storage-type: mysql
database:
  host: "localhost"
  port: 3306
  database: "agrideco"
  username: "root"
  password: "password"
  pool:
    maximum_pool_size: 10
    minimum_idle: 2
    connection_timeout: 30000
    idle_timeout: 600000
    max_lifetime: 1800000
```

---

## 💬 Messages

All player-facing messages live in `plugins/AgriDeco/messages.yml` and use
the [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format. Reload with `/agrideco reload`.

**Available placeholders**

| Placeholder | Used in                           | Description                   |
|-------------|-----------------------------------|-------------------------------|
| `{player}`  | `give_success`                    | Target player's name          |
| `{type}`    | `give_success`, `give_invalid_id` | `furniture` or `crop seed`    |
| `{id}`      | `give_success`, `give_invalid_id` | The config ID                 |
| `{seconds}` | `placement_cooldown`              | Remaining cooldown in seconds |

**All message keys**

| Key                     | When shown                                       |
|-------------------------|--------------------------------------------------|
| `plugin_reload`         | After `/agrideco reload`                         |
| `no_permission`         | Missing permission                               |
| `invalid_command`       | Unknown sub-command                              |
| `invalid_configuration` | Config parse error on reload                     |
| `invalid_placement`     | Wrong surface / block type                       |
| `chunk_limit`           | Chunk cap reached                                |
| `placed_furniture`      | Furniture placed                                 |
| `placed_crop`           | Crop planted                                     |
| `placement_cooldown`    | Cooldown active `{seconds}`                      |
| `removed_furniture`     | Furniture removed                                |
| `sneak_to_remove`       | Clicked without sneaking                         |
| `not_owner`             | Tried to remove/harvest something they don't own |
| `harvested_crop`        | Crop harvested                                   |
| `crop_not_ready`        | Crop not fully grown                             |
| `give_success`          | `/agrideco give` succeeded                       |
| `give_invalid_id`       | Unknown furniture/crop ID                        |
| `give_item_error`       | MMOItem could not be retrieved                   |
| `player_not_found`      | Target player not online                         |

---

## 🎮 Commands & Permissions

| Command                                  | Description                                    | Permission       |
|------------------------------------------|------------------------------------------------|------------------|
| `/agrideco reload`                       | Reload `config.yml` and `messages.yml`.        | `agrideco.admin` |
| `/agrideco give furniture <id> [player]` | Give a furniture item.                         | `agrideco.admin` |
| `/agrideco give crop <id> [player]`      | Give a crop seed item.                         | `agrideco.admin` |
| `/agrideco gui`                          | Open the in-game browser GUI.                  | `agrideco.admin` |
| `/agrideco remove furniture <uuid>`      | Force-remove a placed furniture piece by UUID. | `agrideco.admin` |
| `/agrideco remove crop <uuid>`           | Force-remove a planted crop by UUID.           | `agrideco.admin` |

**Aliases:** `/deco`, `/agri`

| Permission       | Default | Description                                               |
|------------------|---------|-----------------------------------------------------------|
| `agrideco.use`   | `true`  | Allows placing furniture and planting crops.              |
| `agrideco.admin` | `op`    | All admin commands + bypass for ownership/removal checks. |

Per-item permission nodes can be added directly in `config.yml` under each furniture or crop definition
via `place_permission` and `remove_permission`.

---

## 🔌 Soft Integrations

### AuraSkills

Grants XP to the harvesting player. Configure the target skill globally:

```yaml
aura_skills:
  skill: "farming"   # built-in skill name or namespaced ID e.g. my_pack:herbalism
```

Per-crop XP amount is set under each crop's `aura_skills.experience`.

### JobsReborn

Grants job EXP and money on harvest. The player must already be enrolled in the job for rewards to apply. Configure per
crop under `jobs.job_id`, `jobs.experience`, and `jobs.money`.

### GriefPrevention

When enabled, placement of furniture and crops is blocked in claims the player does not have build access to. No extra
configuration needed.

---

## 🏗️ Building from Source

```bash
git clone https://github.com/sun-mc-dev/AgriDeco.git
cd AgriDeco
mvn clean package
```

The shaded jar will be at `target/AgriDeco-1.0.jar`.

Dependencies fetched automatically via Maven from PaperMC, CodeMC, AuraSkills, JitPack, and PhoenixDevt repositories.

---

## 📁 Plugin File Structure

```
plugins/AgriDeco/
├── config.yml       — furniture & crop definitions, settings, database config
├── messages.yml     — all player-facing messages (MiniMessage format)
└── data.db          — SQLite database (only when storage-type: sqlite)
```

---

## ⚠️ Known Limitations

- **Container contents are not persisted** — items placed in furniture containers are lost on server restart. Full
  persistence is planned for a future release.
- **Structural config changes** (renaming or removing furniture/crop IDs) require a restart; reload only re-applies
  settings and messages to live objects.
- **No walk-into-chunk streaming** — virtual entities are sent on join and chunk load events. Players who walk into
  range of an entity after loading may not see it until they relog or a chunk reload triggers. A `PlayerMoveEvent`-based
  solution is planned.
- Virtual entity IDs occupy the range `[1 073 741 824, 2 147 483 646]` to avoid collisions with server-assigned entity
  IDs.

---

## 🛠️ Planned / Roadmap

- [ ] Container inventory persistence across restarts
- [ ] Walk-into-chunk entity visibility streaming (`PlayerMoveEvent`)
- [ ] Data migration utility for renamed config IDs
- [ ] `LIGHT` furniture type implementation

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).

---

*Built with ❤️ by [SunMC](https://github.com/sun-mc-dev).*