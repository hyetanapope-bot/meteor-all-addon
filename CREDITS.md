# Credits

AllAddons is a single addon that reimplements the most useful features of popular Meteor Client
addons for Meteor 26.3. **No upstream source code was copied**: the modules here were written from
scratch against Meteor's public addon API, using the projects below as a description of *what* the
feature should do.

If you maintain one of these projects and want a different attribution, or believe a fragment is
too close to your implementation, open an issue and it will be fixed.

## Feature inspiration

| Project | Author | Licence | Features used here |
| --- | --- | --- | --- |
| [Meteorist](https://github.com/Zgoly/Meteorist) | Zgoly | GPL-3.0 | `auto-login`, chunk grid idea |
| [meteor-rejects](https://github.com/AntiCope/meteor-rejects) | AntiCope | GPL-3.0 | `new-chunks`, `sound-locator`, `skeleton-esp`, `anti-vanish`, `coord-logger`, `auto-login` |
| [Numby-hack](https://github.com/cqb13/Numby-hack) | cqb13 | GPL-3.0 | `chunk-borders`, `spawner-esp`, `tnt-fuse-esp`, `new-chunks`, `lava-warning`, `chat-encryption`, `packet-delay`, `item-counter` |
| [HIGTools](https://github.com/RedCarlos26/HIGTools) | RedCarlos26 | GPL-3.0 | `auto-center` |
| [stardust](https://github.com/0xTas/stardust) | 0xTas | GPL-3.0 | `ad-blocker`, `auto-doors` |
| [Nora-Tweaks](https://github.com/noramibu/Nora-Tweaks) | noramibu | CC0-1.0 | `wind-charge-jump`, `auto-dirt-path` |

## Platform

| Project | Licence | Why it matters |
| --- | --- | --- |
| [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) | GPL-3.0 | The client this addon targets; its source is the reference for every API call used here |
| [Fabric Loader](https://github.com/FabricMC/fabric-loader) / [Fabric API](https://github.com/FabricMC/fabric-api) | Apache-2.0 | Mod loading |
| [Gradle Loom](https://github.com/FabricMC/fabric-loom) | MIT | Build tooling |

## Tooling in this repository

* `tools/verify.py` and `tools/make_icon.py` are original scripts, GPL-3.0-or-later like the rest of
  the repository.
* The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`) is the standard wrapper shipped
  with Gradle, Apache-2.0.
