package net.mcbrawls.auxil.provider.sound

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import com.google.gson.JsonArray
import net.kyori.adventure.key.Key
import net.mcbrawls.api.registry.Registry
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource

class SoundProvider(
    sounds: Set<SoundResource>,
    unimplementedSound: Key?,
    val attenuationDistances: MutableMap<Key, Int>,
) : ResourceProvider {
    val soundsByNamespace: Map<String, List<SoundResource>> = sounds.groupBy { sound -> sound.id.namespace() }
    val unimplementedSoundResource: PackResource? = unimplementedSound?.let(::createSoundFileKey)?.let(PackResource::Direct)

    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return buildMap {
            soundsByNamespace.forEach { (namespace, sounds) ->
                // sounds.json
                this[Key.key(namespace, "sounds.json")] = PackResource.RawJson(
                    obj {
                        sounds.forEach { (id, sounds) ->
                            id.value() to obj {
                                "sounds" to JsonArray().also { array ->
                                    sounds.forEach { key ->
                                        val distance = attenuationDistances[key]
                                        if (distance != null) {
                                            array.add(
                                                obj {
                                                    "name" to key
                                                    "attenuation_distance" to distance
                                                }
                                            )
                                        } else {
                                            array.add(key.toString())
                                        }
                                    }
                                }
                            }
                        }
                    }
                )

                // individual sound files
                sounds.forEach { resource ->
                    resource.sounds.forEach { key ->
                        val fileKey = createSoundFileKey(key)
                        this[fileKey] = PackResource.Direct(fileKey, unimplementedSoundResource)
                    }
                }
            }
        }
    }

    private fun createSoundFileKey(key: Key): Key = Key.key(key.namespace(), "sounds/${key.value()}.ogg")

    class Builder {
        private val sounds: MutableSet<SoundResource> = mutableSetOf()
        private var unimplementedSound: Key? = null
        private val attenuationDistances: MutableMap<Key, Int> = mutableMapOf()

        fun addKeys(sounds: Collection<Key>): Builder {
            sounds.map(SoundResource::create).forEach(::add)
            return this
        }

        fun addKeys(vararg sounds: Key): Builder {
            return addKeys(sounds.toSet())
        }

        fun add(sounds: Collection<SoundResource>): Builder {
            this.sounds.addAll(sounds)
            return this
        }

        fun add(vararg sounds: SoundResource): Builder {
            return add(sounds.toSet())
        }

        fun add(registry: Registry<SoundResource>): Builder {
            return add(registry.collectEntries())
        }

        fun <T : Any> add(registry: Registry<T>, transform: (T) -> SoundResource): Builder {
            registry.collectEntries()
                .map(transform)
                .forEach(::add)

            return this
        }

        fun unimplemented(key: Key): Builder {
            this.unimplementedSound = key
            return this
        }

        fun attenuation(distances: Map<Key, Int>): Builder {
            attenuationDistances.putAll(distances)
            return this
        }

        fun build(): SoundProvider {
            return SoundProvider(sounds, unimplementedSound, attenuationDistances)
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }
    }
}
