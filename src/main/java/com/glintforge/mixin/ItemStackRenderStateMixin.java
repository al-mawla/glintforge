package com.glintforge.mixin;

import com.glintforge.GlintKeyHolder;
import com.glintforge.GlintTags;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin implements GlintKeyHolder {
    @Unique private String glintforge$item;

    @Shadow private int activeLayerCount;
    @Shadow private ItemStackRenderState.LayerRenderState[] layers;

    @Override public String glintforge$getItem() { return glintforge$item; }
    @Override public void glintforge$setItem(String id) { glintforge$item = id; }

    /** Right before drawing: link each layer's quad list to this item's id. */
    @Inject(method = "submit", at = @At("HEAD"))
    private void glintforge$register(PoseStack pose, SubmitNodeCollector collector, int a, int b, int c, CallbackInfo ci) {
        for (int i = 0; i < activeLayerCount && i < layers.length; i++) {
            ItemStackRenderState.LayerRenderState layer = layers[i];
            if (layer != null) {
                GlintTags.put(((LayerRenderStateAccessor) (Object) layer).glintforge$quads(), glintforge$item);
            }
        }
    }
}
