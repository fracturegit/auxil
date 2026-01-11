package net.mcbrawls.auxil

import java.io.File

data class ResourcePack(
    val bytes: ByteArray,
) {
    data class Meta(
        val description: String = "",
        val format: Int = 69,
        val logo: File? = null,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResourcePack

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}
