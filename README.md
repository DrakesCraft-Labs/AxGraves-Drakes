# AxGraves Drake

> Estado: preparado para Paper/Purpur 1.21.11 y Java 21. El artefacto se valida
> localmente antes de cualquier ventana de despliegue.

A Minecraft plugin that creates a death chest (grave) at the location where a player dies, so they can recover their items.

## Features

- Drops a grave chest containing all of the player's items at the death location.
- Stores the player's XP inside the grave.
- Configurable despawn timer, per-player grave limits, world filters, and death-cause blacklists.
- Optional rotation and holograms for the grave entity.
- Folia-compatible (regionised multithreading).

## Configuration

All options live in `plugins/AxGraves/config.yml` (auto-generated on first run). Notable keys:

| Key | Default | Description |
|---|---|---|
| `despawn-time-seconds` | `468000` (130 hours) | Seconds before an empty / expired grave is removed. Set to `-1` to disable. |
| `drop-items` | `true` | Whether remaining items fall to the ground when the grave expires. |
| `store-items` | `true` | Whether to move items into the grave. |
| `store-xp` | `true` | Whether to store XP. |
| `xp-keep-percentage` | `1.0` | Fraction of stored XP returned to the player. |
| `override-keep-inventory` | `false` | Keep protected inventories authoritative; a grave is created only when the final death event does not retain items. |
| `interact-only-own` | `false` | Restrict grave opening to the owner only. |
| `disabled-worlds` | `[]` | Worlds where graves are not created. |
| `blacklisted-death-causes` | `[]` | Death causes that skip grave creation (e.g. `VOID`). |
| `death-listener-priority` | `MONITOR` | Bukkit event priority for the death listener. |
| `save-graves.enabled` | `true` | Persist graves across server restarts. |

## Commands

| Command | Description | Permission |
|---|---|---|
| `/graves help` | Show the in-game help. | `axgraves.help` |
| `/graves reload` | Reload `config.yml` and `messages.yml`. | `axgraves.reload` |
| `/graves list [player]` | List your (or another player's) active graves. | `axgraves.list` / `axgraves.list.other` |
| `/graves tp <id>` | Teleport to a grave by ID. | `axgraves.tp` |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `axgraves.allowgraves` | `true` | Allow grave creation on death. |
| `axgraves.list` | `true` | List your own graves. |
| `axgraves.list.other` | `op` | List any player's graves. |
| `axgraves.tp` | `op` | Teleport to a grave. |
| `axgraves.tp.bypass` | `op` | Bypass ownership checks when teleporting. |
| `axgraves.help` | `true` | Access the `/graves help` command. |
| `axgraves.reload` | `op` | Reload configuration. |

## Placeholders

The plugin registers an identifier under `axgraves`. Available placeholders:

- `%axgraves_despawn-time_<graveId>%` — time remaining until a specific grave despawns.
- `%axgraves_despawn-time%` — despawn-time fallback for messages.

## PlaceholderAPI placeholders

When PlaceholderAPI is installed, the following player-scoped tokens are exposed:

- `%axgraves_grave_count%` — number of active graves owned by the player.
- `%axgraves_has_grave%` — `true` / `false`.

## Compatibility

| Server | Supported |
|---|---|
| Spigot 1.20.x | Yes |
| Paper 1.20.x – 1.21.x | Yes |
| Purpur 1.21.x | Yes |
| Folia | Yes (`folia-supported: true`) |

Requires **Java 21** on the server JVM.

## Soft Dependencies

- **Slimefun** — When present, items marked as `Soulbound` (either through the `Soulbound` interface
  or via the `Soulbound Rune` lore flag) **are excluded** from the grave to prevent duplication.
  AxGraves works normally when Slimefun is absent.

## Operaci\u00f3n Drake

- La decisi\u00f3n de conservar un objeto Soulbound pertenece a Slimefun; AxGraves
  excluye esos objetos de la tumba y restaura los casos pendientes al reconectar.
  Esto evita que ambos sistemas entreguen el mismo objeto.
- MoreGraves debe mantenerse deshabilitado antes de instalar este fork. No se
  deben cargar dos plugins de tumbas a la vez.
- El fork no ejecuta comprobaciones de actualizaciones, descargas autom\u00e1ticas ni
  telemetr\u00eda externa. Los JAR se revisan y despliegan de forma controlada.

## Building

Requirements: JDK 21 and Maven 3.8+.

```bash
mvn clean test
mvn package
```

The shaded jar is produced at `target/AxGraves-<version>-Drake-all.jar`.

## License

This project is licensed under the **GNU General Public License v3.0**. See [`LICENSE`](LICENSE)
for the full text.

Based on the original `AxGraves` plugin by Artillex-Studios; modified under the terms of GPLv3.
