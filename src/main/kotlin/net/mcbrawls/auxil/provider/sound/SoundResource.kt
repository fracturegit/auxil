package net.mcbrawls.auxil.provider.sound

import net.kyori.adventure.key.Key
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class SoundResource(
    val id: Key,
    val sounds: Collection<Key>,
    val stream: Boolean,
) {
    companion object {
        fun create(id: Key, builder: Builder.() -> Unit = {
            sounds.add(id)
        }): SoundResource {
            return Builder().apply(builder).build(id)
        }

        fun create(id: Key, keys: Collection<Key>): SoundResource {
            return create(id) {
                sounds.addAll(keys)
            }
        }

        fun create(id: Key, vararg keys: Key): SoundResource {
            return create(id) {
                sounds.addAll(keys)
            }
        }
    }

    class Builder {
        val sounds: MutableSet<Key> = mutableSetOf()
        var stream: Boolean = false

        fun build(id: Key): SoundResource {
            if (sounds.isEmpty()) logger.warn("Sounds were empty: $id")
            return SoundResource(id, sounds, stream)
        }

        companion object {
            private val logger: Logger = LoggerFactory.getLogger(Builder::class.java)
        }
    }
}
