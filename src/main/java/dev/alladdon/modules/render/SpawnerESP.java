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
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlights mob spawners around the player.
 *
 * <p>Feature idea from Numby-hack (cqb13), GPL-3.0. Meteor's own {@code storage-esp} does not
 * cover spawners and {@code block-esp} has to be configured per block, so this is a one-click
 * alternative.</p>
 */
public class SpawnerESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("horizontal-radius")
        .description("Horizontal search radius in blocks.")
        .defaultValue(16)
        .min(1)
        .sliderRange(1, 64)
        .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("vertical-radius")
        .description("Vertical search radius in blocks.")
        .defaultValue(8)
        .min(1)
        .sliderRange(1, 32)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the spawners are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Fill color.")
        .defaultValue(new SettingColor(170, 120, 255, 40))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Outline color.")
        .defaultValue(new SettingColor(170, 120, 255, 255))
        .build()
    );

    private final Setting<Boolean> tracers = sgGeneral.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Draws a line from you to every spawner.")
        .defaultValue(false)
        .build()
    );

    private final List<BlockPos> spawners = new ArrayList<>();

    public SpawnerESP() {
        super(AllAddons.CATEGORY, "spawner-esp", "Highlights mob spawners around you.");
    }

    @Override
    public void onDeactivate() {
        spawners.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        spawners.clear();

        BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
            if (blockState.getBlock() == Blocks.SPAWNER) spawners.add(blockPos.immutable());
        });
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;

        for (BlockPos pos : spawners) {
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);

            if (tracers.get()) {
                event.renderer.line(
                    mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(), mc.player.getZ(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    lineColor.get()
                );
            }
        }
    }
}
