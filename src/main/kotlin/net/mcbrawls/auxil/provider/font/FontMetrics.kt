package net.mcbrawls.auxil.provider.font

import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.io.File

class FontMetrics(
    file: File,
    size: Float,
    format: Int = Font.TRUETYPE_FONT,
) {
    val fontBytes: ByteArray = file.readBytes()
    val font: Font = Font.createFont(format, fontBytes.inputStream()).deriveFont(size)

    /**
     * Returns the bounds of the provided text for this font.
     */
    fun getBounds(string: String): List<Pair<Char, Rectangle2D?>> {
        return string.map { char ->
            char to if (font.canDisplay(char)) {
                font.getStringBounds(char.toString(), FONT_RENDER_CONTEXT)
            } else {
                null
            }
        } // TODO cache
    }

    companion object {
        private val FONT_RENDER_CONTEXT = FontRenderContext(AffineTransform(), false, false)
    }
}
