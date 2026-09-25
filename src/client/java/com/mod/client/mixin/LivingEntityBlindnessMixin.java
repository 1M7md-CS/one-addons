package com.mod.client.mixin;

import com.mod.client.module.NoBlind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBlindnessMixin {
    @Inject(
        method = "hasEffect(Lnet/minecraft/core/Holder;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void oneaddons$hideEffects(
        Holder<MobEffect> effect,
        CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self == Minecraft.getInstance().player && NoBlind.shouldHideEffect(effect)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F",
        at = @At("HEAD"),
        cancellable = true
    )
    private void oneaddons$hideEffectBlendFactor(
        Holder<MobEffect> effect,
        float tickDelta,
        CallbackInfoReturnable<Float> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self == Minecraft.getInstance().player && NoBlind.shouldHideEffect(effect)) {
            cir.setReturnValue(0.0f);
        }
    }
}
