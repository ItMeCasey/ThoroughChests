"""Writes the resource files of Thorough Chests that are not textures: blockstates, models, item definitions, language, recipes, loot
tables and tags. Textures come from make_textures.py.

Shared files go to common/src/main/resources; the per-version data (loot tables, whose format differs in 26.3) goes to every
version-<mc> folder. Run it again after changing a recipe or a name; it overwrites what it writes.
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MOD = "thoroughchests"
COMMON = os.path.join(ROOT, "common", "src", "main", "resources")
ASSETS = os.path.join(COMMON, "assets", MOD)
VERSIONS = {"26.1.2": "old", "26.2": "old", "26.3": "new"}
LEVELS = ["copper", "iron", "gold", "diamond", "netherite"]
PARTICLE = {"copper": "copper_block", "iron": "iron_block", "gold": "gold_block", "diamond": "diamond_block", "netherite": "netherite_block"}
AUGMENTS = [("compact", 1, "Compact"), ("furnace", 3, "Furnace"), ("blast_furnace", 3, "Blast Furnace"), ("magnet", 3, "Magnet"),
            ("feeder", 3, "Feeder"), ("void", 1, "Void")]
ROMAN = {1: "I", 2: "II", 3: "III"}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        if isinstance(data, str):
            f.write(data)
        else:
            json.dump(data, f, indent=2)
            f.write("\n")


def augment_items():
    for kind, maxlevel, title in AUGMENTS:
        for lv in range(1, maxlevel + 1):
            name = f"{kind}_augment" if maxlevel == 1 else f"{kind}_augment_{lv}"
            label = f"{title} Augment" if maxlevel == 1 else f"{title} Augment {ROMAN[lv]}"
            yield kind, lv, name, label


def item_names():
    names = {}
    for lv in LEVELS:
        names[f"{lv}_chest"] = f"{lv.capitalize()} Chest"
    for lv in LEVELS[1:]:
        names[f"{lv}_upgrade_card"] = f"{lv.capitalize()} Upgrade Card"
    for _, _, name, label in augment_items():
        names[name] = label
    return names


def m(item):
    return item if ":" in item else f"minecraft:{item}"


def shaped(pattern, key, result, count=1):
    return {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
            "key": {k: m(v) for k, v in key.items()}, "result": {"id": f"{MOD}:{result}", "count": count}}


def recipes():
    out = {"copper_chest": shaped(["CCC", "CHC", "CCC"], {"C": "copper_ingot", "H": "chest"}, "copper_chest")}
    material = {"iron": "iron_ingot", "gold": "gold_ingot", "diamond": "diamond", "netherite": "netherite_ingot"}
    for lv, mat in material.items():
        out[f"{lv}_upgrade_card"] = shaped([" X ", "XPX", " X "], {"X": mat, "P": "paper"}, f"{lv}_upgrade_card")
    tier = {1: "iron_ingot", 2: "gold_ingot", 3: "diamond"}
    heart = {"compact": "crafting_table", "furnace": "furnace", "blast_furnace": "blast_furnace", "magnet": "ender_pearl",
             "feeder": "hopper", "void": "obsidian"}
    for kind, lv, name, _ in augment_items():
        out[name] = shaped(["ABA", "BCB", "ABA"], {"A": tier[lv], "B": "redstone", "C": heart[kind]}, name)
    return out


def loot(block, style):
    if style == "old":
        pool = {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}]}
    else:
        pool = {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block}"}]}
    return {"type": "minecraft:block", "pools": [pool], "random_sequence": f"{MOD}:blocks/{block}"}


LANG_EXTRA = {
    "itemGroup.thoroughchests": "Thorough Chests",
    "message.thoroughchests.wrong_card": "Wrong card for this chest",
    "gui.thoroughchests.search": "Search",
    "gui.thoroughchests.on": "On",
    "gui.thoroughchests.off": "Off",
    "gui.thoroughchests.redstone.ignore": "Always",
    "gui.thoroughchests.redstone.when_powered": "Powered",
    "gui.thoroughchests.redstone.when_unpowered": "Inverse",
    "gui.thoroughchests.allow": "Allow",
    "gui.thoroughchests.deny": "Deny",
    "gui.thoroughchests.used": "%s/%s",
    "gui.thoroughchests.slot.lock": "Lock",
    "gui.thoroughchests.slot.augment": "Augment",
    "gui.thoroughchests.slot.filter": "Filter",
    # Information lines (SMWTI and Jade)
    "smwti.thoroughchests.level": "%s Chest, %s of %s slots",
    "smwti.thoroughchests.augment": "%s",
    "smwti.thoroughchests.augment_off": "%s (off)",
    "smwti.thoroughchests.contents": "%s x %s",
    "jade.thoroughchests.level": "%s Chest",
    # Recipe viewers
    "viewer.thoroughchests.chest_upgrade": "Chest Upgrade",
    "viewer.thoroughchests.augment_upgrade": "Augment Upgrade",
    "viewer.thoroughchests.augment_install": "Augment",
    "viewer.thoroughchests.void_link": "Void Link",
}


def lang():
    out = dict(LANG_EXTRA)
    for key, name in item_names().items():
        kind = "block" if key.endswith("_chest") else "item"
        out[f"{kind}.{MOD}.{key}"] = name
    return dict(sorted(out.items()))


def main():
    names = item_names()
    # Blockstates and models
    for lv in LEVELS:
        variants = {}
        for facing in ("north", "east", "south", "west"):
            for wet in ("false", "true"):
                variants[f"facing={facing},waterlogged={wet}"] = {"model": f"{MOD}:block/{lv}_chest"}
        write(os.path.join(ASSETS, "blockstates", f"{lv}_chest.json"), {"variants": variants})
        write(os.path.join(ASSETS, "models", "block", f"{lv}_chest.json"), {"textures": {"particle": f"minecraft:block/{PARTICLE[lv]}"}})
        write(os.path.join(ASSETS, "models", "item", f"{lv}_chest.json"),
              {"parent": "minecraft:item/template_chest", "textures": {"particle": f"minecraft:block/{PARTICLE[lv]}"}})
        write(os.path.join(ASSETS, "items", f"{lv}_chest.json"),
              {"model": {"type": "minecraft:special", "base": f"{MOD}:item/{lv}_chest", "model": {"type": "minecraft:chest", "texture": f"{MOD}:{lv}"}}})
    for key in names:
        if key.endswith("_chest"):
            continue
        write(os.path.join(ASSETS, "models", "item", f"{key}.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{key}"}})
        write(os.path.join(ASSETS, "items", f"{key}.json"), {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{key}"}})
    write(os.path.join(ASSETS, "lang", "en_us.json"), lang())
    # The chest texture folder is stitched into the chest atlas by the vanilla atlas definition (directory source entity/chest).
    # Data: recipes and loot per version, tags shared
    for version, style in VERSIONS.items():
        base = os.path.join(ROOT, f"version-{version}", "src", "main", "resources", "data", MOD)
        for name, r in recipes().items():
            write(os.path.join(base, "recipe", name + ".json"), r)
        for lv in LEVELS:
            write(os.path.join(base, "loot_table", "blocks", f"{lv}_chest.json"), loot(f"{lv}_chest", style))
    chests = [f"{MOD}:{lv}_chest" for lv in LEVELS]
    write(os.path.join(COMMON, "data", "minecraft", "tags", "block", "mineable", "axe.json"), {"replace": False, "values": chests})
    write(os.path.join(COMMON, "data", "minecraft", "tags", "block", "wither_immune.json"), {"replace": False, "values": [f"{MOD}:netherite_chest"]})
    write(os.path.join(COMMON, "data", "minecraft", "tags", "block", "dragon_immune.json"), {"replace": False, "values": [f"{MOD}:netherite_chest"]})
    print("resources written")


if __name__ == "__main__":
    main()
