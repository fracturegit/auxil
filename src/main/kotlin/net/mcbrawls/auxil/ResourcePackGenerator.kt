package net.mcbrawls.auxil

import com.github.mgrzeszczak.jsondsl.Json.Companion.obj
import com.google.gson.Gson
import net.kyori.adventure.key.Key
import net.mcbrawls.auxil.provider.ResourceProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ResourcePackGenerator(
    val meta: ResourcePack.Meta,
    val sourceFiles: Set<File>,
) {
    private val providers: MutableSet<ResourceProvider> = mutableSetOf()

    fun add(provider: ResourceProvider): ResourcePackGenerator {
        providers.add(provider)
        return this
    }

    fun generate(sources: Map<Key, ByteArray> = generateSources()): ResourcePack {
        val resources = providers.flatMap { it.collectFiles(sources).entries }.associate { it.key to it.value }

        val files = buildMap {
            this["pack.mcmeta"] = generateMetaBytes()
            meta.logo?.let { logo -> this["pack.png"] = logo.readBytes() }

            resources.forEach { (key, resource) ->
                val namespace = key.namespace()
                val path = key.value()
                this["assets/$namespace/$path"] = resource.createBytes(sources)
            }
        }

        val packBytes = createZip(files)
        return ResourcePack(packBytes)
    }

    fun generateSources(): Map<Key, ByteArray> = Auxil.generateSources(sourceFiles).mapValues { it.value.invoke() }

    private fun generateMetaBytes(): ByteArray {
        val format = meta.format
        val jsonString = gson.toJson(
            obj {
                "pack" to obj {
                    "description" to meta.description
                    "pack_format" to format
                    "supported_formats" to array(0, format)
                    "min_format" to 0
                    "max_format" to format
                }
            }
        )

        return jsonString.encodeToByteArray()
    }

    class Builder {
        private var meta: ResourcePack.Meta = ResourcePack.Meta()
        private val sources: MutableSet<File> = mutableSetOf()

        fun description(description: String): Builder {
            meta = meta.copy(description = description)
            return this
        }

        fun format(version: Int): Builder {
            meta = meta.copy(format = version)
            return this
        }

        fun logo(file: File): Builder {
            meta = meta.copy(logo = file)
            return this
        }

        fun source(folder: File): Builder {
            require(folder.isDirectory) { "File must be directory" }
            sources.add(folder)
            return this
        }

        fun build(): ResourcePackGenerator {
            return ResourcePackGenerator(meta, sources)
        }
    }

    companion object {
        private val gson = Gson()

        fun builder(): Builder {
            return Builder()
        }

        /**
         * Creates a zip file from a map of paths to their file byte arrays.
         * @return a zip byte array
         */
        fun createZip(files: Map<String, ByteArray>): ByteArray {
            val outputStream = ByteArrayOutputStream()
            val zipOutputStream = ZipOutputStream(outputStream)

            zipOutputStream.use { stream ->
                for ((fileName, fileContent) in files) {
                    val entry = ZipEntry(fileName)
                    entry.time = 0
                    stream.putNextEntry(entry)
                    stream.write(fileContent)
                    stream.closeEntry()
                }
            }

            return outputStream.toByteArray()
        }
    }
}
