package com.glintforge.mixin;

import com.glintforge.GlintKeyHolder;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tags every ItemStackRenderState with the registry id of the stack it was built from. */
@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    @Inject(method = "updateForTopItem", at = @At("TAIL"))
    private void glintforge$tag(ItemStackRenderState state, ItemStack stack, ItemDisplayContext ctx,
                                Level level, ItemOwner owner, int seed, CallbackInfo ci) {
        ((GlintKeyHolder) state).glintforge$setItem(
                stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
}
