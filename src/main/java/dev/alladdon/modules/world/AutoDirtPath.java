/*
 * AllAddons - a single Meteor Client addon bundling the most useful community features.
 * Copyright (C) 2026 AllAddons contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. See LICENSE for the full text.
 */

package dev.alladdon.modules.world;

import dev.alladdon.AllAddons;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Turns grass and dirt around you into dirt paths with a shovel.
 *
 * <p>Feature idea from Nora-Tweaks (noramibu), GPL-3.0. Shovels can path the block they are used on
 * in vanilla, so this module only has to find the block, swap the shovel in and click it.</p>
 */
public class AutoDirtPath extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("How many blocks around you are pathed.")
        .defaultValue(3)
        .min(1)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between two blocks.")
        .defaultValue(4)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    private int timer;

    public AutoDirtPath() {
        super(AllAddons.CATEGORY, "auto-dirt-path", "Creates dirt paths around you with a shovel.");
    }

    @Override
    public void onDeactivate() {
        InvUtils.swapBack();
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Utils.canUpdate()) return;
        if (timer++ < delay.get()) return;
        timer = 0;

        FindItemResult shovel = InvUtils.findInHotbar(stack -> stack.is(ItemTags.SHOVELS));
        if (!shovel.found()) {
            error("No shovel in your hotbar.");
            toggle();
            return;
        }

        BlockPos target = findTarget();
        if (target == null) return;

        InvUtils.swap(shovel.slot(), true);

        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        BlockUtils.interact(hit, InteractionHand.MAIN_HAND, true);

        InvUtils.swapBack();
    }

    private BlockPos findTarget() {
        BlockPos origin = mc.player.blockPosition();
        int r = radius.get();

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
            origin.offset(-r, -2, -r),
            origin.offset(r, 1, r)
        )) {
            mutablePos.set(pos);

            if (!isPathCandidate(mc.level.getBlockState(mutablePos).getBlock())) continue;
            if (!mc.level.getBlockState(mutablePos.above()).isAir()) continue;

            double distance = PlayerUtils.distanceTo(mutablePos);
            if (distance >= bestDistance) continue;

            bestDistance = distance;
            best = mutablePos.immutable();
        }

        return best;
    }

    private static boolean isPathCandidate(Block block) {
        return block == Blocks.GRASS_BLOCK
            || block == Blocks.DIRT
            || block == Blocks.COARSE_DIRT
            || block == Blocks.PODZOL
            || block == Blocks.MYCELIUM
            || block == Blocks.ROOTED_DIRT;
    }
}
