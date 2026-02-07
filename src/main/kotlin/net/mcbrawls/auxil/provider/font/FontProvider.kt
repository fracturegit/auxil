package net.mcbrawls.auxil.provider.font

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import net.kyori.adventure.key.Key
import net.mcbrawls.api.registry.Registry
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource
import kotlin.math.min

class FontProvider(val fonts: Set<Font>) : ResourceProvider {
    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        return buildMap {
            fonts.forEach { font ->
                val jsonFile = createKey(font, "json", Font::fullKey)
                val ttfFile = createKey(font, "ttf", Font::key)

                this[jsonFile] = PackResource.RawJson(
                    obj {
                        "providers" to array(
                            obj {
                                val key = font.key
                                "type" to "ttf"
                                "file" to Key.key(key.namespace(), "${key.value()}.ttf")
                                "size" to font.size
                                "oversample" to font.oversample

                                font.shift?.let { shift ->
                                    "shift" to array(shift.first, shift.second)
                                }
                            },

                            obj {
                                "type" to "bitmap"
                                "file" to "fracture:font/bullet.png"
                                "chars" to array("⏺")
                                "height" to 5

                                val shift = font.shift
                                if (shift != null) {
                                    "ascent" to min(5 - shift.second.toInt(), 5)
                                } else {
                                    "ascent" to 5
                                }
                            },

                            obj {
                                "type" to "reference"
                                "id" to "minecraft:default"
                            },
                        )
                    }
                )

                this[ttfFile] = PackResource.Direct(ttfFile)
            }
        }
    }

    class Builder {
        private val fonts: MutableSet<Font> = mutableSetOf()

        fun add(vararg fonts: Font): Builder {
            this.fonts.addAll(fonts)
            return this
        }

        fun add(fonts: Collection<Font>): Builder {
            this.fonts.addAll(fonts)
            return this
        }

        fun add(registry: Registry<Font>): Builder {
            registry.collectEntries().forEach(::add)
            return this
        }

        fun build(): FontProvider {
            return FontProvider(fonts)
        }
    }

    companion object {
        fun builder(): Builder {
            return Builder()
        }

        fun createKey(font: Font, extension: String, keyProvider: (Font) -> Key): Key {
            val key = keyProvider.invoke(font)
            return createKey(key, extension)
        }

        fun createKey(key: Key, extension: String): Key {
            return Key.key(key.namespace(), "font/${key.value()}.$extension")
        }
    }
}
