package com.mod.client.module

import com.mod.client.category.Categories
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import net.minecraft.core.Holder
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffects

object NoBlind : Module(
    name = "No Blind",
    description = "Removes blindness and darkness visual effects.",
    category = Categories.ONEADDONS
) {
    private val removeEffects by BooleanSetting(
        "Remove Effects",
        true,
        desc = "Removes blindness and darkness visual effects."
    )

    @JvmStatic
    fun shouldHideEffect(effect: Holder<MobEffect>): Boolean {
        return enabled && removeEffects &&
            (effect.value() == MobEffects.BLINDNESS.value() ||
                effect.value() == MobEffects.DARKNESS.value())
    }
}
