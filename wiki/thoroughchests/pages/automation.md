---
title: Hoppers, pipes and redstone
summary: How automation and redstone see a chest.
---
## Hoppers and pipes

Vanilla hoppers see the chest as a normal container with every slot, so a hopper can fill and empty slots beyond the first hundred. Pipes from other mods see every slot through the loader item handler (Forge `IItemHandler`, NeoForge `ResourceHandler<ItemResource>`).

With a Void augment installed, automation never blocks on a full chest: the overflow is destroyed (see [[augments]]). A hopper pointing into a full chest has its stuck items cleared the same way.

## Comparators

A comparator beside the chest reads 0 for an empty chest and 1 to 15 for the fill of the whole inventory.

## Redstone

Augments work whether or not the chest is powered. The comparator signal is the only redstone feature.
