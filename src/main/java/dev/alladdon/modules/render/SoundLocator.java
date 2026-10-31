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
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
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
import net.minecraft.client.resources.sounds.SoundInstance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Draws a marker where a nearby sound was played.
 *
 * <p>Feature idea from meteor-rejects (AntiCope), GPL-3.0. Useful for spotting other players'
 * mining, footsteps, or an ender chest being opened behind a wall.</p>
 */
public class SoundLocator extends Module {
    private static final int MAX_MARKERS = 96;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Only sounds within this many blocks are marked.")
        .defaultValue(24)
        .min(1)
        .sliderRange(1, 64)
        .build()
    );

    private final Setting<Double> lifetime = sgGeneral.add(new DoubleSetting.Builder()
        .name("lifetime")
        .description("How long a marker stays visible, in seconds.")
        .defaultValue(3)
        .min(0.1)
        .sliderRange(0.1, 10)
        .build()
    );

    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-own")
        .description("Ignores sounds that are basically in your own position.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreAmbient = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-ambient")
        .description("Ignores music and ambient noises.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> size = sgGeneral.add(new DoubleSetting.Builder()
        .name("size")
        .description("Size of a marker.")
        .defaultValue(0.4)
        .min(0.05)
        .sliderRange(0.05, 2)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How markers are rendered.")
        .defaultValue(ShapeMode.Lines)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Fill color.")
        .defaultValue(new SettingColor(255, 170, 0, 30))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Outline color.")
        .defaultValue(new SettingColor(255, 170, 0, 220))
        .build()
    );

    private final List<Marker> markers = new ArrayList<>();

    public SoundLocator() {
        super(AllAddons.CATEGORY, "sound-locator", "Marks where nearby sounds are coming from.");
    }

    @Override
    public void onDeactivate() {
        markers.clear();
    }

    @EventHandler
    private void onPlaySound(PlaySoundEvent event) {
        if (mc.player == null) return;

        SoundInstance sound = event.sound;
        if (sound == null) return;

        // Music and ambience are identified by their registry path; the client fires a lot of them
        // and they would drown out the sounds that actually matter.
        if (ignoreAmbient.get()) {
            String path = sound.getIdentifier().getPath();
            if (path.contains("ambient") || path.contains("music")) return;
        }

        double x = sound.getX();
        double y = sound.getY();
        double z = sound.getZ();

        double distance = PlayerUtils.distanceTo(x, y, z);
        if (distance > range.get()) return;
        if (ignoreOwn.get() && distance < 2.0) return;

        long now = System.currentTimeMillis();

        // The client fires the same sound more than once (multiplayer re-plays), so dedupe.
        for (Marker marker : markers) {
            if (now - marker.time() > 250L) continue;
            if (marker.isClose(x, y, z)) return;
        }

        markers.add(new Marker(x, y, z, now));
        while (markers.size() > MAX_MARKERS) markers.remove(0);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        long now = System.currentTimeMillis();
        long lifetimeMs = (long) (lifetime.get() * 1000.0);

        for (Iterator<Marker> it = markers.iterator(); it.hasNext(); ) {
            if (now - it.next().time() > lifetimeMs) it.remove();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || markers.isEmpty()) return;

        long now = System.currentTimeMillis();
        double lifetimeMs = lifetime.get() * 1000.0;
        double half = size.get() / 2.0;

        for (Marker marker : markers) {
            double age = (now - marker.time()) / lifetimeMs;
            if (age >= 1.0) continue;

            double fade = 1.0 - age;
            Color side = fade(sideColor.get(), fade);
            Color line = fade(lineColor.get(), fade);

            event.renderer.box(
                marker.x() - half, marker.y() - half, marker.z() - half,
                marker.x() + half, marker.y() + half, marker.z() + half,
                side, line, shapeMode.get(), 0
            );
        }
    }

    private static Color fade(Color color, double factor) {
        return new Color(color.r, color.g, color.b, (int) Math.round(color.a * factor));
    }

    private record Marker(double x, double y, double z, long time) {
        boolean isClose(double otherX, double otherY, double otherZ) {
            return Math.abs(x - otherX) < 1.0 && Math.abs(y - otherY) < 1.0 && Math.abs(z - otherZ) < 1.0;
        }
    }
}
