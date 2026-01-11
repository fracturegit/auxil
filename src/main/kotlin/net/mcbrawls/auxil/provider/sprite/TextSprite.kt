package net.mcbrawls.auxil.provider.sprite

import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.FilePropertyGetter
import net.mcbrawls.auxil.resource

data class TextSprite(
    val key: Key,
    val index: (TextSprite) -> Int,
    val height: Int? = null,
    val ascent: Int? = null,
) {
    val unicodeValue: Int get() = 0xE000 + index.invoke(this)
    val unicodeChar: Char get() = unicodeValue.toChar()

    val metadata: Metadata by lazy(::createMetadata)

    fun createMetadata(): Metadata {
        val key = TextSpriteProvider.createKey(key, "png", "textures/sprites/")
        val resource = resource("source/${key.namespace()}/${key.value()}") ?: error("No sprite: $key")
        val bytes = resource.readBytes()
        return Metadata(
            trueHeight = FilePropertyGetter.getImageHeight(bytes) ?: error("Could not get image height: $key"),
            trueWidth = FilePropertyGetter.getWhitespaceRemovedWidth(bytes),
        )
    }

    inner class Metadata(
        val trueHeight: Int,
        val trueWidth: Int,
    ) {
        val scaleFactor = (height ?: trueHeight).toDouble() / trueHeight
        val width = trueWidth * scaleFactor
    }
}
