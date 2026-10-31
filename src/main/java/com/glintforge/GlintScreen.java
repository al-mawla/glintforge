package com.glintforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public class GlintScreen extends Screen {
    private final Screen parent;
    private String itemId = "";
    private GlintSettings draft = new GlintSettings();
    private EditBox idBox;
    private StringWidget status;
    private String statusText = "";

    public GlintScreen(Screen parent) {
        super(Component.literal("GlintForge"));
        this.parent = parent;
        var p = Minecraft.getInstance().player;
        if (p != null && !p.getMainHandItem().isEmpty()) {
            itemId = BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString();
            GlintSettings existing = GlintConfig.get(itemId);
            if (existing != null) draft = existing.copy();
        }
    }

    @Override
    protected void init() {
        int cx = width / 2, w = 220, x = cx - w / 2;
        int y = Math.max(28, height / 2 - 100);

        addRenderableWidget(new StringWidget(x, 10, w, 9, title, font));

        idBox = new EditBox(font, x, y, w, 20, Component.literal("Item ID"));
        idBox.setMaxLength(120);
        idBox.setHint(Component.literal("minecraft:diamond_sword"));
        idBox.setValue(itemId);
        idBox.setResponder(v -> itemId = v.trim());
        addRenderableWidget(idBox);

        addRenderableWidget(Button.builder(Component.literal("Use held item"), b -> {
            var p = Minecraft.getInstance().player;
            if (p == null || p.getMainHandItem().isEmpty()) { setStatus("Hold an item first"); return; }
            itemId = BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString();
            GlintSettings e = GlintConfig.get(itemId);
            if (e != null) draft = e.copy();
            rebuildWidgets();
        }).bounds(x, y + 24, w / 2 - 2, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Saved items >"), b -> {
            List<String> ids = GlintConfig.ids();
            if (ids.isEmpty()) { setStatus("No saved items yet"); return; }
            int i = ids.indexOf(itemId);
            itemId = ids.get((i + 1) % ids.size());
            draft = GlintConfig.get(itemId).copy();
            rebuildWidgets();
        }).bounds(x + w / 2 + 2, y + 24, w / 2 - 2, 20).build());

        addRenderableWidget(new StepSlider(x, y + 52, w, draft.pixelSize, v -> draft.pixelSize = v));
        addRenderableWidget(new Slider(x, y + 76, w, "Opacity", 0.0, 1.0, draft.opacity, v -> draft.opacity = (float) v, "%.0f%%", 100));
        addRenderableWidget(new Slider(x, y + 100, w, "Speed", 0.0, 4.0, draft.speed, v -> draft.speed = (float) v, "%.2fx"));

        addRenderableWidget(Button.builder(textureLabel(), b -> {
            List<String> opts = new ArrayList<>();
            opts.add("");
            opts.addAll(GlintVariants.listTextures());
            int i = Math.max(0, opts.indexOf(draft.texture));
            draft.texture = opts.get((i + 1) % opts.size());
            b.setMessage(textureLabel());
        }).bounds(x, y + 126, w, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Open texture folder"),
                b -> Util.getPlatform().openPath(GlintConfig.textureDir()))
                .bounds(x, y + 150, w, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(x, y + 178, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset item"), b -> {
            if (itemId.isEmpty()) return;
            GlintConfig.remove(itemId);
            draft = new GlintSettings();
            setStatus("Reset " + itemId);
            rebuildWidgets();
        }).bounds(x + 75, y + 178, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x + 150, y + 178, 70, 20).build());

        status = new StringWidget(x, y + 204, w, 9, Component.literal(statusText), font);
        addRenderableWidget(status);
    }

    private Component textureLabel() {
        return Component.literal("Overlay texture: " + (draft.texture.isEmpty() ? "(vanilla)" : draft.texture));
    }

    private void save() {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) { setStatus("Unknown item id"); return; }
        if (draft.isVanilla()) GlintConfig.remove(itemId); else GlintConfig.put(itemId, draft.copy());
        setStatus("Saved " + itemId);
    }

    private void setStatus(String s) {
        statusText = s;
        if (status != null) status.setMessage(Component.literal(s));
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }

    /** Snaps to 16x, 32x, 64x, 128x, 256x, 512x. */
    private static final class StepSlider extends AbstractSliderButton {
        private static final int[] SIZES = { 16, 32, 64, 128, 256, 512 };
        private final IntConsumer onChange;

        StepSlider(int x, int y, int w, int currentSize, IntConsumer onChange) {
            super(x, y, w, 20, Component.empty(), indexOf(currentSize) / 5.0);
            this.onChange = onChange;
            updateMessage();
        }

        private static int indexOf(int s) {
            for (int i = 0; i < SIZES.length; i++) if (SIZES[i] == s) return i;
            return 0;
        }

        private int idx() { return (int) Math.round(value * 5); }

        @Override protected void updateMessage() {
            int s = SIZES[idx()];
            setMessage(Component.literal("Size: " + s + "x" + (s == 16 ? " (default)" : "")));
        }

        @Override protected void applyValue() {
            value = idx() / 5.0;
            onChange.accept(SIZES[idx()]);
        }
    }

    private static final class Slider extends AbstractSliderButton {
        private final String label, fmt;
        private final double min, max, mult;
        private final DoubleConsumer onChange;

        Slider(int x, int y, int w, String label, double min, double max, double init,
               DoubleConsumer onChange, String fmt) { this(x, y, w, label, min, max, init, onChange, fmt, 1); }

        Slider(int x, int y, int w, String label, double min, double max, double init,
               DoubleConsumer onChange, String fmt, double mult) {
            super(x, y, w, 20, Component.empty(), (init - min) / (max - min));
            this.label = label; this.min = min; this.max = max; this.onChange = onChange;
            this.fmt = fmt; this.mult = mult;
            updateMessage();
        }

        private double real() { return min + value * (max - min); }

        @Override protected void updateMessage() {
            setMessage(Component.literal(label + ": " + String.format(fmt, real() * mult)));
        }

        @Override protected void applyValue() { onChange.accept(real()); }
    }
}
