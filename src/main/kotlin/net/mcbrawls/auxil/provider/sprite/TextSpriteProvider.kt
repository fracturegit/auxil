package net.mcbrawls.auxil.provider.sprite

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import net.kyori.adventure.key.Key
import net.mcbrawls.api.registry.Registry
import net.mcbrawls.auxil.FilePropertyGetter
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.provider.font.FontProvider
import net.mcbrawls.auxil.resource.PackResource

class TextSpriteProvider(val sprites: Set<TextSprite>, val font: Key) : ResourceProvider {
    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return buildMap {
            this[FontProvider.createKey(font, "json")] = PackResource.RawJson(
                obj {
                    val files = this@buildMap
                    "providers" to array(
                        *sprites.mapNotNull { sprite ->
                            val key = sprite.key
                            val spriteKey = Key.key(key.namespace(), "sprites/${key.value()}")

                            val textureKey = createKey(spriteKey, "png", "textures/")
                            val textureBytes = sources[textureKey] ?: return@mapNotNull null

                            // register textures
                            files[textureKey] = PackResource.Direct(textureKey)

                            createKey(spriteKey, "png.mcmeta").let { key ->
                                if (sources.containsKey(key)) {
                                    files[key] = PackResource.Direct(key)
                                }
                            }

                            val height = sprite.height ?: (FilePropertyGetter.getImageHeight(textureBytes) ?: 8)
                            val ascent = sprite.ascent ?: height

                            obj {
                                "type" to "bitmap"
                                "file" to createKey(spriteKey, "png")
                                "height" to height
                                "ascent" to ascent
                                "chars" to array(sprite.unicodeChar)
                            }
                        }.toTypedArray()
                    )
                }
            )
        }
    }

    class Builder {
        private val sprites: MutableSet<TextSprite> = mutableSetOf()

        fun add(vararg sprites: TextSprite): Builder {
            this.sprites.addAll(sprites)
            return this
        }

        fun add(sprites: Collection<TextSprite>): Builder {
            this.sprites.addAll(sprites)
            return this
        }

        fun add(registry: Registry<TextSprite>): Builder {
            registry.collectEntries().forEach(::add)
            return this
        }

        fun build(font: Key): TextSpriteProvider {
            return TextSpriteProvider(sprites, font)
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }

        fun createKey(key: Key, extension: String, prefix: String = ""): Key {
            return Key.key(key.namespace(), "$prefix${key.value()}.$extension")
        }
    }
}
