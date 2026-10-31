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
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.AbstractClientPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Records where you have seen other players.
 *
 * <p>Feature idea from meteor-rejects (AntiCope), GPL-3.0. Every player that comes within range is
 * written to chat and/or to {@code <game dir>/meteor-client/coord-logger.txt} once per cooldown,
 * which is handy for tracking who is around your base.</p>
 */
public class CoordLogger extends Module {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Players further away than this are ignored.")
        .defaultValue(64)
        .min(1)
        .sliderRange(1, 256)
        .build()
    );

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Seconds before the same player is logged again.")
        .defaultValue(30)
        .min(1)
        .sliderRange(1, 600)
        .build()
    );

    private final Setting<Boolean> chat = sgGeneral.add(new BoolSetting.Builder()
        .name("chat")
        .description("Prints the entry to your chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> file = sgGeneral.add(new BoolSetting.Builder()
        .name("file")
        .description("Appends the entry to meteor-client/coord-logger.txt.")
        .defaultValue(true)
        .build()
    );

    private final Map<UUID, Long> lastLogged = new HashMap<>();

    public CoordLogger() {
        super(AllAddons.CATEGORY, "coord-logger", "Logs the coordinates where you see other players.");
    }

    @Override
    public void onDeactivate() {
        lastLogged.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        long cooldownMs = cooldown.get() * 1000L;

        for (AbstractClientPlayer player : mc.level.players()) {
            if (player == mc.player) continue;

            double distance = PlayerUtils.distanceTo(player);
            if (distance > range.get()) continue;

            Long last = lastLogged.get(player.getUUID());
            if (last != null && now - last < cooldownMs) continue;

            lastLogged.put(player.getUUID(), now);
            log(player.getName().getString(), player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ(), distance);
        }
    }

    private void log(String name, int x, int y, int z, double distance) {
        String entry = "%s - %s at %d, %d, %d (%.0f blocks away)".formatted(
            LocalDateTime.now().format(TIME_FORMAT), name, x, y, z, distance
        );

        if (chat.get()) info("(highlight)%s(default) at (highlight)%d, %d, %d(default) - %.0f blocks away.", name, x, y, z, distance);
        if (file.get()) appendToFile(entry);
    }

    private void appendToFile(String entry) {
        Path path = MeteorClient.FOLDER.toPath().resolve("coord-logger.txt");

        try {
            Files.writeString(
                path, entry + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            error("Could not write coord-logger.txt: %s", e.getMessage());
        }
    }
}
