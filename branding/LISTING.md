# Thorough Chests listing kit: CurseForge

Everything needed to fill in the CurseForge project form. Files are in `branding/icon/` and `branding/banners/`.

## Project fields

| Field | CurseForge |
|---|---|
| Project ID | 1804333 (slug `thorough-chests`; also set in `.github/scripts/publish_curseforge.py`) |
| Name | Thorough Chests |
| Slug / URL | `thoroughchests` |
| Mod id | `thoroughchests` |
| Icon / logo | `icon/icon_512.png` (square, at least 400x400) |
| Class | Mods |
| Main category | Storage |
| Loaders | Forge and NeoForge |
| Game versions | Minecraft 26.1.2, 26.2 and 26.3, one jar per Minecraft version; set on each uploaded file |
| Environment | Client and Server (the upload script sends both) |
| License | MIT |
| Source | https://github.com/ItMeCasey/ThoroughChests |
| Issues | https://github.com/ItMeCasey/ThoroughChests/issues |
| Wiki | https://itmecasey.com/wiki/thoroughchests/ (not deployed yet; add the link once it is live) |

### One-line description

> Big chests that scroll and search, five levels from copper to netherite, and augments that compact, smelt, attract, feed and void items, for Forge and NeoForge.

Use it for the CurseForge summary (under 250 characters), social posts and the top of the description.

### Gallery

| File | Use |
|---|---|
| `banners/features.png` (1920x1080) | First gallery image: the augments and cards |
| `banners/levels.png` (1920x1080) | The five levels, second gallery image |
| `banners/ingame.png` (1920x1080) | Third gallery image: game screenshots of the levels, the lid overlays and the chest screen |
| `banners/social.png` (1280x640) | GitHub social preview (repo Settings > Social preview) |
| `banners/banner.png` (800x150, saved at 2x) | Top of the description |
| `banners/h_*.png` (800x46, saved at 2x) | Optional section headers (features, levels, augments, config, compat, faq); the description uses plain `#` headers |

`features`, `levels`, `social` and the headers are drawn for the branding from the mod's own textures. `ingame.png` uses frames from `docs/frames/neoforge-26.2`.

### Description

The CurseForge description is in [`curseforge/DESCRIPTION.md`](../curseforge/DESCRIPTION.md). Paste it with the description type set to **Markdown**. Its images load from GitHub, so `branding/` has to be pushed first.

## Checklist

1. Push `branding/` to GitHub so the description images load.
2. Create the CurseForge project with `icon_512.png` as the logo, record the project id above and in the upload script, and add the `CURSEFORGE_TOKEN`, `IMC_MAVEN_USER` and `IMC_MAVEN_PASSWORD` repository secrets.
3. Set `banners/social.png` as the GitHub social preview.
4. `icon/thoroughchests_logo.png` is the mod list logo (a copy is in `common/src/main/resources`).
