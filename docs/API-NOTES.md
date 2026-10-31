# Meteor 26.3 API notes

Notes collected while porting features to Meteor 26.3 (`minecraft = "26.3"`, JDK 25, Fabric Loader
0.19.5). They document the exact calls this addon relies on, so a future port has a checklist.

## Addon entrypoint

```java
public class AllAddons extends MeteorAddon {
    public static final Category CATEGORY = new Category("AllAddons");

    @Override public void onInitialize() { Hud.get().register(MyHud.INFO); }
    @Override public void onRegisterCategories() { Modules.registerCategory(CATEGORY); }
    @Override public String getPackage() { return "dev.alladdon"; }
    @Override public GithubRepo getRepo() { return new GithubRepo("owner", "repo", "main", null); }
}
```

Meteor scans `getPackage()` for `Module` subclasses, so modules only need a no-arg constructor that
calls `super(CATEGORY, "name", "description")`. Categories may only be registered inside
`onRegisterCategories()`. `fabric.mod.json` needs a `meteor` entrypoint and at least one author.

## Events

| Event | Notes |
| --- | --- |
| `TickEvent.Pre` / `TickEvent.Post` | One per client tick |
| `Render3DEvent` | `matrices` (`PoseStack`), `renderer` and `depthRenderer` (`Renderer3D`), `tickDelta` |
| `Render2DEvent` | Uses `GuiGraphicsExtractor` in 26.3, not the old `DrawContext` |
| `PlaySoundEvent` | Carries a `SoundInstance` |
| `PlaySoundPacketEvent` | Carries the raw `ClientboundSoundPacket` |
| `ReceiveMessageEvent` / `SendMessageEvent` | Both cancellable; `SendMessageEvent.message` is a mutable `String` |
| `PacketEvent.Send/Receive` | Cancellable; `event.sendSilently(packet)` avoids recursion |
| `ChunkDataEvent` | Record with `chunk()` |
| `GameJoinedEvent`, `GameLeftEvent` | Session boundaries |

## Rendering

`Renderer3D` replaced the old renderer helpers:

```java
event.renderer.line(x1, y1, z1, x2, y2, z2, color);
event.renderer.box(blockPos, sideColor, lineColor, ShapeMode.Both, 0);
event.renderer.box(aabb, sideColor, lineColor, ShapeMode.Both, 0);
```

`RenderUtils` still exists, but the box drawing lives on `Renderer3D` now.

## HUD

```java
public static final HudElementInfo<X> INFO = new HudElementInfo<>(Hud.GROUP, "name", "desc", X::new);

@Override public void render(HudRenderer renderer) {
    double x = renderer.text("text", this.x, this.y, color, shadow, Hud.get().getTextScale());
    setSize(x - this.x, renderer.textHeight(shadow));
}
```

## Utilities used

* `BlockUtils.interact(BlockHitResult, InteractionHand, boolean swing)` — world interaction.
* `BlockUtils.place(...)`, `InvUtils.swap/findInHotbar/find`, `FindItemResult#found()/slot()`.
* `Rotations.rotate(yaw, pitch, priority)` — server-side rotation.
* `PlayerUtils.distanceTo(...)`, `squaredDistanceTo(...)`, `centerPlayer()`.
* `BlockIterator.register(hRadius, vRadius, (pos, state) -> ...)` — per-tick block scan.
* `ChatUtils.sendPlayerMsg(String)` — send a chat message or `/command`.
* `MeteorClient.FOLDER` — `<game dir>/meteor-client`, for addon files.
* Settings: `settings.getDefaultGroup()` / `settings.createGroup("Name")`, then
  `IntSetting/DoubleSetting/BoolSetting/StringSetting/EnumSetting/ColorSetting/StringListSetting`.

## Vanilla API details that changed or are easy to get wrong

* `Inventory` still exposes `getContainerSize()`, `getItem(i)` and `getSelectedSlot()`; equipment is
  also reachable through `getNonEquipmentItems()`.
* `SoundInstance` uses `getIdentifier()` (not `getLocation()`), plus `getX()/getY()/getZ()`.
* `ItemStack#is(TagKey<Item>)` with `ItemTags.SHOVELS` is safer than `instanceof ShovelItem`.
* `PrimedTnt#getFuse()` — vanilla fuse is 80 ticks.
* `mc.level.entitiesForRendering()` for entity iteration, `mc.level.players()` for players.
* `mc.getConnection().getOnlinePlayers()` (tab list) and `getPlayerInfo(UUID)`.
* `ChunkPos.pack(x, z)` for a long key; `chunk.getPos().x()/.z()` are record accessors.
* `BlockPos.betweenClosed(a, b)` iterates without allocating.
* `state.getValue(BlockStateProperties.OPEN/POWERED)` for door state.
* `BlockHitResult(Vec3 location, Direction, BlockPos, boolean inside)` — same shape as before.

## What Meteor already has

These are **not** reimplemented here on purpose: `auto-armor`, `chest-swap`, `fast-use`, `auto-afk`,
`stash-finder`, `highway-builder`, `packet-logger`, `auto-nametag`, `hole-filler`, `auto-sign`,
`packet-mine`, `storage-esp`, `block-esp`, `trajectories`, `xray`, `better-chat` (it already has
prefix/suffix), `zoom`, `player-radar` HUD. Duplicating them would only add confusion.
