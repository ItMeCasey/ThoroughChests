# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-14
Summary: Thorough Chests (Forge and NeoForge, Minecraft 26.1.2, 26.2, 26.3) is code complete and self-tested on all six builds; viewer plugins and the wiki are written but not client-checked or deployed, nothing is pushed or released.
Spend: none

## Foundation
- [x] Design spec, plan, repository and scaffold (w:2)
- [x] Shared logic in common with unit tests (w:3)
- [x] Six loader modules build and load (w:3)

## Chests
- [x] Five chest blocks with lid animation, sounds, openers and comparator (w:4)
- [x] Configurable slot counts, blast resistance, netherite explosion immunity (w:2)
- [x] Upgrade cards keep contents and move one level (w:2)
- [x] Hopper and pipe access to every slot (w:2)

## GUI
- [x] Scrolling inventory that fits any screen and GUI scale (w:4)
- [x] Search bar (w:3)
- [x] Chest screen has only the search bar (Casey removed the augment, lock, filter and toggle controls; the lock itself is gone) (w:1)

## Augments
- [x] Compact (w:3)
- [x] Furnace levels 1 to 3 (w:3)
- [x] Blast Furnace levels 1 to 3 (w:2)
- [x] Magnet levels 1 to 3 (w:2)
- [x] Feeder levels 1 to 3 (w:2)
- [x] Void augment with optional link (w:2)
- [x] Redstone mode for augment activity (w:1)

## Compatibility and scripting
- [x] Recipe data is datapack JSON for KubeJS and datapacks (w:1)
- [~] BEI plugin: categories for every recipe (w:2)
- [~] JEI plugin (NeoForge 26.1.2, 26.2, 26.3) (w:2)
- [~] REI plugin (NeoForge 26.1.2, 26.2, 26.3) (w:2)
- [ ] EMI plugin (no build exists for 26.x: known gap) (w:1)
- [~] SMWTI block info (w:1)
- [~] Jade block info (NeoForge) (w:1)
- [x] ModJS global: events, reads, content hooks, typed API, docs, tests (w:4)
- [~] KubeJS bridge, compile-checked where no build exists (w:2)
- [x] Mod runs without every optional dependency (proved by test) (w:1)

## Testing
- [x] Unit tests pass in every module (w:2)
- [x] Headless self test per module prints SELFTEST DONE failures=0 (w:3)
- [x] Real server self test per build (w:2)
- [~] Client frames: levels closed and open, GUI, augments, viewer pages (w:3)

## Documentation and listing
- [x] README, CHANGELOG, LICENSE, issue templates (w:1)
- [~] Wiki pages under wiki/thoroughchests (w:2)
- [x] Branding: icon, banner, social, LISTING.md, CurseForge description (w:2)
- [x] Command Center entry (w:1)
- [x] COMPLETION_REPORT.md (w:1)

## Release (Project Manager and Casey)
- [ ] GitHub repository created (w:1)
- [x] CurseForge project created and id recorded: 1804333 (w:1)
- [ ] Push, CI green, tag 2026.11.1 and upload (w:2)
- [ ] Wiki deployed (w:1)
