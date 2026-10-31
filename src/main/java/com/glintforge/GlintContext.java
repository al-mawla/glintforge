package com.glintforge;

import net.minecraft.client.renderer.rendertype.RenderType;

/** Holds the settings of the item that is being drawn right now (rendering is single-threaded). */
public final class GlintContext {
    public static GlintSettings current;
    private static boolean logged = false;

    public static RenderType swap(RenderType vanilla) {
        GlintSettings s = current;
        if (s == null) return vanilla;
        if (!logged) { logged = true; GlintForge.LOGGER.info("GlintForge: custom glint applied to an item"); }
        return GlintVariants.get(s);
    }
}
