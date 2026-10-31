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
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encrypts outgoing chat with a shared key and decodes messages from other users of the addon.
 *
 * <p>Feature idea from Numby-hack (cqb13), GPL-3.0. This is a XOR obfuscation with Base64 encoding,
 * not real cryptography: it keeps server-side chat logs and curious onlookers from reading what you
 * write, but anyone with the addon and the key can decode it. Both sides must use the same key.</p>
 */
public class ChatEncryption extends Module {
    private static final String MARKER = "[#enc]";

    /** Vanilla chat messages are capped at 256 characters, so keep some headroom. */
    private static final int MAX_ENCRYPTED_LENGTH = 240;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> key = sgGeneral.add(new StringSetting.Builder()
        .name("key")
        .description("Shared secret. Everyone you talk to must set the same one.")
        .defaultValue("change-me")
        .wide()
        .build()
    );

    private final Setting<Boolean> encryptOutgoing = sgGeneral.add(new BoolSetting.Builder()
        .name("encrypt-outgoing")
        .description("Encrypts the messages you send. Commands are never touched.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> decryptIncoming = sgGeneral.add(new BoolSetting.Builder()
        .name("decrypt-incoming")
        .description("Decodes messages that carry the encryption marker.")
        .defaultValue(true)
        .build()
    );

    private boolean warnedAboutLength;

    public ChatEncryption() {
        super(AllAddons.CATEGORY, "chat-encryption", "Encrypts chat with a shared key and decodes messages from other users of this addon.");
    }

    @Override
    public void onActivate() {
        warnedAboutLength = false;
    }

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        if (!encryptOutgoing.get()) return;

        String message = event.message;
        if (message == null || message.isBlank()) return;

        // Commands, and anything that is already encrypted, are left alone.
        if (message.startsWith("/") || message.contains(MARKER)) return;

        String encrypted = MARKER + encrypt(message);

        if (encrypted.length() > MAX_ENCRYPTED_LENGTH) {
            if (!warnedAboutLength) {
                warning("Message is too long to encrypt, sending it unencrypted. Shorten it to stay private.");
                warnedAboutLength = true;
            }
            return;
        }

        event.message = encrypted;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (!decryptIncoming.get()) return;

        Component message = event.getMessage();
        String text = message.getString();

        int index = text.indexOf(MARKER);
        if (index < 0) return;

        String payload = text.substring(index + MARKER.length()).trim();

        // Stop at the first space: servers may append their own formatting around the message.
        int space = payload.indexOf(' ');
        if (space > 0) payload = payload.substring(0, space);

        String decoded = decrypt(payload);
        if (decoded == null) return;

        event.setMessage(Component.literal("§d[encrypted] §f" + decoded));
    }

    private String encrypt(String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.get().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length == 0) return Base64.getEncoder().encodeToString(data);

        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) result[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);

        return Base64.getEncoder().encodeToString(result);
    }

    private String decrypt(String payload) {
        if (payload.isBlank()) return null;

        try {
            byte[] data = Base64.getDecoder().decode(payload);
            byte[] keyBytes = key.get().getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length == 0) return new String(data, StandardCharsets.UTF_8);

            byte[] result = new byte[data.length];
            for (int i = 0; i < data.length; i++) result[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);

            return new String(result, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Not our payload after all - leave the original message untouched.
            return null;
        }
    }
}
