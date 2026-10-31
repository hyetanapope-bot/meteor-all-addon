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
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.SectionPos;

/**
 * Outlines the chunk grid around the player.
 *
 * <p>Feature idea from Numby-hack (cqb13) and Meteorist (Zgoly), both GPL-3.0. Implemented from
 * scratch against the Meteor 26.3 addon API.</p>
 */
public class ChunkBorders extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("How many chunks in every direction are outlined.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 8)
        .build()
    );

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
        .name("height")
        .description("How far above and below you the vertical lines are drawn.")
        .defaultValue(16)
        .min(1)
        .sliderRange(1, 128)
        .build()
    );

    private final Setting<Boolean> grid = sgGeneral.add(new BoolSetting.Builder()
        .name("floor-grid")
        .description("Also draws a flat grid at your feet height.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Color of the chunk borders.")
        .defaultValue(new SettingColor(170, 120, 255, 200))
        .build()
    );

    public ChunkBorders() {
        super(AllAddons.CATEGORY, "chunk-borders", "Draws the chunk grid around you.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;

        int chunkX = SectionPos.posToSectionCoord(mc.player.getBlockX());
        int chunkZ = SectionPos.posToSectionCoord(mc.player.getBlockZ());
        int chunkRadius = radius.get();

        double bottom = Math.max(mc.level.getMinY(), mc.player.getY() - height.get());
        double top = Math.min(mc.level.getHeight(), mc.player.getY() + height.get());

        int minX = (chunkX - chunkRadius) * 16;
        int maxX = (chunkX + chunkRadius + 1) * 16;
        int minZ = (chunkZ - chunkRadius) * 16;
        int maxZ = (chunkZ + chunkRadius + 1) * 16;

        SettingColor color = lineColor.get();

        // Vertical lines running along Z, then along X.
        for (int x = minX; x <= maxX; x += 16) {
            event.renderer.line(x, bottom, minZ, x, top, minZ, color);
            event.renderer.line(x, bottom, maxZ, x, top, maxZ, color);
        }

        for (int z = minZ; z <= maxZ; z += 16) {
            event.renderer.line(minX, bottom, z, minX, top, z, color);
            event.renderer.line(maxX, bottom, z, maxX, top, z, color);
        }

        if (grid.get()) {
            double y = Math.floor(mc.player.getY());

            for (int x = minX; x <= maxX; x += 16) event.renderer.line(x, y, minZ, x, y, maxZ, color);
            for (int z = minZ; z <= maxZ; z += 16) event.renderer.line(minX, y, z, maxX, y, z, color);
        }
    }
}
