# Scripting (ModJS and KubeJS)

Thorough Chests adds a global `ThoroughChests` to ModJS server scripts when ModJS is installed. Without ModJS nothing is loaded. Client scripts do not get the global, because chests live on the server. A typed description for editors is in [thoroughchests.d.ts](thoroughchests.d.ts).

## Events

`ThoroughChests.on(name, fn)` calls `fn(event)` on the server thread. The event has `type()`, `get(key)`, `string(key)`, `number(key)` and `keys()`. A listener that throws is logged on one line and skipped.

| Event | Keys |
|---|---|
| `chest_opened`, `chest_closed` | `dimension`, `x`, `y`, `z`, `level` (first player opens, last player closes) |
| `chest_upgraded` | `dimension`, `x`, `y`, `z`, `level` (the new level), `from`, `to` |
| `augment_installed` | `dimension`, `x`, `y`, `z`, `level`, `augment`, `augment_level` |
| `augment_removed` | `dimension`, `x`, `y`, `z`, `level` |
| `augment_action` | `dimension`, `x`, `y`, `z`, `augment`, `augment_level`, `action` (`compact`, `smelt`, `blast`, `magnet`, `feed_player`, `feed_animal`, `void`), `item`, `count` |
| `chest_locked`, `chest_unlocked` | `dimension`, `x`, `y`, `z`, `level`, `owner` |

`ThoroughChests.events()` lists the names. `augment_action` fires for every pass that did something, so a busy magnet fires often; register it only when you need it.

## Reading chests

- `ThoroughChests.chest(dimension, x, y, z)` returns a read-only map for the chest at that position, or `null` when there is none (or the server has not started): `level`, `slots`, `used`, `augment` (empty text for none), `augmentLevel`, `augmentEnabled`, `redstone` (`ignore`, `when_powered`, `when_unpowered`), `locked`, `owner`, `open` (players with it open), `comparator`.
- `ThoroughChests.count(dimension, x, y, z, itemId)` counts one item in a chest (`-1` when there is no chest).
- `ThoroughChests.levels()` and `ThoroughChests.augments()` list the ids. `ThoroughChests.slots(level)` gives the configured slot count of a level.

## Rules and settings

- `ThoroughChests.addItemRule(fn)`: when `fn(itemId)` returns `false`, no chest takes that item (players, hoppers, pipes and augments alike).
- `ThoroughChests.settingKeys()`, `setting(key)` and `setSetting(key, value)` read and change the values of `config/thoroughchests.properties` until the game closes (the file is not changed). Slot counts apply to chests created or loaded afterwards; augment numbers apply at once.

## Example

```js
ThoroughChests.on('chest_upgraded', e => {
  console.log('Chest at ' + e.number('x') + ' ' + e.number('y') + ' ' + e.number('z') + ' is now ' + e.string('to'));
});

ThoroughChests.on('augment_action', e => {
  if (e.string('action') === 'void') {
    console.log('Voided ' + e.number('count') + ' ' + e.string('item'));
  }
});

ThoroughChests.addItemRule(id => id !== 'minecraft:bedrock');

ThoroughChests.setSetting('magnet.radius.3', '20');
```

## KubeJS

Recipes (the copper chest, the upgrade cards, the lock and every augment) are datapack JSON, so KubeJS and datapacks add, change and remove them like any other recipe. When KubeJS is installed, Thorough Chests writes `kubejs/startup_scripts/thoroughchests.js` once (an existing file is never overwritten). It puts the same API in `global.ThoroughChests`:

```js
// server_scripts
global.ThoroughChests.on('chest_upgraded', e => console.log(e.string('to')))
```

The bridge has no compile-time link to KubeJS, so it is compiled in every build and only does something where a KubeJS build exists (NeoForge 26.1.2 at the time of writing; see [COMPATIBILITY.md](COMPATIBILITY.md)).

## Left for later

Adding chest levels or augments from scripts needs registry changes at startup and is not part of this release.
