/*
 * AllAddons - a single Meteor Client addon bundling the most useful community features.
 * Copyright (C) 2026 AllAddons contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. See LICENSE for the full text.
 */

package dev.alladdon;

import com.mojang.logging.LogUtils;
import dev.alladdon.hud.ItemCounterHud;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

/**
 * Entrypoint of the addon.
 *
 * <p>Everything lives in a single category so the ClickGUI stays tidy: modules are discovered
 * automatically by Meteor (it scans {@link #getPackage()}), only categories and HUD elements have
 * to be registered by hand.</p>
 */
public class AllAddons extends MeteorAddon {
    /** Kept in sync with {@code mod-version} in {@code gradle/libs.versions.toml}. */
    public static final String VERSION = "1.0.0";

    public static final Logger LOG = LogUtils.getLogger();

    /** Shared category for every module in this addon. */
    public static final Category CATEGORY = new Category("AllAddons");

    @Override
    public void onInitialize() {
        LOG.info("Initializing AllAddons v{}", VERSION);

        // HUD elements are not modules, so they are not picked up by the reflection scan.
        Hud.get().register(ItemCounterHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "dev.alladdon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("hyetanapope-bot", "meteor-all-addon", "main", null);
    }
}
