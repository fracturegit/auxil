package net.mcbrawls.auxil.provider.model

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import com.google.gson.Gson
import net.kyori.adventure.key.Key
import net.mcbrawls.api.registry.BasicRegistry
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource

class ItemSpriteProvider(val sprites: Set<ItemSprite>) : ResourceProvider {
    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return buildMap {
            val spriteKeys = mutableSetOf<Key>()

            sprites.forEach { sprite ->
                val key = sprite.fullKey
                spriteKeys.add(key)

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

                        sprite.scale?.let { (x, y, z) ->
                            "display" to obj {
                                "gui" to obj {
                                    "scale" to array(x, y, z)
                                }
                            }
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
        private val sprites: MutableSet<ItemSprite> = mutableSetOf()

        fun add(vararg sprites: ItemSprite): Builder {
            this.sprites.addAll(sprites)
            return this
        }

        fun add(models: Collection<ItemSprite>): Builder {
            this.sprites.addAll(models)
            return this
        }

        fun add(registry: BasicRegistry<ItemSprite>): Builder {
            registry.collectEntries().forEach(::add)
            return this
        }

        fun build(): ItemSpriteProvider {
            return ItemSpriteProvider(sprites)
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
