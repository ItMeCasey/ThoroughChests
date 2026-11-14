# Thorough Chests design

Mod id `thoroughchests`, version 2026.11.1, MIT, both sides. Forge and NeoForge on Minecraft 26.1.2, 26.2, 26.3.
Decisions marked (Casey) were made before the build and are not open. Decisions marked (design) are the builder's choices where the brief left room.

## Content

| Thing | Ids |
|---|---|
| Chests (Casey) | `copper_chest`, `iron_chest`, `gold_chest`, `diamond_chest`, `netherite_chest` |
| Upgrade cards (Casey) | `iron_upgrade_card`, `gold_upgrade_card`, `diamond_upgrade_card`, `netherite_upgrade_card`; a card moves a chest to the level in its name |
| Augments (Casey) | `compact_augment`, `furnace_augment_1..3`, `blast_furnace_augment_1..3`, `magnet_augment_1..3`, `feeder_augment_1..3`, `void_augment` |
| Lock | `chest_lock` (design) |

### Levels and slots (Casey)
Default slots Copper 36, Iron 54, Gold 72, Diamond 108, Netherite 135, all in the config (`level.<id>.slots`, 1 to 512). A chest is one block with one inventory; there are no double chests.
Blast resistance rises with every level (config `level.<id>.blast_resistance`): copper 6, iron 12, gold 30, diamond 1200, netherite 3600000, and netherite is also immune to the Wither and the dragon (tags) and never drops from an explosion because it is never destroyed.
Containers never lose items: a chest saved with more stacks than its configured size (a config change) keeps the larger size until the extra stacks are emptied.

### Recipes (design; all datapack JSON, so datapacks and KubeJS can change them)
- Copper chest: vanilla chest in the middle, eight copper ingots around it.
- Upgrade card for level X: four X ingots (diamond for diamond) in a plus shape around one paper.
- Chest lock: iron ingot over tripwire hook over gold ingot (a column).
- Augments, shape `ABA / BCB / ABA`: A = tier material, B = redstone, C = the augment's heart.
  Level 1: A iron ingot, level 2: gold ingot, level 3: diamond. Hearts: compact crafting table (single level, A iron ingot), furnace furnace, blast furnace blast furnace, magnet ender pearl, feeder hopper. Void: obsidian heart, A iron ingot, single level.
- A level 2 or 3 augment is a full item of its own (its recipe does not contain the lower level) and is applied on top of the installed lower level.

### Upgrading a chest (Casey)
Right-click a chest with a card: the card must be exactly the next level. The block is replaced by the next level's block; the inventory, augment, lock, filter and settings are kept. Skipping levels is impossible (one card, one level). Higher chests have no recipe at all.

## Block and block entity
One class `TcChestBlock` with a `ChestLevel`, state `facing` (horizontal, placed facing the player) and `waterlogged`. Shape is the vanilla single chest shape. Render shape invisible; a block entity renderer draws the vanilla chest model (the vanilla `chest` model layer) with the level's texture and the vanilla lid animation and sounds.
`TcChestBlockEntity implements Container, LidBlockEntity`: a `ContainerOpenersCounter` (open and close sounds, lid synced with a block event, recheck ticks while open), a `ChestLidController`, comparator signal scaled across all slots.
- Comparator: 0 when empty, otherwise `1 + floor(14 * fill)` where fill is the sum of `count / maxStack` over all slots divided by the slot count.
- Hoppers use the `Container` interface directly (all slots). The loader layers expose the same container as the loader's item handler (`InvWrapper` on Forge, the transfer API's `VanillaContainerWrapper` on NeoForge) so pipes see every slot. Insertions through automation run through the Void augment (see below).

## GUI (design, Casey asked for scroll, search, fit any screen)
Our own `TcChestMenu` and `TcChestScreen`, independent of JEI, REI, EMI and BEI.
- The menu has a window of `rows * 9` chest slots and the player inventory, nothing else (Casey: only a search bar; augments and the lock never appear in the screen). The window shows container slots `(scroll + row) * 9 + col` of the filtered list; clicks are resolved by the server, which keeps the window mapping, so shift-click, drag, double-click and hoppers agree.
- Rows adapt to the screen. Menu types exist for 1 to 12 rows. The screen computes how many rows fit the current scaled height; if that differs from the menu it asks the server (menu button) to reopen with that many rows and the server remembers the player's row count for the session. The default is 6. Width is always 9 columns (176 wide plus a side panel that moves under the main panel when the screen is too narrow). Because the layout is computed from the real scaled size, it fits any window size and any GUI scale; the smallest case keeps 1 row plus the player inventory and a compact player inventory when even that cannot fit.
- Scrolling: mouse wheel over the window, a scrollbar, and Page keys. Sent to the server as menu button ids (no custom payload).
- Search bar: an `EditBox`; the text is sent to the server as characters (button ids) and the server filters by item name, `@modid`, `#tag`. Search never touches the viewers' boxes.
- No hint, tutorial or explanatory text anywhere in the UI; buttons show icons and plain names, values show numbers.

## Augments (Casey)
One augment per chest. Install rule (pure, in `AugmentRules`): level 1 of anything needs an empty augment slot; level 2 needs level 1 of the same augment installed; level 3 needs level 2. The upgrade consumes the old item. Shift-clicking an augment from the inventory applies this rule; right-clicking a chest while sneaking with an augment in hand does the same. Taking the augment out returns the installed item. Furnace and Blast Furnace cannot coexist because only one augment fits.
Augments have no switches, no redstone mode and no filters (Casey: no interface for them). Config holds every number.

