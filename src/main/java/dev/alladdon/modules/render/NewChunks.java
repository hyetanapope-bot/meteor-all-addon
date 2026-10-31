/*
 * AllAddons - a single Meteor Client addon bundling the most useful community features.
 * Copyright (C) 2026 AllAddons contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. See LICENSE for the full text.
 */

package dev.alladdon.modules.render;

import dev.alladdon.AllAddons;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Highlights chunks the server generated just now.
 *
 * <p>Feature idea from meteor-rejects (AntiCope) and Numby-hack (cqb13), GPL-3.0. A chunk that
 * arrives at the client for the first time during a session is almost always freshly generated,
 * which is how players find new bases and mining operations on anarchy servers.</p>
 */
public class NewChunks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> lifetime = sgGeneral.add(new IntSetting.Builder()
        .name("lifetime")
        .description("How long a new chunk stays highlighted, in seconds.")
        .defaultValue(60)
        .min(1)
        .sliderRange(1, 300)
        .build()
    );

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Only chunks within this many chunks are rendered.")
        .defaultValue(12)
        .min(1)
        .sliderRange(1, 32)
        .build()
    );

    private final Setting<Integer> ignoreFirst = sgGeneral.add(new IntSetting.Builder()
        .name("ignore-first-ticks")
        .description("Chunks received during the first ticks after joining are ignored, so the already world you spawned into is not highlighted.")
        .defaultValue(100)
        .min(0)
        .sliderRange(0, 400)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Color of the chunk outline.")
        .defaultValue(new SettingColor(255, 200, 0, 220))
        .build()
    );

    private final Set<Long> seen = new HashSet<>();
    private final List<Mark> marks = new ArrayList<>();

    private int ignoreTicks;

    public NewChunks() {
        super(AllAddons.CATEGORY, "new-chunks", "Highlights chunks that were generated after you joined.");
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        reset();
    }

    private void reset() {
        seen.clear();
        marks.clear();
        ignoreTicks = ignoreFirst.get();
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        reset();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        ChunkPos pos = event.chunk().getPos();
        long key = ChunkPos.pack(pos.x(), pos.z());

        // Only the first delivery of a chunk in this session counts.
        if (!seen.add(key)) return;
        if (ignoreTicks > 0) return;

        marks.add(new Mark(pos.x(), pos.z(), System.currentTimeMillis()));
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (ignoreTicks > 0) ignoreTicks--;

        long now = System.currentTimeMillis();
        long lifetimeMs = lifetime.get() * 1000L;

        for (Iterator<Mark> it = marks.iterator(); it.hasNext(); ) {
            if (now - it.next().time() > lifetimeMs) it.remove();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null || marks.isEmpty()) return;

        int chunkX = SectionPos.posToSectionCoord(mc.player.getBlockX());
        int chunkZ = SectionPos.posToSectionCoord(mc.player.getBlockZ());
        int chunkRadius = radius.get();
        double y = Math.floor(mc.player.getY());

        long now = System.currentTimeMillis();
        long lifetimeMs = lifetime.get() * 1000L;

        SettingColor color = lineColor.get();

        for (Mark mark : marks) {
            if (Math.abs(mark.x() - chunkX) > chunkRadius || Math.abs(mark.z() - chunkZ) > chunkRadius) continue;

            double age = Math.min(1.0, (now - mark.time()) / (double) lifetimeMs);
            int alpha = (int) Math.round(color.a * (1.0 - age * 0.75));

            double minX = mark.x() * 16.0;
            double minZ = mark.z() * 16.0;
            double maxX = minX + 16.0;
            double maxZ = minZ + 16.0;

            int r = color.r, g = color.g, b = color.b;

            event.renderer.line(minX, y, minZ, maxX, y, minZ, new Color(r, g, b, alpha));
            event.renderer.line(maxX, y, minZ, maxX, y, maxZ, new Color(r, g, b, alpha));
            event.renderer.line(maxX, y, maxZ, minX, y, maxZ, new Color(r, g, b, alpha));
            event.renderer.line(minX, y, maxZ, minX, y, minZ, new Color(r, g, b, alpha));
        }
    }

    private record Mark(int x, int z, long time) {
    }
}
