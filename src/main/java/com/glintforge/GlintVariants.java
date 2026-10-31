package com.glintforge;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix4f;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Builds and caches one glint RenderType per distinct settings combination.
 * Size/speed live in the texture-transform matrix; opacity + custom image live in a DynamicTexture.
 *
 * NOTE: everything touching RenderSetup / RenderType / TextureTransform is the most likely place
 * to need small signature fixes on 26.1.2. See README notes in the chat.
 */
public final class GlintVariants {
    private static final Identifier VANILLA_GLINT = Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
    private static final Map<String, Variant> CACHE = new HashMap<>();
    private static int counter = 0;

    private record Variant(RenderType type, Identifier texture) {}

    public static RenderType get(GlintSettings s) {
        return CACHE.computeIfAbsent(s.cacheKey(), k -> build(s)).type();
    }

    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        for (Variant v : CACHE.values()) mc.getTextureManager().release(v.texture());
        CACHE.clear();
    }

    public static List<String> listTextures() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> st = Files.list(GlintConfig.textureDir())) {
            st.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
              .forEach(p -> out.add(p.getFileName().toString()));
        } catch (Exception ignored) {}
        java.util.Collections.sort(out);
        return out;
    }

    private static Variant build(GlintSettings s) {
        int n = counter++;
        Identifier texId = Identifier.fromNamespaceAndPath(GlintForge.MOD_ID, "dyn/glint_" + n);
        NativeImage img = makeImage(s);
        Minecraft.getInstance().getTextureManager().register(texId, new DynamicTexture(() -> "glintforge_" + texId.getPath(), img));

        final float speed = s.speed;
        final float scale = 8.0f / Math.max(0.05f, s.size);
        TextureTransform transform = new TextureTransform("glintforge_transform_" + n, () -> {
            long t = (long) (Util.getMillis() * (double) speed * 8.0);
            float f = (float) (t % 110000L) / 110000.0f;
            float g = (float) (t % 30000L) / 30000.0f;
            return new Matrix4f().translation(-f, g, 0.0f).rotateZ(0.17453292f).scale(scale);
        });

        RenderSetup setup = RenderSetup.builder(RenderPipelines.GLINT)
                .withTexture("Sampler0", texId, true)
                .setTextureTransform(transform)
                .createRenderSetup();
        return new Variant(RenderType.create("glintforge_" + n, setup), texId);
    }

    private static NativeImage makeImage(GlintSettings s) {
        NativeImage src = null;
        try {
            if (s.texture != null && !s.texture.isEmpty()) {
                Path p = GlintConfig.textureDir().resolve(s.texture).normalize();
                if (p.startsWith(GlintConfig.textureDir()) && Files.exists(p)) {
                    try (InputStream in = Files.newInputStream(p)) { src = NativeImage.read(in); }
                }
            }
            if (src == null) {
                var res = Minecraft.getInstance().getResourceManager().getResource(VANILLA_GLINT).orElseThrow();
                try (InputStream in = res.open()) { src = NativeImage.read(in); }
            }
        } catch (Exception e) {
            GlintForge.LOGGER.error("Failed to load glint texture '{}', using blank", s.texture, e);
        }
        if (src == null) src = new NativeImage(16, 16, true);

        // The glint pipeline blends additively, so alpha is ignored: scale RGB by opacity instead.
        int w = src.getWidth(), h = src.getHeight();
        NativeImage out = new NativeImage(w, h, false);
        float o = Math.max(0f, Math.min(1f, s.opacity));
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getPixel(x, y);
                int a = argb >>> 24;
                int r = (int) (((argb >> 16) & 0xFF) * o * a / 255f);
                int g = (int) (((argb >> 8) & 0xFF) * o * a / 255f);
                int b = (int) ((argb & 0xFF) * o * a / 255f);
                out.setPixel(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }
        src.close();
        return out;
    }
}
