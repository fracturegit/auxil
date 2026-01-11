package net.mcbrawls.auxil.provider.sound

import net.kyori.adventure.key.Key

data class SoundResource(
    val id: Key,
    val sounds: Collection<Key>,
) {
    companion object {
        fun create(id: Key): SoundResource {
            return SoundResource(id, setOf(id))
        }

        fun create(id: Key, vararg sounds: Key): SoundResource {
            return SoundResource(id, sounds.toSet())
        }
    }
}
