package com.glintforge.mixin;

import com.glintforge.GlintConfig;
import com.glintforge.GlintContext;
import com.glintforge.GlintTags;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin {
    private static final String GLINT =
        "Lnet/minecraft/client/renderer/rendertype/RenderTypes;glint()Lnet/minecraft/client/renderer/rendertype/RenderType;";
    private static final String GLINT_TRANSLUCENT =
        "Lnet/minecraft/client/renderer/rendertype/RenderTypes;glintTranslucent()Lnet/minecraft/client/renderer/rendertype/RenderType;";

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void glintforge$begin(MultiBufferSource.BufferSource source, OutlineBufferSource outline,
                                  SubmitNodeStorage.ItemSubmit submit, CallbackInfo ci) {
        String id = GlintTags.get(submit.quads());
        GlintContext.current = id == null ? null : GlintConfig.get(id);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void glintforge$end(MultiBufferSource.BufferSource source, OutlineBufferSource outline,
                                SubmitNodeStorage.ItemSubmit submit, CallbackInfo ci) {
        GlintContext.current = null;
    }

    @WrapOperation(method = "renderItem", at = @At(value = "INVOKE", target = GLINT))
    private RenderType glintforge$glintA(Operation<RenderType> op) { return GlintContext.swap(op.call()); }

    @WrapOperation(method = "renderItem", at = @At(value = "INVOKE", target = GLINT_TRANSLUCENT))
    private RenderType glintforge$glintB(Operation<RenderType> op) { return GlintContext.swap(op.call()); }

    @WrapOperation(method = { "getFoilBuffer", "getFoilRenderType" }, at = @At(value = "INVOKE", target = GLINT))
    private static RenderType glintforge$glintC(Operation<RenderType> op) { return GlintContext.swap(op.call()); }

    @WrapOperation(method = { "getFoilBuffer", "getFoilRenderType" }, at = @At(value = "INVOKE", target = GLINT_TRANSLUCENT))
    private static RenderType glintforge$glintD(Operation<RenderType> op) { return GlintContext.swap(op.call()); }
}
