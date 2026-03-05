package net.mcbrawls.auxil.provider.font

import net.kyori.adventure.key.Key

data class Font(
    val key: Key,
    val size: Double,
    val oversample: Double,
    val shift: FontShift? = null,
)
