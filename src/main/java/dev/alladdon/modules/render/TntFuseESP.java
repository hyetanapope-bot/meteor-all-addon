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
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;

/**
 * Highlights primed TNT and fades the color as the fuse burns down.
 *
 * <p>Feature idea from Numby-hack (cqb13), GPL-3.0. Vanilla TNT has an 80 tick fuse, so the box
 * becomes fully red one tick before the explosion.</p>
 */
public class TntFuseESP extends Module {
    private static final int DEFAULT_FUSE = 80;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Only TNT within this many blocks is rendered.")
        .defaultValue(32)
        .min(1)
        .sliderRange(1, 128)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the TNT is rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> safeColor = sgGeneral.add(new ColorSetting.Builder()
        .name("safe-color")
        .description("Color right after the TNT was primed.")
        .defaultValue(new SettingColor(170, 120, 255, 255))
        .build()
    );

    private final Setting<SettingColor> dangerColor = sgGeneral.add(new ColorSetting.Builder()
        .name("danger-color")
        .description("Color right before the explosion.")
        .defaultValue(new SettingColor(255, 40, 40, 255))
        .build()
    );

    public TntFuseESP() {
        super(AllAddons.CATEGORY, "tnt-fuse-esp", "Highlights primed TNT and shows how close it is to exploding.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;

        double rangeSquared = range.get() * range.get();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof PrimedTnt tnt)) continue;
            if (PlayerUtils.squaredDistanceTo(tnt) > rangeSquared) continue;

            // 1.0 right after priming, 0.0 at the moment of the explosion.
            double remaining = Math.max(0.0, Math.min(1.0, tnt.getFuse() / (double) DEFAULT_FUSE));
            double danger = 1.0 - remaining;

            Color line = mix(safeColor.get(), dangerColor.get(), danger);
            Color side = mix(safeColor.get(), dangerColor.get(), danger);

            // Fill is only useful when the side is rendered, and always semi transparent.
            side.a = sideColorAlpha(side);

            event.renderer.box(tnt.getBoundingBox().inflate(0.02), side, line, shapeMode.get(), 0);
        }
    }

    /** Lerps between two colors without touching the shared setting instances. */
    private static Color mix(Color from, Color to, double t) {
        return new Color(
            (int) Math.round(from.r + (to.r - from.r) * t),
            (int) Math.round(from.g + (to.g - from.g) * t),
            (int) Math.round(from.b + (to.b - from.b) * t),
            (int) Math.round(from.a + (to.a - from.a) * t)
        );
    }

    private static int sideColorAlpha(Color color) {
        return Math.max(0, Math.min(255, color.a / 4));
    }
}
