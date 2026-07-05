package net.mcbrawls.auxil.provider.raw

import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource

class RawCopyProvider(val source: Key, val destination: Key) : ResourceProvider {
    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return mapOf(destination to PackResource.Direct(source))
    }
}