| Augment | Behavior | Levels | Config |
|---|---|---|---|
| Compact | Every interval, for each item with a uniform crafting recipe (2x2 or 3x3 filled with that one item), crafts the result and puts it back. | 1 | interval, `compact.any_recipe` |
| Furnace | While fuel is in the chest, takes an item with a smelting recipe, cooks it with furnace timing, puts the result back. | 1-3 | speed, fuel use per level |
| Blast Furnace | Same with blasting recipes (ores, metals). | 1-3 | speed, fuel use per level |
| Magnet | Pulls item entities within a radius into the chest. | 1-3 | radius, cadence per level |
| Feeder | Feeds players (hunger) and friendly animals (heal, grow, breed) from food in the chest. | 1-3 | range, cadence per level |
| Void | Destroys items that do not fit when inserted by automation (hoppers, pipes, magnet) and clears hoppers stuck on a full chest. | 1 | none |

Compact decision (design): by default only compacts into results that can be crafted back into at least as many of the same items (ingots to blocks, not planks to crafting tables); `compact.any_recipe=true` allows any uniform recipe whose result has fewer items than it consumes. Filters protect either way.
Furnace details: fuel is any item with a fuel value (`fuelValues`), preferring items without a smelting recipe; an item is never both fuel and ingredient at once; fuel remainders (buckets) return to the chest; a finished item waits if there is no room. Time per item is the recipe time divided by the level's speed, and fuel burned per item is the recipe time times the level's fuel factor.

## Lock (design: Casey left the form open)
A separate key item, `chest_lock`, held apart from the augment, so it never competes with the one-augment limit. Sneak-using a lock on a chest as player P stores P's UUID and name as owner (shown by SMWTI and Jade). Only the owner (and operators when `lock.ops_bypass`) opens, breaks or takes the lock; others get a short action bar message. Automation (hoppers, pipes) still works unless `lock.block_automation` is true. Explosions follow the level rules.

## Redstone (Casey)
Comparator signal as above; augments ignore redstone.

## Compatibility (Casey, global rule)
All optional, by plugin annotation or reflection; the mod runs the same with none installed, and the headless self test runs a build without any of them.
- Recipe viewers show every recipe (chests, cards, lock, augments at every level) in their crafting categories, plus two categories of our own: Chest Upgrade (chest + card gives the next chest) and Augment Upgrade (installed level + next item gives the next level), and the Void link has an info page. BEI through `bei-api` on all six builds, JEI and REI on NeoForge 26.1.2, 26.2, 26.3. EMI, JEI and REI on Forge, and KubeJS outside NeoForge 26.1.2 have no 26.x builds (checked 2026-11-14), so those are known gaps listed in `docs/COMPATIBILITY.md`.
- SMWTI block info on all six builds; Jade on NeoForge. Lines: level, used slots, augment and level, owner, state.
- ModJS global `ThoroughChests` (installed through `ExtraBindings.register`): events (`chest_opened`, `chest_closed`, `chest_upgraded`, `augment_installed`, `augment_removed`, `augment_action`, `chest_locked`, `chest_unlocked`), reads (chest info at a position, settings), content hooks (item rules: allow or refuse items per chest level, set augment numbers through settings), typed API file for editors, docs and tests.
- KubeJS: recipes and tags are datapack JSON; a bridge class registers the same events for KubeJS where a KubeJS build exists (NeoForge 26.1.2), compile-checked there; elsewhere datapacks are the interface.
- Wiki pages Compatibility and Scripting under `wiki/thoroughchests`.

## Config (`config/thoroughchests-common.toml`)
Slots, blast resistance, every augment number per level, intervals, void default, lock options, compact mode, feeder behavior (breed on or off, effect foods). Built from `common/Settings.java`, the single list of keys, defaults, ranges and comments: NeoForge `ModConfigSpec` (with the in-game config screen) and Forge `ForgeConfigSpec`, registered as COMMON, with the values pushed into `Settings` when the file loads or reloads. The values are used on the server; the client needs none of them (the screen takes slot counts from the menu). An old `thoroughchests.properties` is read once to seed the defaults of a new file and is never changed or deleted.

## Textures (Casey)
Vanilla textures only, generated by `scripts/make_textures.py` and regenerable. Every chest texture is the vanilla chest texture with a border frame built from the texture of the level below: Copper takes the frame from the vanilla planks look, Iron a copper frame, Gold an iron frame, Diamond a gold frame, Netherite a diamond frame. Palettes are sampled from the vanilla block textures so the shading stays vanilla; nothing is drawn in an invented style. The model is the vanilla chest model and animation. Item icons, GUI buttons and the GUI sheet are built the same way. Textures are never marked approved.

## Tests
- Unit tests (common): levels, slot config, augment rules, config definitions, furnace simulation, compaction math, window mapping, search matching, GUI row fitting, settings, script events.
- Headless self test (`SELFTEST DONE failures=0`) in every module, dev tools only: chests per level, slot counts, upgrade cards, each augment and level, filters, locks, explosion resistance, persistence on upgrade and on world save and load (save to NBT and reload), comparator, hopper access, menu window and search, scripts.
- Real server self test per build with `scripts/server_check.py` (ports 8760 to 8765).
- Client frames through the project tour hook with `launch-isolated.ps1`, at most two clients, saved under `docs/frames`.

## Out of scope (listed, not built)
Double chests, chest linking, wireless access, upgrade cards that skip levels, per-player chests.
