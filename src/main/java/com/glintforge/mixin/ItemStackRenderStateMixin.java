package com.glintforge.mixin;

import com.glintforge.GlintKeyHolder;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Remembers which item this render state belongs to. The actual glint hook comes in the next step. */
@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin implements GlintKeyHolder {
    @Unique private String glintforge$item;

    @Override public String glintforge$getItem() { return glintforge$item; }
    @Override public void glintforge$setItem(String id) { glintforge$item = id; }
}
