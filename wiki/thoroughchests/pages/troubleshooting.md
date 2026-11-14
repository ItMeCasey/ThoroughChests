---
title: Troubleshooting
summary: Common questions.
---
## An augment does nothing

- Check that the augment is installed: the overlay on the lid shows it.
- A furnace augment needs fuel in the chest that has no smelting recipe of its own (coal, charcoal, blocks of coal).
- A Magnet only takes items that no longer have a pickup delay.
- Compact only uses recipes that can be crafted back unless `compact.any_recipe` is true.

## Hoppers or pipes skip the chest

The chest is full and has no Void augment.

## The chest opens with a different number of rows

The screen fits itself to the window and GUI scale. See [[screen]].

## A slot count changed after editing the config

Existing chests with more items than the new slot count keep their size until the extra stacks are emptied.
