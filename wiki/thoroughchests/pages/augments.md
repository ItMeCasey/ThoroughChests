---
title: Augments
summary: The six augments, their levels and how they are installed.
---
A chest holds one augment at a time. Level 1 goes into an empty augment slot. Level 2 goes on top of level 1 of the same augment, and level 3 on top of level 2; the old item is consumed. Put an augment in by clicking it into the slot, by shift-clicking it from your inventory, or by sneak-using it on the chest. Furnace and Blast Furnace never mix because only one augment fits. The installed augment is drawn on the front of the chest, outlined in the color of its level: iron grey for 1, gold for 2, diamond for 3.

| Augment | What it does | Config keys |
|---|---|---|
| Compact | Every pass, crafts items that fill a 2x2 or 3x3 recipe (all the same item) down to the result, so ingots become blocks. By default only recipes that can be crafted back are used (planks do not become crafting tables); `compact.any_recipe` lifts that. | `compact.*` |
| Furnace (1 to 3) | While the chest holds fuel, smelts one item at a time with furnace timing and puts the result back. Higher levels are faster and burn less fuel. | `furnace.speed.N`, `furnace.fuel_use.N` |
| Blast Furnace (1 to 3) | The same with blasting recipes, so ores and metals. | `blast_furnace.speed.N`, `blast_furnace.fuel_use.N` |
| Magnet (1 to 3) | Pulls item entities within a radius into the chest. | `magnet.radius.N`, `magnet.interval.N` |
| Feeder (1 to 3) | Feeds hungry players and friendly animals from food in the chest: players eat, hurt animals heal, babies grow and adults enter love mode. Foods with effects are skipped unless `feeder.effect_foods` is true. | `feeder.range.N`, `feeder.interval.N`, `feeder.breed` |
| Void | Destroys items that do not fit when automation or the magnet puts them in. Sneak-use the augment on a vanilla-style inventory to link it: overflow then goes there instead. Off by default. | `void.enabled_by_default` |

Fuel is an item with a burn time and no recipe of its own for that furnace type. A half-cooked item goes back into the chest if the augment is taken out.
