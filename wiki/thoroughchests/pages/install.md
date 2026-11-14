---
title: Install and versions
summary: Which Minecraft versions and loaders are supported and what is optional.
---
Put the jar for your Minecraft version in the `mods` folder of the server and of every client. One jar holds both the Forge and the NeoForge build of a Minecraft version.

| Minecraft | NeoForge | Forge |
|---|---|---|
| 26.1.2 | yes | yes |
| 26.2 | yes | yes |
| 26.3 | yes | yes |

Nothing else is required. Recipe viewers, info overlays and script mods are optional; see [[compatibility]].

## Known gaps

| Integration | Missing on |
|---|---|
| JEI, REI, Jade | Forge (no builds exist for these Minecraft versions) |
| EMI | every version (no build exists yet) |
| KubeJS events bridge | everything except NeoForge 26.1.2 (no other KubeJS builds exist yet) |

These are added when the other mods port. The recipes are datapack JSON, so datapacks work everywhere.
