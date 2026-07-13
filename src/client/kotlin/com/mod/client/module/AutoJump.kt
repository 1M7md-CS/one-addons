package com.mod.client.module

import com.mod.client.category.Categories
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.client.KeyMapping
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

object AutoJump : Module(
    name = "Auto Jump",
    description = "Automatically jumps when colliding with a one-block-high obstacle while moving forward.",
    category = Categories.ONEADDONS
) {
    private val jumpDelay by NumberSetting("Delay", 2, 0, 10, 1, unit = "t", desc = "Cooldown in ticks between jumps.")

    private var wantsJump = false
    private var cooldown = 0

    init {
        on<TickEvent.Start> {
            if (wantsJump) {
                setKeyMappingDown(mc.options.keyJump, false)
                wantsJump = false
                return@on
            }

            if (cooldown > 0) { cooldown--; return@on }

            val player = mc.player ?: return@on
            val level = mc.level ?: return@on
            if (mc.screen != null) return@on
            if (!player.onGround()) return@on
            if (!mc.options.keyUp.isDown) return@on

            val yaw = Math.toRadians(player.yRot.toDouble())
            val dirX = -sin(yaw)
            val dirZ = cos(yaw)

            val feetY = floor(player.y).toInt()

            for (dist in 1..2) {
                val checkX = floor(player.x + dirX * dist * 0.5).toInt()
                val checkZ = floor(player.z + dirZ * dist * 0.5).toInt()
                val blockPos = BlockPos(checkX, feetY, checkZ)

                val state = level.getBlockState(blockPos)
                if (state.isAir) continue

                val shape = state.getCollisionShape(level, blockPos)
                if (shape.isEmpty) continue

                val above = level.getBlockState(blockPos.above())
                if (!above.isAir) continue

                val headPos = BlockPos(player.blockPosition().above(2))
                if (!level.getBlockState(headPos).isAir) continue

                setKeyMappingDown(mc.options.keyJump, true)
                wantsJump = true
                cooldown = jumpDelay
                return@on
            }
        }
    }

    override fun onDisable() {
        if (wantsJump) {
            setKeyMappingDown(mc.options.keyJump, false)
            wantsJump = false
        }
        cooldown = 0
        super.onDisable()
    }

    private fun setKeyMappingDown(km: KeyMapping, down: Boolean) {
        try {
            val f = KeyMapping::class.java.getDeclaredField("isDown")
            f.isAccessible = true
            f.setBoolean(km, down)
        } catch (_: Exception) {}
        if (!down) {
            try {
                val f = KeyMapping::class.java.getDeclaredField("clickCount")
                f.isAccessible = true
                f.setInt(km, 0)
            } catch (_: Exception) {}
            try {
                val f = KeyMapping::class.java.getDeclaredField("wasDown")
                f.isAccessible = true
                f.setBoolean(km, false)
            } catch (_: Exception) {}
        }
    }
}
