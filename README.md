# Thorough Chests

Chests with big scrolling inventories, five levels from copper to netherite, upgrade cards, and augments that compact, smelt, attract, feed and void items. For Minecraft on Forge and NeoForge (26.1.2, 26.2, 26.3), on both the client and the server.

Docs: https://itmecasey.com/wiki/thoroughchests/. Listing text: [`curseforge/DESCRIPTION.md`](curseforge/DESCRIPTION.md). Design: [`docs/DESIGN.md`](docs/DESIGN.md).

## What it adds

| Block or item | What it does |
|---|---|
| Copper, Iron, Gold, Diamond and Netherite Chest | One block with one inventory: 36, 54, 72, 108 and 135 slots by default. Blast resistance rises with each level and Netherite is immune to explosions. Vanilla lid animation and sounds, comparator signal across the whole inventory, hoppers and pipes reach every slot |
| Upgrade cards | One card moves a chest exactly one level and keeps everything in it. Only the copper chest has a crafting recipe (a vanilla chest and eight copper ingots) |
| Chest screen | Scrolling window with a search bar (name, `@mod`, `#tag`); it fits any window size and GUI scale |
| Chest Lock | Its own slot beside the augment slot; only the owner (and operators) opens, mines or unlocks the chest |
| Compact augment | Crafts uniform 2x2 and 3x3 recipes down (ingots to blocks) |
| Furnace and Blast Furnace augments, levels 1 to 3 | Smelt what is in the chest while it holds fuel; higher levels are faster and use less fuel |
| Magnet augment, levels 1 to 3 | Pulls item entities in from a radius |
| Feeder augment, levels 1 to 3 | Feeds hungry players and friendly animals from the chest's food |
| Void augment | Destroys overflow, or passes it to a linked inventory; off by default |

One augment per chest. Level 2 goes on top of level 1 and level 3 on top of level 2; the old augment is consumed. Every augment can be switched off per chest, gated by redstone, and the Compact, Magnet, Feeder and Void augments use the chest's whitelist or blacklist.

Scripting: with ModJS installed, server scripts get a `ThoroughChests` global; see [docs/scripting.md](docs/scripting.md). KubeJS: recipes are datapack JSON and the events have a bridge where KubeJS has a build.

Optional integrations: BEI, JEI and REI recipe categories, Show Me What This Is and Jade info lines. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for what exists per version.

## Build

One module per loader and Minecraft version (`neoforge-26.2`, `forge-26.2`, and so on), sharing `common/` (Minecraft-free logic and unit tests) and the master code in `version-26.2`. Never edit the copies in `version-26.1.2` and `version-26.3`; run `python scripts/sync_versions.py` after changing `version-26.2` (loader code lives in the 26.1.2 layers and is copied the same way).

Textures are generated from vanilla textures by `python scripts/make_textures.py`; other resources by `python scripts/make_assets.py`.

```
cd neoforge-26.2
./gradlew build
./gradlew test
```

Development tools (self test, screenshot tour) build only with `-PdevTools`, so release jars never contain them. `python scripts/server_check.py <layer>` runs the self test on a real dedicated server.

## License

MIT, see [LICENSE](LICENSE).
