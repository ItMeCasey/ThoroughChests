---
title: Settings
summary: The config file.
---
`config/thoroughchests.properties` is written on the first start with every key and a comment. Delete a line to restore its default; restart the game or server after editing.

| Keys | Meaning |
|---|---|
| `level.<id>.slots` | Slots of a level (1 to 512) |
| `level.<id>.blast_resistance` | Blast resistance of a level; 3600000 or more is immune |
| `compact.interval_ticks`, `compact.max_crafts`, `compact.any_recipe` | Compact timing and recipe rule |
| `furnace.speed.N`, `furnace.fuel_use.N` | Furnace level N: speed multiplier and fuel factor |
| `blast_furnace.speed.N`, `blast_furnace.fuel_use.N` | The same for the Blast Furnace |
| `magnet.radius.N`, `magnet.interval.N` | Magnet radius in blocks and ticks between pulls |
| `feeder.range.N`, `feeder.interval.N`, `feeder.breed`, `feeder.effect_foods` | Feeder range, cadence and rules |
| `void.enabled_by_default` | Whether a new Void augment starts on |
| `lock.ops_bypass`, `lock.block_automation` | Lock rules |
| `gui.default_rows` | Rows of the chest window before the screen has fitted itself |

Times are in ticks (20 per second).
