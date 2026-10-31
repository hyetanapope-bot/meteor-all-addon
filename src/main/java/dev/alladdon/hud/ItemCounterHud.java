/*
 * AllAddons - a single Meteor Client addon bundling the most useful community features.
 * Copyright (C) 2026 AllAddons contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. See LICENSE for the full text.
 */

package dev.alladdon.hud;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists how many of each item you carry.
 *
 * <p>Feature idea from Numby-hack (cqb13), GPL-3.0. Everything in the player inventory is counted,
 * including the hotbar and the offhand.</p>
 */
public class ItemCounterHud extends HudElement {
    public static final HudElementInfo<ItemCounterHud> INFO = new HudElementInfo<>(
        Hud.GROUP, "item-counter", "Counts how many of each item you are carrying.", ItemCounterHud::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
        .name("sort")
        .description("How the list is sorted.")
        .defaultValue(SortMode.Count)
        .build()
    );

    private final Setting<Integer> maxLines = sgGeneral.add(new IntSetting.Builder()
        .name("max-lines")
        .description("How many item types are shown at most.")
        .defaultValue(8)
        .min(1)
        .sliderRange(1, 30)
        .build()
    );

    private final Setting<Boolean> showTotal = sgGeneral.add(new BoolSetting.Builder()
        .name("show-total")
        .description("Adds a line with the total amount of items.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Text shadow.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Item name color.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> countColor = sgGeneral.add(new ColorSetting.Builder()
        .name("count-color")
        .description("Amount color.")
        .defaultValue(new SettingColor(170, 120, 255))
        .build()
    );

    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays a background behind the text.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Background color.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    public ItemCounterHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        List<Line> lines = collect();
        if (lines.isEmpty() && !isInEditor()) return;

        double lineHeight = renderer.textHeight(shadow.get(), Hud.get().getTextScale()) + 2;
        double width = 0;
        double y = this.y;

        for (Line line : lines) {
            double x = renderer.text(line.name(), this.x, y, textColor.get(), shadow.get(), Hud.get().getTextScale());
            x = renderer.text(" x" + line.count(), x, y, countColor.get(), shadow.get(), Hud.get().getTextScale());

            width = Math.max(width, x - this.x);
            y += lineHeight;
        }

        if (showTotal.get() && !lines.isEmpty()) {
            double x = renderer.text("Total", this.x, y, textColor.get(), shadow.get(), Hud.get().getTextScale());
            x = renderer.text(" " + totalOf(lines), x, y, countColor.get(), shadow.get(), Hud.get().getTextScale());

            width = Math.max(width, x - this.x);
            y += lineHeight;
        }

        setSize(width, y - this.y);

        if (background.get()) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }
    }

    private List<Line> collect() {
        List<Line> lines = new ArrayList<>();

        if (mc.player == null) {
            if (isInEditor()) {
                lines.add(new Line("Diamond", 64));
                lines.add(new Line("Obsidian", 32));
                lines.add(new Line("Totem of Undying", 5));
            }

            return lines;
        }

        Map<Item, Integer> counts = new HashMap<>();
        Map<Item, String> names = new HashMap<>();

        var inventory = mc.player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            names.putIfAbsent(stack.getItem(), stack.getHoverName().getString());
        }

        for (Map.Entry<Item, Integer> entry : counts.entrySet()) {
            lines.add(new Line(names.getOrDefault(entry.getKey(), "?"), entry.getValue()));
        }

        Comparator<Line> comparator = sortMode.get() == SortMode.Name
            ? Comparator.comparing(Line::name, String.CASE_INSENSITIVE_ORDER)
            : Comparator.comparingInt(Line::count).reversed();

        lines.sort(comparator.thenComparing(Line::name));

        return lines.size() > maxLines.get() ? new ArrayList<>(lines.subList(0, maxLines.get())) : lines;
    }

    private int totalOf(List<Line> lines) {
        int total = 0;
        for (Line line : lines) total += line.count();
        return total;
    }

    private record Line(String name, int count) {
    }

    public enum SortMode {
        Count,
        Name
    }
}
