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
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import java.util.List;
import java.util.Locale;

/**
 * Hides chat messages that look like advertisements.
 *
 * <p>Feature idea from stardust (0xTas), GPL-3.0. The filter is a plain case-insensitive substring
 * match, so nothing is sent anywhere - it only cancels the message before it reaches your chat.</p>
 */
public class AdBlocker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> keywords = sgGeneral.add(new StringListSetting.Builder()
        .name("keywords")
        .description("A message containing any of these strings is hidden.")
        .defaultValue(
            "discord.gg", "discord.com/invite", "twitch.tv/", "youtube.com/", "t.me/",
            "buy now", "cheap", "on sale", "/shop", "store:", "free rank", "advertising"
        )
        .build()
    );

    private final Setting<Boolean> notify = sgGeneral.add(new BoolSetting.Builder()
        .name("notify")
        .description("Tells you in chat when a message was hidden. Can be noisy on a busy server.")
        .defaultValue(false)
        .build()
    );

    private int hidden;

    public AdBlocker() {
        super(AllAddons.CATEGORY, "ad-blocker", "Hides chat messages that look like advertisements.");
    }

    @Override
    public String getInfoString() {
        return hidden > 0 ? Integer.toString(hidden) : null;
    }

    @Override
    public void onActivate() {
        hidden = 0;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        String text = event.getMessage().getString().toLowerCase(Locale.ROOT);
        if (text.isBlank()) return;

        for (String keyword : keywords.get()) {
            if (keyword == null || keyword.isBlank()) continue;
            if (!text.contains(keyword.toLowerCase(Locale.ROOT))) continue;

            event.cancel();
            hidden++;

            if (notify.get()) info("Hidden a message matching \"(highlight)%s(default)\".", keyword);
            return;
        }
    }
}
