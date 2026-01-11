package net.mcbrawls.auxil.provider.model

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import com.google.gson.Gson
import net.kyori.adventure.key.Key
import net.mcbrawls.api.registry.BasicRegistry
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource

class ItemSpriteProvider(val models: Set<Key>) : ResourceProvider {
    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return buildMap {
            val spriteKeys = models.map { Key.key(it.namespace(), "sprites/${it.value()}") }

            spriteKeys.forEach { key ->
                val textureKey = createKey(key, "png", "textures/")
                this[textureKey] = PackResource.Direct(textureKey)

                val itemFile = createKey(key, "json", "items/")
                this[itemFile] = PackResource.RawJson(
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

                this[createKey(key, "json", "models/item/")] = PackResource.RawJson(
                    obj {
                        "parent" to Key.key("item/generated")
                        "textures" to obj {
                            "layer0" to key
                        }
                    }
                )
            }

            this[Key.key("atlases/items.json")] = PackResource.RawJson(
                obj {
                    "sources" to array(
                        *spriteKeys.map { key ->
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

        fun build(): ItemSpriteProvider {
            return ItemSpriteProvider(models)
        }
    }

    companion object {
        private val gson = Gson()

        fun builder(): Builder {
            return Builder()
        }

        fun createKey(key: Key, extension: String, folder: String = ""): Key {
            return Key.key(key.namespace(), "$folder${key.value()}.$extension")
        }
    }
}
