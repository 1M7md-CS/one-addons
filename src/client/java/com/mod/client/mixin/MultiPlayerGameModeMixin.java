package com.mod.client.mixin;

import com.mod.client.OneAddons;
import com.mod.client.compat.DestroyDelayAccessor;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin implements DestroyDelayAccessor {

    @Shadow
    private int destroyDelay;

    @Override
    public void setDestroyDelay(int value) {
        this.destroyDelay = value;
    }

    @Inject(method = "startDestroyBlock", at = @At("TAIL"))
    private void onStartDestroyBlock(CallbackInfoReturnable<Boolean> cir) {
        if (OneAddons.cooldownFixEnabled) {
            DestroyDelayAccessor.reset((MultiPlayerGameMode) (Object) this);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("TAIL"))
    private void onContinueDestroyBlock(CallbackInfoReturnable<Boolean> cir) {
        if (OneAddons.cooldownFixEnabled) {
            DestroyDelayAccessor.reset((MultiPlayerGameMode) (Object) this);
        }
    }
}
