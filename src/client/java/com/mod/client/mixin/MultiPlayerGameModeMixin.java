package com.mod.client.mixin;

import com.mod.client.OneAddons;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Shadow
    private int destroyDelay;

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void onStartDestroyBlock(CallbackInfoReturnable<Boolean> cir) {
        if (OneAddons.cooldownFixEnabled) {
            this.destroyDelay = 0;
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void onContinueDestroyBlock(CallbackInfoReturnable<Boolean> cir) {
        if (OneAddons.cooldownFixEnabled) {
            this.destroyDelay = 0;
        }
    }
}
