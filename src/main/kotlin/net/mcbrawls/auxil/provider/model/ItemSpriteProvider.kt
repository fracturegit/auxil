package net.mcbrawls.auxil.provider.model

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import net.kyori.adventure.key.Key
import net.mcbrawls.api.registry.Registry
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource

class ItemSpriteProvider(val sprites: Map<String, ItemSprite>) : ResourceProvider {
    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return buildMap {
            val spriteKeys = mutableSetOf<Key>()

            sprites.forEach { (fontId, sprite) ->
                val jsonKey = Key.key(fontId)
                val textureKey = sprite.textureKey

                spriteKeys.add(textureKey)

                val pngFileKey = createKey(textureKey, "png", "textures/")
                this[pngFileKey] = PackResource.Direct(pngFileKey)


                val itemFile = createKey(jsonKey, "json", "items/")
                this[itemFile] = PackResource.RawJson(
                    obj {
                        "oversized_in_gui" to true
                        "model" to obj {
                            "type" to "minecraft:model"
                            "model" to Key.key(jsonKey.namespace(), "item/${jsonKey.value()}")
                            "tints" to array(
                                obj {
                                    "type" to "minecraft:dye"
                                    "default" to 0xFFFFFFFF
                                }
                            )
                        }
                    }
                )

                this[createKey(jsonKey, "json", "models/item/")] = PackResource.RawJson(
                    obj {
                        "parent" to Key.key("item/generated")
                        "textures" to obj {
                            "layer0" to textureKey
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
        private val fonts: MutableMap<String, ItemSprite> = mutableMapOf()

        fun add(key: String, font: ItemSprite): Builder {
            fonts[key] = font
            return this
        }

        fun add(key: Key, font: ItemSprite): Builder {
            return add(key.toString(), font)
        }

        fun add(registry: Registry<ItemSprite>): Builder {
            registry.forEachEntry(::add)
            return this
        }

        fun build(): ItemSpriteProvider {
            return ItemSpriteProvider(fonts)
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }

        fun createKey(key: Key, extension: String, folder: String = ""): Key {
            return Key.key(key.namespace(), "$folder${key.value()}.$extension")
        }
    }
}
