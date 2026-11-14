---
title: Filters, redstone and switches
summary: The per-chest filter, redstone mode and on-off switch.
---
## Filter

The nine filter slots under the augment slot hold ghost copies of items. The mode button picks **Allow** (only the listed items) or **Deny** (everything but the listed items). An empty filter lets everything through. The filter applies to Compact (what it compacts), Magnet (what it picks up), Feeder (what it uses as food) and Void (what it destroys). The furnaces ignore it.

## Redstone mode

| Button | Meaning |
|---|---|
| Always | Redstone is ignored |
| Powered | The augment works only while the chest is powered |
| Inverse | The augment works only while the chest is not powered |

## On and off

The On/Off button switches the augment for this one chest. The Void augment starts switched off (setting `void.enabled_by_default`).
