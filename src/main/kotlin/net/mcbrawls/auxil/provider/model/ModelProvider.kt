package net.mcbrawls.auxil.provider.model

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.kyori.adventure.key.Key
import net.mcbrawls.api.registry.BasicRegistry
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource

class ModelProvider(val models: Set<Key>) : ResourceProvider {
    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return buildMap {
            val allTextures = mutableSetOf<Key>()

            models.forEach { key ->
                val modelSourceFile = createKey(key, "models", "json")
                sources[modelSourceFile]?.let { bytes ->
                    val jsonString = bytes.decodeToString()
                    val json = gson.fromJson(jsonString, JsonObject::class.java)
                    json["textures"]?.let(JsonElement::getAsJsonObject)?.let { texturesObj ->
                        val textures = texturesObj.asMap().values
                            .filterIsInstance<JsonPrimitive>()
                            .map(JsonPrimitive::getAsString)

                        val rawTextureKeys = textures.mapNotNull { texture ->
                            runCatching {
                                Key.key(texture)
                            }.getOrNull()
                        }
                        val textureKeys = rawTextureKeys.map { Key.key(it.namespace(), "textures/${it.value()}.png") }
                        textureKeys.forEach { this[it] = PackResource.Direct(it) }
                        allTextures.addAll(rawTextureKeys)
                    }

                    this[createKey(key, "models/item", "json")] = PackResource.Direct(modelSourceFile)

                    this[createKey(key, "items", "json")] = PackResource.RawJson(
                        obj {
                            "oversized_in_gui" to true
                            "model" to obj {
                                "type" to "minecraft:model"
                                "model" to Key.key(key.namespace(), "item/${key.value()}")
                                "tints" to array(
                                    obj {
                                        "type" to "minecraft:dye"
                                        "default" to 0xFFFFFFFF
                                    }
                                )
                            }
                        }
                    )
                }
            }

            this[Key.key("atlases/blocks.json")] = PackResource.RawJson(
                obj {
                    "sources" to array(
                        *allTextures.map { key ->
                            obj {
                                "type" to "single"
                                "resource" to key
                            }
                        }.toTypedArray()
                    )
                }
            )
        }
    }

    class Builder {
        private val models: MutableSet<Key> = mutableSetOf()

        fun add(vararg models: Key): Builder {
            this.models.addAll(models)
            return this
        }

        fun add(models: Collection<Key>): Builder {
            this.models.addAll(models)
            return this
        }

        fun add(registry: BasicRegistry<Key>): Builder {
            registry.collectEntries().forEach(::add)
            return this
        }

        fun build(): ModelProvider {
            return ModelProvider(models)
        }
    }

    companion object {
        private val gson = Gson()

        fun builder(): Builder {
            return Builder()
        }

        fun createKey(key: Key, folder: String, extension: String): Key {
            return Key.key(key.namespace(), "$folder/${key.value()}.$extension")
        }
    }
}
