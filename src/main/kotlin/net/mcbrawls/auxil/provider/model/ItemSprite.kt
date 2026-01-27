package net.mcbrawls.auxil.provider.model

import net.kyori.adventure.key.Key

data class ItemSprite(
    val key: Key,
    val scale: Triple<Double, Double, Double>? = null,
) {
    val fullKey = Key.key(key.namespace(), "sprites/${key.value()}")
}
