package net.mcbrawls.auxil.obfuscation

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.Inflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DeobfuscatedZip {

    /**
     * Reads an obfuscated ZIP from an [InputStream] and returns a standard, readable ZIP.
     */
    fun fromInputStream(input: InputStream): ByteArray {
        return fromByteArray(input.readBytes())
    }

    /**
     * Reads an obfuscated ZIP from a [ByteArray] and returns a standard, readable ZIP.
     */
    fun fromByteArray(data: ByteArray): ByteArray {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        val (cdOffset, cdSize) = readEocd(buf)
        val entries = readCentralDirectory(buf, cdOffset, cdSize)

        return writeStandardZip(data, entries)
    }

    /* eocd */

    private data class EocdInfo(val cdOffset: Int, val cdSize: Int)

    private fun readEocd(buf: ByteBuffer): EocdInfo {
        val data = buf.array()
        val eocdSig = byteArrayOf(0x50, 0x4b, 0x05, 0x06)

        val searchStart = maxOf(0, data.size - 65536)
        for (i in (searchStart..data.size - 22).reversed()) {
            if (data[i] == eocdSig[0] && data[i+1] == eocdSig[1] &&
                data[i+2] == eocdSig[2] && data[i+3] == eocdSig[3]) {
                buf.position(i + 4)
                buf.getShort() // disk number (corrupted - ignore)
                buf.getShort() // disk with CD start (ignore)
                buf.getShort() // entries on disk (corrupted - ignore)
                buf.getShort() // total entries (corrupted - ignore)
                val cdSize   = buf.getInt()
                val cdOffset = buf.getInt()
                return EocdInfo(cdOffset, cdSize)
            }
        }

        error("EOCD signature not found - not a valid ZIP")
    }

    /* cd */

    private data class CdEntry(
        val filename: String,
        val crc: Int,
        val compressedSize: Int,
        val compression: Int,
        val localOffset: Int,
    )

    private fun readCentralDirectory(buf: ByteBuffer, cdOffset: Int, cdSize: Int): List<CdEntry> {
        val entries = mutableListOf<CdEntry>()
        buf.position(cdOffset)
        val cdEnd = cdOffset + cdSize

        while (buf.position() < cdEnd - 4) {
            val sig = buf.getInt()
            check(sig == 0x02014b50) {
                "Expected CD signature at ${buf.position() - 4}, got ${sig.toUInt().toString(16)}"
            }

            buf.getShort() // version made by
            buf.getShort() // version needed
            buf.getShort() // flags
            val compression    = buf.getShort().toInt() and 0xFFFF
            buf.getShort()     // mod time
            buf.getShort()     // mod date
            val crc            = buf.getInt()
            val compressedSize = buf.getInt()
            buf.getInt()       // uncompressed size (corrupted - ignore)
            val filenameLen    = buf.getShort().toInt() and 0xFFFF
            val extraLen       = buf.getShort().toInt() and 0xFFFF
            val commentLen     = buf.getShort().toInt() and 0xFFFF
            buf.getShort()     // disk start (corrupted - ignore)
            buf.getShort()     // internal attributes
            buf.getInt()       // external attributes
            val localOffset    = buf.getInt()

            val filenameBytes = ByteArray(filenameLen)
            buf.get(filenameBytes)
            val filename = filenameBytes.toString(Charsets.UTF_8)

            buf.position(buf.position() + extraLen + commentLen)

            entries += CdEntry(filename, crc, compressedSize, compression, localOffset)
        }

        return entries
    }

    /* reconstruction */

    private fun writeStandardZip(data: ByteArray, entries: List<CdEntry>): ByteArray {
        val out = ByteArrayOutputStream()
        val zip = ZipOutputStream(out)

        zip.use { stream ->
            for (entry in entries) {
                val localFnameLen = (data[entry.localOffset + 26].toInt() and 0xFF) or ((data[entry.localOffset + 27].toInt() and 0xFF) shl 8)
                val localExtraLen = (data[entry.localOffset + 28].toInt() and 0xFF) or ((data[entry.localOffset + 29].toInt() and 0xFF) shl 8)
                val dataOffset = entry.localOffset + 30 + localFnameLen + localExtraLen

                val compressedContent = data.copyOfRange(dataOffset, dataOffset + entry.compressedSize)

                val content = when (entry.compression) {
                    ZipEntry.DEFLATED -> {
                        val inflater = Inflater(true)
                        inflater.setInput(compressedContent)
                        val inflated = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (!inflater.finished()) {
                            inflated.write(buffer, 0, inflater.inflate(buffer))
                        }
                        inflater.end()
                        inflated.toByteArray()
                    }
                    else -> compressedContent
                }

                val zipEntry = ZipEntry(entry.filename)
                zipEntry.time = 0
                zipEntry.method = ZipEntry.STORED
                zipEntry.size = content.size.toLong()
                zipEntry.compressedSize = content.size.toLong()
                zipEntry.crc = CRC32().also { it.update(content) }.value  // recompute, don't trust CD

                stream.putNextEntry(zipEntry)
                stream.write(content)
                stream.closeEntry()
            }
        }

        return out.toByteArray()
    }
}
