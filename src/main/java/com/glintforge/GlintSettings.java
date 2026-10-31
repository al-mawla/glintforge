package com.glintforge;

/** Per-item glint tweaks. texture == "" means the vanilla glint texture. */
public final class GlintSettings {
    public float size = 1.0f;     // pattern scale (2 = pattern twice as big)
    public float opacity = 1.0f;  // 0..1, baked into the texture brightness (glint blend is additive)
    public float speed = 1.0f;    // 0 = frozen
    public String texture = "";   // file name inside config/glintforge/textures/

    public GlintSettings copy() {
        GlintSettings s = new GlintSettings();
        s.size = size; s.opacity = opacity; s.speed = speed; s.texture = texture;
        return s;
    }

    public boolean isVanilla() {
        return size == 1f && opacity == 1f && speed == 1f && (texture == null || texture.isEmpty());
    }

    public String cacheKey() {
        return size + "|" + opacity + "|" + speed + "|" + texture;
    }
}
