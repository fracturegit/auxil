package net.mcbrawls.auxil.provider.raw

import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource

class RawDirectoryProvider(vararg val files: Key) : ResourceProvider {
    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return sources.keys.filter { key ->
            files.any { it.namespace() == key.namespace() && key.value().startsWith(it.value()) }
        }.associateWith(PackResource::Direct)
    }
}
