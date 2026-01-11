package net.mcbrawls.auxil.provider.font

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import com.google.gson.JsonObject
import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.provider.ResourceProvider
import net.mcbrawls.auxil.resource.PackResource

open class NegativeSpaceFontProvider(
    /**
     * The font id.
     */
    val key: Key = Key.key("negative_space", "font"),

    /**
     * The maximum/minimum extreme of negative space.
     */
    val extremity: Int = Short.MAX_VALUE / 4,
) : ResourceProvider {
    /**
     * The base character to sum.
     */
    private val baseChar: Char = (extremity / 2).toChar()

    /**
     * All space values to their characters.
     */
    private val spaceToChar: Map<Int, Char> = buildMap {
        for (i in -extremity..extremity) {
            this[i] = baseChar + i
        }
    }

    private val charToSpace: Map<Char, Int> = spaceToChar.entries.associate { it.value to it.key }

    override fun collectFiles(sources: Map<Key, ByteArray>): Map<Key, PackResource> {
        val fontFile = FontProvider.createKey(key, "json")
        return mapOf(fontFile to PackResource.RawJson(obj {
            "providers" to array(
                obj {
                    "type" to "space"
                    "advances" to JsonObject().apply {
                        charToSpace.mapKeys { it.key.toString() }.forEach(::addProperty)
                    }
                }
            )
        }))
    }

    /**
     * Retrieves the text component for the given space width value.
     * @return a text component
     */
    operator fun get(width: Int): Char {
        return spaceToChar[width] ?: baseChar
    }

    /**
     * Retrieves the width of the given character.
     * @return a width value
     */
    fun widthOf(char: Char): Int {
        return charToSpace[char] ?: error("Invalid char: $char")
    }

    companion object : NegativeSpaceFontProvider()
}
