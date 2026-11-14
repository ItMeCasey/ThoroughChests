# Thorough Chests: completion report

Version 2026.11.1 in all six modules (Forge and NeoForge on Minecraft 26.1.2, 26.2, 26.3). Written 2026-11-14.
The code is finished; nothing is pushed, tagged or released.

## What is built

| Area | Status |
|---|---|
| Five chest blocks (Copper, Iron, Gold, Diamond, Netherite) | Built. One block with one inventory, vanilla chest model, lid animation and sounds, open counting, comparator signal across the whole inventory, slot counts 36 / 54 / 72 / 108 / 135 (config), blast resistance rising per level, Netherite immune (also Wither and dragon tags) |
| Upgrade cards | Built. One card, one level, contents kept; wrong or skipped cards refused |
| Chest screen | Built. A scrolling window, a search bar (`name`, `@mod`, `#tag`) and the player inventory, nothing else (Casey: no sort, no quick-stack, no augment, lock, filter, redstone or link controls). Rows fit the window and GUI scale; the server remembers the size per player |
| Augments | Built, all by sneak-use on the chest (sneak-use with an empty hand takes it back): Compact, Furnace 1-3, Blast Furnace 1-3, Magnet 1-3, Feeder 1-3, Void. One per chest, level n only on level n-1 |
| Augment overlay | Built. The installed augment is drawn as a 4x4 pixel plate in the top right back corner of the lid, hinged on the lid, outlined in the level color (iron, gold, diamond); synced to clients by the block entity update packet |
| Chest lock | Built. Owner name stored, owner and operators only, sneak-use to install and take back |
| Hoppers and pipes | Built. Hoppers use the `Container`; Forge `IItemHandler` and NeoForge `ResourceHandler<ItemResource>` expose every slot; Void swallows overflow for pipes and clears stuck hoppers |
| Textures | Generated from vanilla textures by `scripts/make_textures.py` and `scripts/item_art.py` (chest frames from the level below, icons, overlays). Not approved |
| Config | `config/thoroughchests-common.toml` from NeoForge `ModConfigSpec` (with the NeoForge config screen) and Forge `ForgeConfigSpec`, built from `common/Settings.java`; the old `thoroughchests.properties` seeds a new file once and is never touched. NeoForge 26.3 uses its renamed `LOCAL` type with the same file name |
| Scripting | ModJS global `ThoroughChests` (events, reads, item rules, settings), typed API file, docs; KubeJS bridge (writes a startup script when KubeJS is present, never overwrites); recipes are datapack JSON |
| Viewers and info | BEI categories (all six builds), JEI and REI categories, Jade (NeoForge), SMWTI (all six builds) |
| Docs and listing | README, CHANGELOG, LICENSE, DESIGN, COMPATIBILITY, scripting, wiki pages under `wiki/thoroughchests`, CurseForge description, listing kit, issue templates, branding (icon, banners) |

## What was verified, and how

- **Unit tests** (`./gradlew test build` through `gradle-slot.sh`, exit 0 in all six modules): levels, slot config, blast resistance, augment rules, furnace timing, compaction math, window mapping and search, GUI row fitting, comparator math, lock rule, script events and item rules, config definitions (ranges, defaults, key shape, legacy import leaves the old file untouched), KubeJS helper writing, and asset tests (every texture, model, overlay region and centering).
- **Real dedicated server self test** (`scripts/server_check.py`, ports 8760 to 8765): `SELFTEST DONE failures=0` with 135 checks on each of the six builds. It covers every level and slot count, comparator, save and load (block entity NBT, chunk-style round trip, config shrink never loses items), upgrade cards across all levels, locks, installs and upgrades of every augment level, taking an augment back, hopper access past slot 100, Compact, Furnace and Blast Furnace timing and fuel, Magnet radius and levels, Feeder (animals and players), Void (including stuck hoppers), the menu window (scroll, search by name, mod and tag, clicks, shift-click, rows), the scripting API, explosions (copper, iron and gold destroyed; diamond and netherite survive a power 12 blast).
- **ModJS present and absent**: the same self test passes without ModJS, and with ModJS 2026.11.31 on NeoForge 26.2 the global registers, a script reads 5 levels and 6 augments and receives `chest_upgraded` events.
- **Real client (NeoForge 26.2 only)**: the tour (`scripts/client_tour.py`, hidden desktop) saved 37 frames under `docs/frames/neoforge-26.2`: every level closed and open, the overlay of every augment and level (plus lid open), the chest screen (scrolled, searched, GUI scale 1 and 2). The overlay was checked in a frame after fixing its face and orientation.
- Release jars hold no dev classes; Forge jars carry `pack.mcmeta`.

## Not verified

- No client run on Forge, nor on 26.1.2 or 26.3 (they compile, run the server self test and pass the asset tests).
- GUI scales above 2: the test window (854x480) caps the scale at 2, so scales 3 to 5 were not seen; the fit logic is unit tested for every size and scale.
- JEI, REI, BEI, SMWTI and Jade plugins compile and sit in the jars; no viewer or overlay was opened on a client (no viewer frames). KubeJS was not run (it only exists for NeoForge 26.1.2).
- The wiki pages are link-checked by script, not viewed in a browser, and not deployed.
- Config sync: the config is COMMON, not synced; no client code needs the values (the screen takes sizes from the menu).
- Void forwarding to a linked inventory, the on/off switch, redstone mode and item filters were built first and then removed on Casey's request; none exists now.
- EMI, and JEI, REI and Jade on Forge, have no 26.x builds (known gaps, listed in `docs/COMPATIBILITY.md`).

## Decisions worth knowing

- Compact only uses recipes that can be crafted back by default (ingots to blocks, not planks to crafting tables); `compact.any_recipe` lifts it.
- A furnace augment also smelts the products it makes when they have a recipe (stone to smooth stone), because anything in the chest counts as an ingredient.
- 26.3 changed fuel burn times (loot-context resolved) and blasting times; the code handles both (`scripts/variants/Fuel263.java`).

## What Casey and the Project Manager still do

- Create the GitHub repository (public or private; the CurseForge description images load from `ItMeCasey/ThoroughChests` on `main`, so `branding/` must be pushed first).
- CurseForge project 1804333 exists; add the `CURSEFORGE_TOKEN`, `IMC_MAVEN_USER` and `IMC_MAVEN_PASSWORD` repository secrets, paste `curseforge/DESCRIPTION.md`, upload the logo and gallery images from `branding/`.
- Push, let CI pick the builds from the diff, tag `v2026.11.1` when Casey says so.
- Deploy the wiki folder `wiki/thoroughchests` (and add `thoroughchests` to `wiki/mods.json` of the site).
- Look at the frames and decide on the textures; none is marked approved.
- Optionally: client runs of Forge, 26.1.2 and 26.3, and of the viewers.
