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
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.AbstractClientPlayer;

/**
 * Draws a stick figure inside every nearby player.
 *
 * <p>Feature idea from meteor-rejects (AntiCope), GPL-3.0. The figure is built from the standing
 * player hitbox, so it is an approximation: the real model's limb pose (sneaking, swinging,
 * riding) is not sampled.</p>
 */
public class SkeletonESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Only players within this many blocks are drawn.")
        .defaultValue(48)
        .min(1)
        .sliderRange(1, 128)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Color of the skeleton.")
        .defaultValue(new SettingColor(255, 90, 90, 220))
        .build()
    );

    // Local space: x is to the right, y is up from the feet, z is forward. Vanilla hitbox is 1.8 tall.
    private static final double[][] JOINTS = {
        {0.0, 1.62, 0.0},   // 0 head
        {0.0, 1.44, 0.0},   // 1 neck
        {0.0, 1.18, 0.0},   // 2 chest
        {0.0, 0.90, 0.0},   // 3 hips
        {-0.34, 1.38, 0.0}, // 4 left shoulder
        {0.34, 1.38, 0.0},  // 5 right shoulder
        {-0.45, 0.92, 0.0}, // 6 left hand
        {0.45, 0.92, 0.0},  // 7 right hand
        {-0.16, 0.45, 0.0}, // 8 left knee
        {0.16, 0.45, 0.0},  // 9 right knee
        {-0.16, 0.02, 0.0}, // 10 left foot
        {0.16, 0.02, 0.0},  // 11 right foot
    };

    private static final int[][] BONES = {
        {0, 1}, {1, 2}, {2, 3},
        {1, 4}, {1, 5}, {4, 6}, {5, 7},
        {3, 8}, {3, 9}, {8, 10}, {9, 11},
    };

    public SkeletonESP() {
        super(AllAddons.CATEGORY, "skeleton-esp", "Draws a skeleton inside nearby players.");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;

        for (AbstractClientPlayer player : mc.level.players()) {
            if (player == mc.player) continue;
            if (player.isSpectator()) continue;
            if (PlayerUtils.distanceTo(player) > range.get()) continue;

            draw(event, player);
        }
    }

    private void draw(Render3DEvent event, AbstractClientPlayer player) {
        // Body yaw: the model faces -Z at yaw 0, hence the negation.
        double yaw = Math.toRadians(-player.getYRot());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);

        double baseX = player.getX();
        double baseY = player.getY();
        double baseZ = player.getZ();

        SettingColor color = this.color.get();

        for (int[] bone : BONES) {
            double[] from = JOINTS[bone[0]];
            double[] to = JOINTS[bone[1]];

            event.renderer.line(
                baseX + rotateX(from, cos, sin), baseY + from[1], baseZ + rotateZ(from, cos, sin),
                baseX + rotateX(to, cos, sin), baseY + to[1], baseZ + rotateZ(to, cos, sin),
                color
            );
        }
    }

    private static double rotateX(double[] joint, double cos, double sin) {
        return joint[0] * cos - joint[2] * sin;
    }

    private static double rotateZ(double[] joint, double cos, double sin) {
        return joint[0] * sin + joint[2] * cos;
    }
}
