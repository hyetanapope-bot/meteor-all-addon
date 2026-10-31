/*
 * AllAddons - a single Meteor Client addon bundling the most useful community features.
 * Copyright (C) 2026 AllAddons contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. See LICENSE for the full text.
 */

package dev.alladdon.modules.misc;

import dev.alladdon.AllAddons;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tells you when a player near you disappears while still being in the tab list.
 *
 * <p>Feature idea from meteor-rejects (AntiCope), GPL-3.0. Staff on many servers use /vanish: the
 * player entity is removed but the tab list entry stays. Players that are merely out of tracking
 * range never had a recent position near you, so they are not reported - only players that
 * vanished while close to you are.</p>
 */
public class AntiVanish extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
        .name("check-interval")
        .description("Ticks between checks.")
        .defaultValue(20)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("A player is reported only if they were last seen within this many blocks.")
        .defaultValue(64)
        .min(1)
        .sliderRange(1, 256)
        .build()
    );

    private final Map<UUID, Vec3> lastSeen = new HashMap<>();
    private final Set<UUID> vanished = new HashSet<>();

    private int timer;

    public AntiVanish() {
        super(AllAddons.CATEGORY, "anti-vanish", "Reports players that vanish near you while staying in the tab list.");
    }

    @Override
    public void onDeactivate() {
        lastSeen.clear();
        vanished.clear();
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return;
        if (timer++ < interval.get()) return;
        timer = 0;

        Vec3 ownPosition = mc.player.position();

        // Everyone the client currently renders.
        Set<UUID> visible = new HashSet<>();
        for (AbstractClientPlayer player : mc.level.players()) {
            visible.add(player.getUUID());
            if (player != mc.player) lastSeen.put(player.getUUID(), player.position());
        }

        Set<UUID> listed = new HashSet<>();
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            UUID id = info.getProfile().id();
            if (id.equals(mc.player.getUUID())) continue;

            listed.add(id);
            if (visible.contains(id)) continue;

            Vec3 last = lastSeen.get(id);
            if (last == null || last.distanceTo(ownPosition) > range.get()) continue;

            if (vanished.add(id)) {
                info("(highlight)%s(default) vanished near %.0f, %.0f, %.0f.",
                    nameOf(id), last.x, last.y, last.z);
            }
        }

        // Clean up the ones that came back or left the server.
        for (Iterator<UUID> it = vanished.iterator(); it.hasNext(); ) {
            UUID id = it.next();

            if (!listed.contains(id)) {
                it.remove();
                lastSeen.remove(id);
                continue;
            }

            if (visible.contains(id)) {
                it.remove();
                info("(highlight)%s(default) is visible again.", nameOf(id));
            }
        }
    }

    private String nameOf(UUID id) {
        PlayerInfo info = mc.getConnection().getPlayerInfo(id);
        return info != null ? info.getProfile().name() : id.toString();
    }
}
