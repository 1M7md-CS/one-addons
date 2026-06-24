package com.mod.client.compat;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class KeyBindingCompat {

    public static KeyMapping register(String translationKey, int keyCode, String category) {
        KeyMapping mapping = new KeyMapping(
                translationKey,
                keyCode,
                new KeyMapping.Category(Identifier.withDefaultNamespace(category))
        );
        return KeyMappingHelper.registerKeyMapping(mapping);
    }

    private KeyBindingCompat() {}
}
