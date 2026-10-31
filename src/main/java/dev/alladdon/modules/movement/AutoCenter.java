/*
 * AllAddons - a single Meteor Client addon bundling the most useful community features.
 * Copyright (C) 2026 AllAddons contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. See LICENSE for the full text.
 */

package dev.alladdon.modules.movement;

import dev.alladdon.AllAddons;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;

/**
 * Nudges you into the middle of the block you are standing on.
 *
 * <p>Feature idea from HIGTools (RedCarlos26), GPL-3.0. Being exactly on a block center matters for
 * hole digging, crystal placement and most highway bots.</p>
 */
public class AutoCenter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("How fast you slide towards the center, in blocks per tick.")
        .defaultValue(0.15)
        .min(0.01)
        .sliderRange(0.01, 0.5)
        .build()
    );

    private final Setting<Double> tolerance = sgGeneral.add(new DoubleSetting.Builder()
        .name("tolerance")
        .description("Distance to the center at which you count as centered.")
        .defaultValue(0.05)
        .min(0.001)
        .sliderRange(0.001, 0.2)
        .build()
    );

    private final Setting<Boolean> disableWhenCentered = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-when-centered")
        .description("Turns the module off once you are centered.")
        .defaultValue(true)
        .build()
    );

    public AutoCenter() {
        super(AllAddons.CATEGORY, "auto-center", "Slides you into the middle of the block you are standing on.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        double centerX = Math.floor(mc.player.getX()) + 0.5;
        double centerZ = Math.floor(mc.player.getZ()) + 0.5;

        double deltaX = centerX - mc.player.getX();
        double deltaZ = centerZ - mc.player.getZ();
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (distance <= tolerance.get()) {
            if (disableWhenCentered.get()) {
                info("Centered.");
                toggle();
            }
            return;
        }

        // Never overshoot the center.
        double step = Math.min(speed.get(), distance);
        Vec3 velocity = mc.player.getDeltaMovement();

        mc.player.setDeltaMovement(
            deltaX / distance * step,
            velocity.y,
            deltaZ / distance * step
        );
    }
}
