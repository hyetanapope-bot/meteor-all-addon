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
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.Packet;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holds back outgoing packets for a configurable amount of time.
 *
 * <p>Feature idea from Numby-hack (cqb13), GPL-3.0. Useful for testing how a server behaves when
 * your connection stutters, or for delaying a burst of clicks. Packets that keep the connection
 * alive are always sent immediately, otherwise the server would time you out.</p>
 */
public class PacketDelay extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How long outgoing packets are held back, in milliseconds.")
        .defaultValue(100)
        .min(1)
        .sliderRange(1, 1000)
        .build()
    );

    private final Setting<Integer> maxQueued = sgGeneral.add(new IntSetting.Builder()
        .name("max-queued")
        .description("Maximum number of held packets, the oldest one is released when the queue is full.")
        .defaultValue(64)
        .min(1)
        .sliderRange(1, 512)
        .build()
    );

    private final Deque<Delayed> queue = new ArrayDeque<>();
    private boolean releasing;

    public PacketDelay() {
        super(AllAddons.CATEGORY, "packet-delay", "Delays your outgoing packets.");
    }

    @Override
    public String getInfoString() {
        return Integer.toString(queue.size());
    }

    @Override
    public void onActivate() {
        queue.clear();
    }

    @Override
    public void onDeactivate() {
        while (!queue.isEmpty()) releaseNext();
    }

    @EventHandler
    private void onSend(PacketEvent.Send event) {
        if (releasing) return;
        if (isConnectionCritical(event.packet)) return;

        event.cancel();
        queue.add(new Delayed(event.packet, System.currentTimeMillis() + delay.get()));

        // Do not let the queue grow forever - a stalled queue looks like a frozen client.
        while (queue.size() > maxQueued.get()) releaseNext();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        long now = System.currentTimeMillis();

        while (!queue.isEmpty() && queue.peek().due() <= now) releaseNext();
    }

    private void releaseNext() {
        Delayed delayed = queue.poll();
        if (delayed == null || mc.getConnection() == null) return;

        releasing = true;
        try {
            mc.getConnection().send(delayed.packet());
        } finally {
            releasing = false;
        }
    }

    /**
     * Packets the server uses to detect a dead client. Class names are checked instead of types so
     * this module keeps working across Minecraft versions that move these packets around.
     */
    private static boolean isConnectionCritical(Packet<?> packet) {
        String name = packet.getClass().getSimpleName();

        return name.contains("KeepAlive")
            || name.contains("Pong")
            || name.contains("ClientInformation")
            || name.contains("CustomPayload");
    }

    private record Delayed(Packet<?> packet, long due) {
    }
}
