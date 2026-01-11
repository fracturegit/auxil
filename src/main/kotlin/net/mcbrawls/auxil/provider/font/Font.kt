package net.mcbrawls.auxil.provider.font

import net.kyori.adventure.key.Key

data class Font(
    val key: Key,
    val size: Double,
    val oversample: Double,
    val shift: Pair<Double, Double>? = null,
) {
    val fullKey: Key by lazy {
        if (shift == null) {
            key
        } else {
            val value = "${key.value()}_${shift.first}_${shift.second}"
            Key.key(key.namespace(), value)
        }
    }
}
