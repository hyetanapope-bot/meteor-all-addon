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
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Opens doors you walk into and closes them behind you.
 *
 * <p>Feature idea from stardust (0xTas), GPL-3.0. Only doors the client already knows about are
 * touched, and iron doors are skipped because they need a redstone signal.</p>
 */
public class AutoDoors extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> openRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("open-range")
        .description("Doors closer than this are opened.")
        .defaultValue(2.5)
        .min(0.5)
        .sliderRange(0.5, 6)
        .build()
    );

    private final Setting<Double> closeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("close-range")
        .description("Open doors further away than this are closed again.")
        .defaultValue(3.5)
        .min(1)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between interactions.")
        .defaultValue(4)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Boolean> close = sgGeneral.add(new BoolSetting.Builder()
        .name("close")
        .description("Also closes doors behind you.")
        .defaultValue(true)
        .build()
    );

    private final Map<BlockPos, Long> cooldowns = new HashMap<>();

    private int timer;

    public AutoDoors() {
        super(AllAddons.CATEGORY, "auto-doors", "Opens doors in front of you and closes them behind you.");
    }

    @Override
    public void onDeactivate() {
        cooldowns.clear();
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
        if (timer++ < delay.get()) return;
        timer = 0;

        List<BlockPos> doors = findNearbyDoors();
        long now = System.currentTimeMillis();

        for (BlockPos pos : doors) {
            BlockState state = mc.level.getBlockState(pos);
            if (!(state.getBlock() instanceof DoorBlock)) continue;
            if (state.getValue(BlockStateProperties.POWERED)) continue;

            boolean open = state.getValue(BlockStateProperties.OPEN);
            double distance = PlayerUtils.distanceTo(pos);

            boolean shouldOpen = !open && distance <= openRange.get();
            boolean shouldClose = open && close.get() && distance >= closeRange.get();

            if (!shouldOpen && !shouldClose) continue;

            Long last = cooldowns.get(pos);
            if (last != null && now - last < 500L) continue;

            cooldowns.put(pos, now);

            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            BlockUtils.interact(hit, InteractionHand.MAIN_HAND, true);
            return;
        }
    }

    private List<BlockPos> findNearbyDoors() {
        List<BlockPos> doors = new ArrayList<>();

        BlockPos origin = mc.player.blockPosition();
        int radius = (int) Math.ceil(Math.max(openRange.get(), closeRange.get()));

        for (BlockPos pos : BlockPos.betweenClosed(
            origin.offset(-radius, -2, -radius),
            origin.offset(radius, 2, radius)
        )) {
            if (mc.level.getBlockState(pos).getBlock() instanceof DoorBlock) doors.add(pos.immutable());
        }

        return doors;
    }
}
