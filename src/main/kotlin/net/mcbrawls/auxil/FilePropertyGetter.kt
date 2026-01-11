package net.mcbrawls.auxil

import org.jaudiotagger.audio.AudioFileIO
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

object FilePropertyGetter {
    /**
     * Calculates the duration of a sound file '[file]'.
     * @return a duration
     */
    @Throws(IOException::class)
    fun calculateDuration(file: File): Duration {
        return try {
            val audioFile = AudioFileIO.read(file)
            val durationNanos = (audioFile.audioHeader.preciseTrackLength * 1_000_000_000).toLong()
            return durationNanos.nanoseconds
        } catch (exception: Exception) {
            if (!file.exists() || file.readBytes().isNotEmpty()) {
                exception.printStackTrace()
            }

            Duration.ZERO
        }
    }

    /**
     * Fetches and returns the width of an image file.
     * @return the width of the image file
     */
    fun getImageWidth(bytes: ByteArray): Int? {
        return readImage(bytes)?.width
    }

    // Function to get the left boundary of non-transparent pixels
    fun getLeftBoundary(image: BufferedImage): Int {
        val width = image.width
        val height = image.height

        return (0 until width)
            .takeWhile { x ->
                (0 until height).all { y ->
                    val alpha = (image.getRGB(x, y) shr 24) and 0xFF
                    alpha == 0
                }
            }.count()
    }

    // Function to get the right boundary of non-transparent pixels
    fun getRightBoundary(image: BufferedImage): Int {
        val width = image.width
        val height = image.height

        return (width - 1 downTo 0)
            .takeWhile { x ->
                (0 until height).all { y ->
                    val alpha = (image.getRGB(x, y) shr 24) and 0xFF
                    alpha == 0
                }
            }.count()
    }

    // Function to calculate width considering boundaries
    fun getImageBoundaries(bytes: ByteArray): Pair<Int, Int> {
        val image = readImage(bytes) ?: return 0 to 0

        val leftBoundary = getLeftBoundary(image)
        val rightBoundary = getRightBoundary(image)

        // Return the adjusted width
        return leftBoundary to rightBoundary
    }

    // Function to calculate width considering boundaries
    fun getWhitespaceRemovedWidth(bytes: ByteArray): Int {
        val image = readImage(bytes) ?: return 0
        val width = image.width

        val rightBoundary = getRightBoundary(image)

        // Return the adjusted width
        return width - rightBoundary
    }

    /**
     * Fetches and returns the height of an image file.
     * @return the width of the image file
     */
    fun getImageHeight(bytes: ByteArray): Int? {
        return readImage(bytes)?.height
    }

    fun readImage(bytes: ByteArray): BufferedImage? {
        return runCatching {
            ImageIO.read(bytes.inputStream())
        }.getOrNull()
    }
}
