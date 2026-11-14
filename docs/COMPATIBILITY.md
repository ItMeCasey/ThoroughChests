# Compatibility

Every integration is optional. The mod loads and works the same with none of them installed (the headless self test runs a build without any of them and prints `SELFTEST DONE failures=0`). Availability was checked on 2026-11-14 with the CurseForge file lists.

| Integration | NeoForge 26.1.2 | NeoForge 26.2 | NeoForge 26.3 | Forge 26.1.2 | Forge 26.2 | Forge 26.3 |
|---|---|---|---|---|---|---|
| BEI (`bei-api`): recipe categories | yes | yes | yes | yes | yes | yes |
| JEI: recipe categories | yes | yes | yes | no build | no build | no build |
| REI: recipe categories | yes | yes | yes | no build | no build | no build |
| EMI | no build | no build | no build | no build | no build | no build |
| SMWTI: block info | yes | yes | yes | yes | yes | yes |
| Jade: block info | yes | yes | yes | no build | no build | no build |
| ModJS: `ThoroughChests` global | yes | yes | yes | yes | yes | yes |
| KubeJS: events bridge | yes | no build | no build | no build | no build | no build |

"no build" means CurseForge lists no file for that Minecraft version and loader, so there is nothing to compile against or run. These are the known gaps. They are added when the other mod ports; the recipes themselves are datapack JSON, so KubeJS and datapacks can already change them wherever the game loads datapacks.

## What each integration shows

- **Recipe viewers.** Every crafting recipe of the mod (copper chest, upgrade cards, every augment level) appears in the viewer's crafting category. Two categories of our own: **Chest Upgrade** (a chest and a card give the next chest) and **Augment Upgrade** (an installed level plus the next item give the next level).
- **SMWTI and Jade.** Level and slots used, the installed augment and level, and the three most common items.
- **ModJS.** See [scripting.md](scripting.md).
- **KubeJS.** Events (`chest_opened`, `chest_closed`, `chest_upgraded`, `augment_installed`, `augment_removed`, `augment_action`) and the same reads as ModJS, through a bridge that is compiled against KubeJS 26.1.2 and loaded by reflection.
- **Other mods' automation.** Pipes and hoppers see every slot of every chest through the loader's item handler (Forge `IItemHandler`, NeoForge `ResourceHandler<ItemResource>`) and through the vanilla `Container` interface.

## Verification status

See [COMPLETION_REPORT.md](COMPLETION_REPORT.md) for what was run and what was not.
