/*
 * AllAddons - a single Meteor Client addon bundling the most useful community features.
 * Copyright (C) 2026 AllAddons contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. See LICENSE for the full text.
 */

package dev.alladdon.modules.player;

import dev.alladdon.AllAddons;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Warns you when lava or fire is close by.
 *
 * <p>Feature idea from Numby-hack (cqb13), GPL-3.0. Handy while building over a lava lake or while
 * mining in the Nether.</p>
 */
public class LavaWarning extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Search radius in blocks.")
        .defaultValue(3)
        .min(1)
        .sliderRange(1, 12)
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Seconds between warnings.")
        .defaultValue(5)
        .min(1)
        .sliderRange(1, 60)
        .build()
    );

    private final Setting<Boolean> fire = sgGeneral.add(new BoolSetting.Builder()
        .name("fire")
        .description("Also warn about fire and soul fire.")
        .defaultValue(true)
        .build()
    );

    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    private int timer;

    public LavaWarning() {
        super(AllAddons.CATEGORY, "lava-warning", "Warns you when lava or fire is dangerously close.");
    }

    @Override
    public void onDeactivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
        if (timer > 0) {
            timer--;
            return;
        }

        double nearest = scan();
        if (nearest < 0) return;

        timer = cooldown.get() * 20;
        warning("Lava or fire %.0f blocks away.", nearest);
    }

    /** Distance to the closest lava/fire block, or -1 when there is none. */
    private double scan() {
        BlockPos origin = mc.player.blockPosition();
        int r = radius.get();
        double best = Double.MAX_VALUE;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    mutablePos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);

                    // Cheap early out before touching the block state.
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance >= best) continue;

                    Block block = mc.level.getBlockState(mutablePos).getBlock();
                    if (!isDangerous(block)) continue;

                    best = distance;
                }
            }
        }

        return best == Double.MAX_VALUE ? -1 : best;
    }

    private boolean isDangerous(Block block) {
        if (block == Blocks.LAVA) return true;
        return fire.get() && (block == Blocks.FIRE || block == Blocks.SOUL_FIRE);
    }
}
