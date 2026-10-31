# AllAddons ☄️

A single addon for **Meteor Client 26.3** (Minecraft 26.3, JDK 25) that bundles the most useful
features of popular community addons — one jar instead of ten in your `mods` folder.

* **17 modules** in a dedicated `AllAddons` ClickGUI category.
* **1 HUD element** — an inventory item counter.
* No mixins, no extra dependencies beyond Meteor itself.
* Verified by `tools/verify.py`, which checks every Meteor import and the version metadata.

> Русская версия: [README.md](README.md)

## What is inside

Every feature comes from an open source addon under GPL-3.0 (or a compatible licence), but the code
was written from scratch against the current Meteor 26.3 API. The "Inspired by" column lists where
the idea came from.

### Render

| Module | What it does | Inspired by |
| --- | --- | --- |
| `chunk-borders` | Draws the chunk grid around you | Numby-hack, Meteorist |
| `spawner-esp` | Highlights mob spawners (optional tracers) | Numby-hack |
| `tnt-fuse-esp` | Highlights primed TNT and shifts colour as the fuse burns | Numby-hack |
| `new-chunks` | Highlights chunks generated after you joined | meteor-rejects, Numby-hack |
| `sound-locator` | Marks where a nearby sound was played | meteor-rejects |
| `skeleton-esp` | Draws a skeleton inside nearby players | meteor-rejects |

### Player / Movement / World

| Module | What it does | Inspired by |
| --- | --- | --- |
| `lava-warning` | Warns when lava or fire is close | Numby-hack |
| `auto-center` | Slides you into the block centre (holes, crystals, bots) | HIGTools |
| `wind-charge-jump` | Launches you up with a wind charge | Nora-Tweaks |
| `auto-doors` | Opens doors ahead of you and closes them behind | stardust |
| `auto-dirt-path` | Creates dirt paths with a shovel | Nora-Tweaks |

### Misc

| Module | What it does | Inspired by |
| --- | --- | --- |
| `ad-blocker` | Hides chat adverts by keyword | stardust |
| `chat-encryption` | Encrypts your chat with a shared key, decodes marked messages | Numby-hack |
| `anti-vanish` | Reports players that vanish near you but stay in the tab list | meteor-rejects |
| `coord-logger` | Logs coordinates of players you see, to chat and to a file | meteor-rejects |
| `packet-delay` | Delays outgoing packets; keep-alive is never delayed | Numby-hack |
| `auto-login` | Sends `/login` after joining an auth server | Meteorist, meteor-rejects |

### HUD

| Element | What it does | Inspired by |
| --- | --- | --- |
| `item-counter` | Shows how many of each item you carry | Numby-hack |

Per-module settings are documented in [docs/FEATURES.md](docs/FEATURES.md).

## Installation

1. Install **Meteor Client 26.3** (Fabric 26.3, Fabric Loader ≥ 0.19.5, JDK 25).
2. Grab `all-addons-1.0.0.jar` from [Releases](../../releases), or build it yourself.
3. Drop the jar into `.minecraft/mods`.
4. Modules appear in the ClickGUI under **AllAddons**; the HUD element appears in the HUD editor.

## Building

```bash
./gradlew build
# output: build/libs/all-addons-1.0.0.jar
```

### Verification without a build

```bash
python3 tools/verify.py --meteor ../meteor-client
```

No JDK required: it checks Java file structure, that every `meteordevelopment.meteorclient.*` import
(and the members used through it) exists in a meteor-client checkout, that `fabric.mod.json`,
`libs.versions.toml` and `AllAddons.VERSION` agree, and that module names are unique. CI runs it on
every push.

## Caveats

* **`auto-login`** stores the password in plain text in your Meteor config. Do not use it with a
  password you care about.
* **`chat-encryption`** is XOR + Base64, not cryptography. It keeps casual readers out of chat logs,
  nothing more.
* **`skeleton-esp`** approximates the figure from the hitbox; the real model pose is not sampled.
* **`packet-delay`** always lets keep-alive, pong and custom payload packets through, otherwise the
  server times you out.
* Modules that move you or interact with the world may upset a server's anti-cheat. That is on you.

## Licence and credits

Licensed under **GPL-3.0-or-later**, same as Meteor Client. The projects the feature ideas come from,
and their licences, are listed in [CREDITS.md](CREDITS.md).

The code here was written from scratch — no upstream sources were copied. If you believe a fragment
is derived from your project, open an issue and the attribution will be fixed.
