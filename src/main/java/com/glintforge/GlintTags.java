package com.glintforge;

import java.util.IdentityHashMap;
import java.util.Map;

/** Remembers which item a given quad list belongs to, so the renderer can look the item up later. */
public final class GlintTags {
    private static final Map<Object, String> MAP = new IdentityHashMap<>();
    private static boolean logged = false;

    public static void put(Object quads, String itemId) {
        if (quads == null) return;
        if (itemId == null) { MAP.remove(quads); return; }
        if (MAP.size() > 8192) MAP.clear();
        MAP.put(quads, itemId);
        if (!logged) { logged = true; GlintForge.LOGGER.info("GlintForge: item tagging works ({})", itemId); }
    }

    public static String get(Object quads) { return MAP.get(quads); }
}
