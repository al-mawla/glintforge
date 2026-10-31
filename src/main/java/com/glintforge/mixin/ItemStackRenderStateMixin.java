package com.glintforge.mixin;

import com.glintforge.GlintConfig;
import com.glintforge.GlintKeyHolder;
import com.glintforge.GlintSettings;
import com.glintforge.GlintVariants;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * For items with custom settings: submit the item without vanilla foil, then submit the same quads
 * a second time with our per-item glint RenderType (the glint pipeline uses depth-EQUAL, so it
 * overlays exactly like vanilla's glint pass).
 */
@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin implements GlintKeyHolder {
    @Unique private String glintforge$item;

    @Override public String glintforge$getItem() { return glintforge$item; }
    @Override public void glintforge$setItem(String id) { glintforge$item = id; }

    @WrapOperation(method = "submit", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"))
    private void glintforge$submit(SubmitNodeCollector collector, PoseStack pose, ItemDisplayContext ctx,
                                   int light, int overlay, int outline, int[] tints, List<BakedQuad> quads,
                                   RenderType rt, ItemStackRenderState.FoilType foil,
                                   Operation<Void> original) {
        GlintSettings s = glintforge$item == null ? null : GlintConfig.get(glintforge$item);
        if (s == null || foil == ItemStackRenderState.FoilType.NONE) {
            original.call(collector, pose, ctx, light, overlay, outline, tints, quads, rt, foil);
            return;
        }
        original.call(collector, pose, ctx, light, overlay, outline, tints, quads, rt, ItemStackRenderState.FoilType.NONE);
        if (s.opacity > 0f) {
            collector.submitItem(pose, ctx, light, overlay, outline, tints, quads,
                    GlintVariants.get(s), ItemStackRenderState.FoilType.NONE);
        }
    }
}
