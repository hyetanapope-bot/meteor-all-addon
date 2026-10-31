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
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/**
 * Launches you upwards with a wind charge.
 *
 * <p>Feature idea from Nora-Tweaks (noramibu), GPL-3.0. Aiming straight down and using a wind charge
 * propels you upwards; this module does the aiming, the hotbar swap and the click for you.</p>
 */
public class WindChargeJump extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> rotationPriority = sgGeneral.add(new IntSetting.Builder()
        .name("rotation-priority")
        .description("Rotation priority used while aiming down.")
        .defaultValue(100)
        .min(0)
        .sliderRange(0, 1000)
        .build()
    );

    private int step;
    private int slot = -1;
    private int timer;

    public WindChargeJump() {
        super(AllAddons.CATEGORY, "wind-charge-jump", "Uses a wind charge to launch you straight up.");
    }

    @Override
    public void onActivate() {
        step = 0;
        timer = 0;

        if (mc.player == null) {
            toggle();
            return;
        }

        var result = InvUtils.findInHotbar(Items.WIND_CHARGE);
        if (!result.found()) {
            error("No wind charge in your hotbar.");
            toggle();
            return;
        }

        slot = result.slot();
    }

    @Override
    public void onDeactivate() {
        InvUtils.swapBack();
        step = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || slot < 0) {
            toggle();
            return;
        }

        switch (step) {
            case 0 -> {
                // Look straight down, that is where the charge has to be thrown.
                Rotations.rotate(mc.player.getYRot(), 90.0, rotationPriority.get());
                step = 1;
            }
            case 1 -> {
                InvUtils.swap(slot, true);
                step = 2;
            }
            case 2 -> {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                InvUtils.swapBack();
                step = 3;
                timer = 0;
            }
            default -> {
                // Give the rotation a moment to be sent before the module goes inactive.
                if (++timer > 5) toggle();
            }
        }
    }
}
