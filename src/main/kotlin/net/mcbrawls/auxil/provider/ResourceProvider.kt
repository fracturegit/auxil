package net.mcbrawls.auxil.provider

import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.resource.PackResource

interface ResourceProvider {
    fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource>
}
