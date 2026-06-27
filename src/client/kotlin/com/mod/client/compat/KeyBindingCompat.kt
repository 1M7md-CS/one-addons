package com.mod.client.compat

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier

object KeyBindingCompat {
    @JvmStatic
    fun register(translationKey: String, keyCode: Int, category: String): KeyMapping {
        val mapping = KeyMapping(
            translationKey,
            keyCode,
            KeyMapping.Category(Identifier.withDefaultNamespace(category))
        )
        return KeyMappingHelper.registerKeyMapping(mapping)
    }
}
