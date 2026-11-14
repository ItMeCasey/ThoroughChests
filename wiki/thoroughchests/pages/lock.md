---
title: The chest lock
summary: An owner-only lock in its own slot.
---
The Chest Lock has its own slot beside the augment slot, so it never counts against the one-augment limit.

Put a lock in the slot, or sneak-use one on a chest, and you become the owner. The owner name is stored with the chest and shown by SMWTI and Jade.

| Action | Owner | Operators | Everyone else |
|---|---|---|---|
| Open the chest | yes | yes | no |
| Mine the chest | yes | yes (setting `lock.ops_bypass`) | no |
| Take the lock out | yes | yes | no |
| Upgrade the chest | yes | yes | no |

Hoppers and pipes keep working on a locked chest unless `lock.block_automation` is `true`. Explosions follow the level rules, so a locked copper chest can still be blown up. Creative players can break any block.
