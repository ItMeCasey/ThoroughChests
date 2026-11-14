# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-14
Summary: Thorough Chests, a Forge and NeoForge mod for Minecraft 26.1.2, 26.2 and 26.3 with big scrolling chests, five levels, upgrade cards and six augments, is being built.
Spend: none

## Foundation
- [ ] Design spec, plan, repository and scaffold (w:2)
- [ ] Shared logic in common with unit tests (w:3)
- [ ] Six loader modules build and load (w:3)

## Chests
- [ ] Five chest blocks with lid animation, sounds, openers and comparator (w:4)
- [ ] Configurable slot counts, blast resistance, netherite explosion immunity (w:2)
- [ ] Upgrade cards keep contents and move one level (w:2)
- [ ] Hopper and pipe access to every slot (w:2)
- [ ] Lock key with owner-only access (w:2)

## GUI
- [ ] Scrolling inventory that fits any screen and GUI scale (w:4)
- [ ] Search bar, sort button, quick-stack (w:3)
- [ ] Augment slot, lock slot, filter and toggle controls (w:3)

## Augments
- [ ] Compact (w:3)
- [ ] Furnace levels 1 to 3 (w:3)
- [ ] Blast Furnace levels 1 to 3 (w:2)
- [ ] Magnet levels 1 to 3 (w:2)
- [ ] Feeder levels 1 to 3 (w:2)
- [ ] Void augment with optional link (w:2)
- [ ] Redstone mode for augment activity (w:1)

## Compatibility and scripting
- [ ] Recipe data is datapack JSON for KubeJS and datapacks (w:1)
- [ ] BEI plugin: categories for every recipe (w:2)
- [ ] JEI plugin (NeoForge 26.1.2, 26.2, 26.3) (w:2)
- [ ] REI plugin (NeoForge 26.1.2, 26.2, 26.3) (w:2)
- [ ] EMI plugin (no build exists for 26.x: known gap) (w:1)
- [ ] SMWTI block info (w:1)
- [ ] Jade block info (NeoForge) (w:1)
- [ ] ModJS global: events, reads, content hooks, typed API, docs, tests (w:4)
- [ ] KubeJS bridge, compile-checked where no build exists (w:2)
- [ ] Mod runs without every optional dependency (proved by test) (w:1)

## Testing
- [ ] Unit tests pass in every module (w:2)
- [ ] Headless self test per module prints SELFTEST DONE failures=0 (w:3)
- [ ] Real server self test per build (w:2)
- [ ] Client frames: levels closed and open, GUI, augments, viewer pages (w:3)

## Documentation and listing
- [ ] README, CHANGELOG, LICENSE, issue templates (w:1)
- [ ] Wiki pages under wiki/thoroughchests (w:2)
- [ ] Branding: icon, banner, social, LISTING.md, CurseForge description (w:2)
- [ ] Command Center entry (w:1)
- [ ] COMPLETION_REPORT.md (w:1)

## Release (Project Manager and Casey)
- [ ] GitHub repository created (w:1)
- [ ] CurseForge project created and id recorded (w:1)
- [ ] Push, CI green, tag 2026.11.1 and upload (w:2)
- [ ] Wiki deployed (w:1)
