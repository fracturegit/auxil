package net.mcbrawls.auxil.provider.model

import net.kyori.adventure.key.Key

data class ItemSprite(
    val textureKey: Key,
    val scale: Scale? = null,
) {
    data class Scale(val x: Double, val y: Double, val z: Double) {
        constructor(scale: Double) : this(scale, scale, scale)
    }
}
