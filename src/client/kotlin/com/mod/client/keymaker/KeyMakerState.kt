package com.mod.client.keymaker

enum class KeyMakerState {
    IDLE,
    SCANNING,
    COLLECTING,
    CRAFT_FURNACE,
    WAIT_PROCESS,
    CONFIRM,
    CLOSE_ABORT,
    WAIT_FORGE_RETURN
}
