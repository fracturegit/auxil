package net.mcbrawls.auxil.provider.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.kyori.adventure.key.Key
import net.mcbrawls.api.registry.Registry
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource

class ModelProvider(
    val models: Set<Key>,
    val itemDefinitions: Map<Key, ModelDefinition> = emptyMap()
) : ResourceProvider {
    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return buildMap {
            val allTextures = mutableSetOf<Key>()

            // Collect implicit model keys referenced inside definitions but not explicitly registered.
            // Convention: definition model path `ns:item/foo` → registry key `ns:foo`.
            val implicitKeys = itemDefinitions.values
                .flatMapTo(mutableSetOf()) { it.collectReferencedModelKeys() }
                .mapNotNullTo(mutableSetOf()) { modelKey ->
                    val path = modelKey.value().removePrefix("item/")
                    if (path == modelKey.value()) null // no item/ prefix — skip
                    else Key.key(modelKey.namespace(), path)
                }
                .filter { it !in models }

            fun processModelKey(key: Key, includeItemsJson: Boolean) {
                val modelSourceFile = createKey(key, "models", "json")
                sources[modelSourceFile]?.let { bytes ->
                    val json = gson.fromJson(bytes.decodeToString(), JsonObject::class.java)
                    json["textures"]?.let(JsonElement::getAsJsonObject)?.let { texturesObj ->
                        val rawTextureKeys = texturesObj.asMap().values
                            .filterIsInstance<JsonPrimitive>()
                            .mapNotNull { runCatching { Key.key(it.asString) }.getOrNull() }
                        rawTextureKeys.forEach { this[Key.key(it.namespace(), "textures/${it.value()}.png")] = PackResource.Direct(Key.key(it.namespace(), "textures/${it.value()}.png")) }
                        allTextures.addAll(rawTextureKeys)
                    }

                    this[createKey(key, "models/item", "json")] = PackResource.Direct(modelSourceFile)

                    if (includeItemsJson) {
                        val definition = itemDefinitions[key] ?: ModelDefinition.Model(
                            Key.key(key.namespace(), "item/${key.value()}"),
                            tints = listOf(Tint.Dye())
                        )
                        this[createKey(key, "items", "json")] = PackResource.RawJson(
                            JsonObject().apply {
                                addProperty("oversized_in_gui", true)
                                add("model", definition.toJson())
                            }
                        )
                    }
                }
            }

            models.forEach { processModelKey(it, includeItemsJson = true) }
            implicitKeys.forEach { processModelKey(it, includeItemsJson = false) }

            this[Key.key("atlases/blocks.json")] = PackResource.RawJson(
                JsonObject().apply {
                    add("sources", com.google.gson.JsonArray().apply {
                        allTextures.forEach { key ->
                            add(JsonObject().apply {
                                addProperty("type", "single")
                                addProperty("resource", key.toString())
                            })
                        }
                    })
                }
            )
        }
    }

    class Builder {
        private val models: MutableSet<Key> = mutableSetOf()
        private val itemDefinitions: MutableMap<Key, ModelDefinition> = mutableMapOf()

        fun add(vararg models: Key): Builder {
            this.models.addAll(models)
            return this
        }

        fun add(models: Collection<Key>): Builder {
            this.models.addAll(models)
            return this
        }

        fun add(registry: Registry<Key>): Builder {
            registry.collectEntries().forEach(::add)
            return this
        }

        fun add(registry: ModelRegistry): Builder {
            add(registry as Registry<Key>)
            registry.collectDefinitions().forEach { (key, definition) -> define(key, definition) }
            return this
        }

        /** Override the generated items JSON model definition for [key]. */
        fun define(key: Key, definition: ModelDefinition): Builder {
            itemDefinitions[key] = definition
            return this
        }

        fun build(): ModelProvider {
            return ModelProvider(models, itemDefinitions)
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
