package net.mcbrawls.auxil.obfuscation

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipInputStream

object ObfuscatedZip {
    private const val SIG_LFH: Int  = 0x04034b50
    private const val SIG_CD: Int   = 0x02014b50
    private const val SIG_EOCD: Int = 0x06054b50

    private const val FAKE_UNCOMP_SIZE = 0xFFFFFF7F.toInt()
    private const val FAKE_DISK_START  = 0xFFFE.toShort()
    private const val EOCD_DISK        = 0xFFFF.toShort()

    /**
     * Creates an obfuscated ZIP from a [java.util.zip.ZipInputStream].
     */
    fun fromZipInputStream(input: ZipInputStream): ByteArray {
        val files = LinkedHashMap<String, ByteArray>()
        var entry = input.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                files[entry.name] = input.readBytes()
            }
            input.closeEntry()
            entry = input.nextEntry
        }
        return fromMap(files)
    }

    /**
     * Creates an obfuscated ZIP from a map of paths to file byte arrays.
     */
    fun fromMap(files: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()

        data class CdEntry(
            val filename: ByteArray,
            val crc: Int,
            val size: Int,
            val localOffset: Int,
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as CdEntry

                if (crc != other.crc) return false
                if (size != other.size) return false
                if (localOffset != other.localOffset) return false
                if (!filename.contentEquals(other.filename)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = crc
                result = 31 * result + size
                result = 31 * result + localOffset
                result = 31 * result + filename.contentHashCode()
                return result
            }
        }

        val cdEntries = mutableListOf<CdEntry>()

        for ((name, content) in files) {
            val localOffset = out.size()
            val crc = CRC32().also { it.update(content) }.value.toInt()

            out.writeInt(SIG_LFH)
            out.writeShort(0)
            out.writeShort(0)
            out.writeShort(0)
            out.writeShort(0)
            out.writeShort(0)
            out.writeInt(0)
            out.writeInt(0)
            out.writeInt(0)
            out.writeShort(0)
            out.writeShort(0)

            out.write(content)

            cdEntries += CdEntry(name.toByteArray(Charsets.UTF_8), crc, content.size, localOffset)
        }

        val cdOffset = out.size()

        for (entry in cdEntries) {
            out.writeInt(SIG_CD)
            out.writeShort(0x031e)
            out.writeShort(10)
            out.writeShort(0)
            out.writeShort(0)
            out.writeShort(0)
            out.writeShort(0)
            out.writeInt(entry.crc)
            out.writeInt(entry.size)
            out.writeInt(FAKE_UNCOMP_SIZE)
            out.writeShort(entry.filename.size)
            out.writeShort(0)
            out.writeShort(0)
            out.writeShort(FAKE_DISK_START)
            out.writeShort(0)
            out.writeInt(1)
            out.writeInt(entry.localOffset)
            out.write(entry.filename)
        }

        val cdSize = out.size() - cdOffset

        out.writeInt(SIG_EOCD)
        out.writeShort(EOCD_DISK)
        out.writeShort(EOCD_DISK)
        out.writeShort(0)
        out.writeShort(0)
        out.writeInt(cdSize)
        out.writeInt(cdOffset)
        out.writeShort(0)

        return out.toByteArray()
    }

    private fun ByteArrayOutputStream.writeShort(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }

    private fun ByteArrayOutputStream.writeShort(value: Short) = writeShort(value.toInt() and 0xFFFF)

    private fun ByteArrayOutputStream.writeInt(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }
}
