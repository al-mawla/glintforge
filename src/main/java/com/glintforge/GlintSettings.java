package com.glintforge;

/** Per-item glint tweaks. texture == "" means the vanilla glint texture. */
public final class GlintSettings {
    public int pixelSize = 16;    // 16 (vanilla), 32, 64, 128, 256 or 512
    public float opacity = 1.0f;  // 0..1, baked into the texture brightness (glint blend is additive)
    public float speed = 1.0f;    // 0 = frozen
    public String texture = "";   // file name inside config/glintforge/textures/

    public GlintSettings copy() {
        GlintSettings s = new GlintSettings();
        s.pixelSize = pixelSize; s.opacity = opacity; s.speed = speed; s.texture = texture;
        return s;
    }

    public boolean isVanilla() {
        return pixelSize == 16 && opacity == 1f && speed == 1f && (texture == null || texture.isEmpty());
    }

    public String cacheKey() {
        return pixelSize + "|" + opacity + "|" + speed + "|" + texture;
    }
}
