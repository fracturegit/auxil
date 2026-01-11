package net.mcbrawls.auxil.resource

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.kyori.adventure.key.Key

interface PackResource {
    fun createBytes(sources: Map<Key, ByteArray>): ByteArray

    class Raw(val byteArray: ByteArray) : PackResource {
        override fun createBytes(sources: Map<Key, ByteArray>): ByteArray {
            return byteArray
        }
    }

    class RawJson(val json: JsonElement) : PackResource {
        override fun createBytes(sources: Map<Key, ByteArray>): ByteArray {
            val json = gson.toJson(json)
            return json.encodeToByteArray()
        }

        companion object {
            private val gson = Gson()
        }
    }

    class Direct(val key: Key, val fallback: PackResource? = null): PackResource {
        override fun createBytes(sources: Map<Key, ByteArray>): ByteArray {
            return sources[key] ?: fallback?.createBytes(sources) ?: error("No source for key: $key")
        }
    }
}
