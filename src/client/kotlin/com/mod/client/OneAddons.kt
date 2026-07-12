package com.mod.client

import com.mod.client.module.AutoExperiment
import com.mod.client.module.ChestAssist
import com.mod.client.module.Flower
import com.mod.client.module.KeyMaker
import com.mod.client.module.Mushroom
import com.mod.client.module.PlaceOnPosition
import com.mod.client.module.SwapAssist
import com.mod.client.module.ToggleKey
import com.mod.client.module.Waypoint
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.features.ModuleManager
import net.fabricmc.api.ClientModInitializer

class OneAddons : ClientModInitializer {

    override fun onInitializeClient() {
        val moduleConfig = ModuleConfig("oneaddons")
        ModuleManager.registerModules(
            moduleConfig,
            AutoExperiment,
            Flower,
            Mushroom,
            ChestAssist,
            Waypoint,
            SwapAssist,
            PlaceOnPosition,
            KeyMaker,
            ToggleKey
        )
    }
}
