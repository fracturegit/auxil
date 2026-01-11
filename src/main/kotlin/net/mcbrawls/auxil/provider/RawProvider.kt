package net.mcbrawls.auxil.provider

import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.resource.PackResource

class RawProvider(vararg val files: Key) : ResourceProvider {
    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return files.associateWith(PackResource::Direct)
    }
}
