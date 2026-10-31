package com.glintforge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GlintConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<LinkedHashMap<String, GlintSettings>>() {}.getType();
    private static Map<String, GlintSettings> items = new LinkedHashMap<>();

    public static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("glintforge"); }
    public static Path textureDir() { return dir().resolve("textures"); }
    private static Path file() { return dir().resolve("items.json"); }

    public static void load() {
        try {
            Files.createDirectories(textureDir());
            if (Files.exists(file())) {
                Map<String, GlintSettings> m = GSON.fromJson(Files.readString(file()), TYPE);
                if (m != null) items = m;
            }
        } catch (IOException | RuntimeException e) {
            GlintForge.LOGGER.error("Could not load glint config", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(dir());
            Files.writeString(file(), GSON.toJson(items, TYPE));
        } catch (IOException e) {
            GlintForge.LOGGER.error("Could not save glint config", e);
        }
        GlintVariants.clear();
    }

    public static GlintSettings get(String itemId) { return items.get(itemId); }
    public static void put(String itemId, GlintSettings s) { items.put(itemId, s); save(); }
    public static void remove(String itemId) { items.remove(itemId); save(); }
    public static java.util.List<String> ids() { return new java.util.ArrayList<>(items.keySet()); }
}
