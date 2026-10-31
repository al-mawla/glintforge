package com.glintforge;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix4f;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class GlintVariants {
    private static final Map<String, Variant> CACHE = new HashMap<>();
    private static int counter = 0;
    private static GpuSampler sampler;
    private static boolean samplerTried = false;

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

    /** Looks up a repeating + nearest-neighbour sampler (via reflection so a name mismatch can't break the build). */
    private static GpuSampler nearestRepeatSampler() {
        if (samplerTried) return sampler;
        samplerTried = true;
        try {
            Class<?> fm = Class.forName("com.mojang.blaze3d.textures.FilterMode");
            Object nearest = null;
            for (Object c : fm.getEnumConstants()) if (((Enum<?>) c).name().equals("NEAREST")) nearest = c;
            Object cache = com.mojang.blaze3d.systems.RenderSystem.class.getMethod("getSamplerCache").invoke(null);
            for (Method m : cache.getClass().getMethods()) {
                if (!m.getName().equals("getRepeat")) continue;
                Class<?>[] pt = m.getParameterTypes();
                Object[] args = new Object[pt.length];
                boolean ok = true;
                for (int i = 0; i < pt.length; i++) {
                    if (pt[i] == fm) args[i] = nearest;
                    else if (pt[i] == boolean.class) args[i] = Boolean.FALSE;
                    else ok = false;
                }
                if (!ok) continue;
                Object r = m.invoke(cache, args);
                if (r instanceof GpuSampler g) {
                    sampler = g;
                    GlintForge.LOGGER.info("GlintForge: sampler OK (repeat + nearest)");
                    return g;
                }
            }
            GlintForge.LOGGER.warn("GlintForge: sampler lookup found no usable getRepeat method");
        } catch (Throwable t) {
            GlintForge.LOGGER.warn("GlintForge: sampler lookup failed: {}", t.toString());
        }
        return null;
    }

    private static Variant build(GlintSettings s) {
        int n = counter++;
        Identifier texId = Identifier.fromNamespaceAndPath(GlintForge.MOD_ID, "dyn/glint_" + n);
        NativeImage img = makeImage(s);
        Minecraft.getInstance().getTextureManager().register(texId, new DynamicTexture(() -> "glintforge_" + texId.getPath(), img));

        final float speed = s.speed;
        // 16 = vanilla scale (8). Each doubling makes the pattern twice as big.
        final float scale = 8.0f * 16.0f / Math.max(16, s.pixelSize);
        TextureTransform transform = new TextureTransform("glintforge_transform_" + n, () -> {
            long t = (long) (Util.getMillis() * (double) speed * 8.0);
            float f = (float) (t % 110000L) / 110000.0f;
            float g = (float) (t % 30000L) / 30000.0f;
            return new Matrix4f().translation(-f, g, 0.0f).rotateZ(0.17453292f).scale(scale);
        });

        var builder = RenderSetup.builder(RenderPipelines.GLINT);
        final GpuSampler smp = nearestRepeatSampler();
        if (smp != null) {
            builder = builder.withTexture("Sampler0", texId, () -> smp);
        } else {
            builder = builder.withTexture("Sampler0", texId);
        }
        RenderSetup setup = builder.setTextureTransform(transform).createRenderSetup();
        return new Variant(RenderType.create("glintforge_" + n, setup), texId);
    }

    private static NativeImage makeImage(GlintSettings s) {
        NativeImage src = null;
        boolean custom = false;
        try {
            if (s.texture != null && !s.texture.isEmpty()) {
                Path p = GlintConfig.textureDir().resolve(s.texture).normalize();
                if (p.startsWith(GlintConfig.textureDir()) && Files.exists(p)) {
                    try (InputStream in = Files.newInputStream(p)) { src = NativeImage.read(in); custom = true; }
                }
            }
            if (src == null) {
                var res = Minecraft.getInstance().getResourceManager()
                        .getResource(ItemFeatureRenderer.ENCHANTED_GLINT_ITEM).orElseThrow();
                try (InputStream in = res.open()) { src = NativeImage.read(in); }
            }
        } catch (Exception e) {
            GlintForge.LOGGER.error("GlintForge: failed to load glint texture '{}'", s.texture, e);
        }
        if (src == null) src = new NativeImage(16, 16, true);

        // The glint blends additively (alpha is ignored), so opacity = brightness.
        // Only user-supplied textures get their own transparency applied, so see-through areas stay dark.
        int w = src.getWidth(), h = src.getHeight();
        NativeImage out = new NativeImage(w, h, false);
        float o = Math.max(0f, Math.min(1f, s.opacity));
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getPixel(x, y);
                float k = o * (custom ? (argb >>> 24) / 255f : 1f);
                int r = (int) (((argb >> 16) & 0xFF) * k);
                int g = (int) (((argb >> 8) & 0xFF) * k);
                int b = (int) ((argb & 0xFF) * k);
                out.setPixel(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }
        src.close();
        return out;
    }
}
