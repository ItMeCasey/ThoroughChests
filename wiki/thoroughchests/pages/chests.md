---
title: Levels and upgrade cards
summary: The five chest levels, their slots and blast resistance, and how upgrade cards work.
---
| Level | Slots (default) | Blast resistance (default) |
|---|---|---|
| Copper | 36 | 6 |
| Iron | 54 | 12 |
| Gold | 72 | 30 |
| Diamond | 108 | 1200 |
| Netherite | 135 | immune to explosions |

Slot counts and resistance are in the config (`level.<id>.slots`, `level.<id>.blast_resistance`). Netherite chests also resist the Wither and the Ender Dragon. A resistance of 3600000 or more means immune.

## Upgrade cards

Use a card on a chest. The card must be exactly one level above the chest, so an iron card upgrades a copper chest, a gold card an iron chest, and so on. A card never skips a level. The contents, the augment, the lock, the filter and every setting stay.

If the config gives a level fewer slots than the chest below it, the chest keeps its size until the extra stacks are emptied, so no item is ever lost.

## Lid, sounds and comparators

The chest uses the vanilla chest model, lid animation and open and close sounds. A comparator reads 0 for an empty chest and 1 to 15 for the fill of the whole inventory, however many slots it has.
