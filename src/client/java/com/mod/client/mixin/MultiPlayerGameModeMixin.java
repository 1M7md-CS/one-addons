package com.mod.client.mixin;

import com.mod.client.OneAddons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "net.minecraft.client.player.LocalPlayer")
public class MultiPlayerGameModeMixin {
    @ModifyConstant(method = "method_2902", constant = @Constant(intValue = 5))
    private int removeMiningCooldown(int value) {
        return OneAddons.cooldownFixEnabled ? 0 : value;
    }
}
