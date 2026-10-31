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
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

/**
 * Sends the login command for you after joining an auth-protected server.
 *
 * <p>Feature idea from Meteorist (Zgoly) and meteor-rejects (AntiCope), both GPL-3.0. The password
 * is stored in plain text inside your Meteor config folder - treat that file as a secret.</p>
 */
public class AutoLogin extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> password = sgGeneral.add(new StringSetting.Builder()
        .name("password")
        .description("The password sent to the server. Stored in plain text in your Meteor config.")
        .defaultValue("")
        .wide()
        .build()
    );

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
        .name("command")
        .description("Command used to log in. Use register-command for the registration variant.")
        .defaultValue("login")
        .build()
    );

    private final Setting<Boolean> register = sgGeneral.add(new BoolSetting.Builder()
        .name("register")
        .description("Also sends the registration command, for servers that expect /register on first join.")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> registerCommand = sgGeneral.add(new StringSetting.Builder()
        .name("register-command")
        .description("Registration command, the password is passed twice.")
        .defaultValue("register")
        .visible(register::get)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks to wait after joining before sending the command.")
        .defaultValue(60)
        .min(0)
        .sliderRange(0, 200)
        .build()
    );

    private int timer;
    private boolean sent;

    public AutoLogin() {
        super(AllAddons.CATEGORY, "auto-login", "Automatically sends the login command after joining a server.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        sent = false;

        if (password.get().isBlank()) {
            warning("No password is set, so nothing will be sent. Fill in the password setting first.");
            sent = true;
        }
    }

    @Override
    public void onDeactivate() {
        timer = 0;
        sent = false;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        timer = 0;
        sent = password.get().isBlank();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (sent || !Utils.canUpdate()) return;
        if (timer++ < delay.get()) return;

        sent = true;
        sendCommands();
    }

    private void sendCommands() {
        String secret = password.get().trim();

        if (register.get()) {
            String registerName = stripLeadingSlash(registerCommand.get().isBlank() ? "register" : registerCommand.get());
            ChatUtils.sendPlayerMsg("/" + registerName + " " + secret + " " + secret);
        }

        String loginName = stripLeadingSlash(command.get().isBlank() ? "login" : command.get());
        ChatUtils.sendPlayerMsg("/" + loginName + " " + secret);

        info("Sent /%s.", loginName);
    }

    private static String stripLeadingSlash(String command) {
        String trimmed = command.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }
}
